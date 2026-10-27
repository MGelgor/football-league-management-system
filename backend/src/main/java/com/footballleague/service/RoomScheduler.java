package com.footballleague.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.footballleague.config.RoomContext;
import com.footballleague.entity.Room;
import com.footballleague.entity.RoomStatus;
import com.footballleague.repository.RoomMemberRepository;
import com.footballleague.repository.RoomRepository;

/**
 * Odaların takvimi. Her odanın sıradaki olayı (nextEventAt) zamanı gelince işlenir: sezonda bir maç günü,
 * transfer penceresinin sonunda yeni sezon. Sunucu uyuyup kaçırdıysa olaylar sırayla yetiştirilir.
 * Tetikleyiciler: dakikalık iş (RoomTickJob), harici cron (/internal/tick), odaya gelen her istek (RoomFilter).
 * Aynı oda için aynı anda tek iş çalışır (oda başına kilit; tek sunucu örneği varsayılır).
 * Açık bir transaction'ın içinden çağrılmamalı: oda işleri odanın şemasındaki yeni bağlantılarla yapılır.
 */
@Service
public class RoomScheduler {

    // Lig başlarken / pencere kapanınca ilk maç en az bu kadar sonraya konur (kadro hazırlamak için)
    static final Duration FIRST_MATCH_LEAD = Duration.ofMinutes(10);
    private static final Duration EXTERNAL_TICK_INTERVAL = Duration.ofSeconds(20);
    private static final int MAX_EVENTS_PER_TICK = 60;
    private static final Logger log = LoggerFactory.getLogger(RoomScheduler.class);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final RoomLeague roomLeague;
    private final Clock clock;
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
    private volatile Instant lastExternalTick = Instant.EPOCH;
    // En erken olayın zamanı: o zamana kadar dakikalık iş veritabanına gitmez (boşta Neon uyuyabilsin)
    private Instant nextDue = Instant.EPOCH;

    public RoomScheduler(RoomRepository roomRepository, RoomMemberRepository memberRepository, RoomLeague roomLeague,
            Clock clock) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.roomLeague = roomLeague;
        this.clock = clock;
    }

    /** Zamanı gelmiş tüm odalar; işlenen olay sayısı. */
    public int tickAll() {
        synchronized (this) {
            if (clock.instant().isBefore(nextDue)) {
                return 0;
            }
        }
        int events = roomRepository.findByNextEventAtLessThanEqual(clock.instant()).stream()
                .mapToInt(room -> tick(room.getId()))
                .sum();
        // Okuma ile atama arasında öne çekilen bir olay kaybolmasın: markDue aynı kilidi bekler
        synchronized (this) {
            nextDue = roomRepository.findEarliestNextEventAt().orElse(Instant.MAX);
        }
        return events;
    }

    /** Odanın sıradaki olayı kaydedildikten sonra çağrılır; dakikalık iş onu kaçırmasın. */
    public synchronized void markDue(Instant eventAt) {
        if (eventAt != null && eventAt.isBefore(nextDue)) {
            nextDue = eventAt;
        }
    }

    /** Harici cron için: çok sık çağrılırsa atlanır (null). */
    public Integer tickAllThrottled() {
        Instant now = clock.instant();
        if (now.isBefore(lastExternalTick.plus(EXTERNAL_TICK_INTERVAL))) {
            return null;
        }
        lastExternalTick = now;
        return tickAll();
    }

    /** Odanın zamanı gelmiş olaylarını sırayla işler. Oda zaten işleniyorsa beklemeden döner. */
    public int tick(Long roomId) {
        ReentrantLock lock = lock(roomId);
        if (!lock.tryLock()) {
            return 0;
        }
        try {
            int events = 0;
            while (events < MAX_EVENTS_PER_TICK) {
                Room room = roomRepository.findById(roomId).orElse(null);
                if (room == null || room.getNextEventAt() == null || room.getNextEventAt().isAfter(clock.instant())) {
                    break;
                }
                runEvent(room);
                events++;
            }
            return events;
        } catch (RuntimeException e) {
            log.error("Oda {} takvimi ilerletilemedi; bir sonraki tetiklemede yeniden denenecek", roomId, e);
            return 0;
        } finally {
            lock.unlock();
        }
    }

    /** Lobiden sezona: ilk sezonun fikstürü, ilk maç günü. */
    public void start(Long roomId) {
        locked(roomId, () -> {
            Room room = roomRepository.findById(roomId).orElseThrow();
            if (room.getStatus() != RoomStatus.LOBBY) {
                return null;
            }
            inRoom(room, () -> {
                roomLeague.startSeason();
                return null;
            });
            room.setStatus(RoomStatus.SEASON);
            room.setNextEventAt(firstMatchDay(room, clock.instant()));
            save(room, true);
            return null;
        });
    }

    /** Sıradaki olayı beklemeden işler (oda sahibinin "şimdi oynat"ı, herkes hazırken pencerenin kapanması). */
    public void runNow(Long roomId) {
        locked(roomId, () -> {
            Room room = roomRepository.findById(roomId).orElseThrow();
            runEvent(room);
            return null;
        });
    }

    /** Maç saati / saat dilimi değişince sezondaki odanın sıradaki maç günü yeniden hesaplanır. */
    public void reschedule(Room room) {
        if (room.getStatus() == RoomStatus.SEASON) {
            room.setNextEventAt(firstMatchDay(room, clock.instant()));
        }
    }

    public <T> T locked(Long roomId, Supplier<T> work) {
        ReentrantLock lock = lock(roomId);
        lock.lock();
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }

    private void runEvent(Room room) {
        Instant now = clock.instant();
        switch (room.getStatus()) {
            case SEASON -> {
                RoomLeague.MatchDay day = inRoom(room, roomLeague::playMatchDay);
                boolean complete = day.seasonComplete();
                // Takvim, olayın planlanan zamanına göre ilerler (erken oynatılan gün tekrar oynanmaz)
                Instant planned = room.getNextEventAt() == null ? now : room.getNextEventAt();
                // Canlı yayın planlanan saatte (erken oynatıldıysa şimdi) başlar
                room.setLiveWeek(day.week());
                room.setLiveStartedAt(now.isBefore(planned) ? now : planned);
                if (complete) {
                    room.setStatus(RoomStatus.TRANSFER_WINDOW);
                    room.setNextEventAt(planned.plus(Duration.ofHours(room.getTransferHours())));
                } else {
                    room.setNextEventAt(nextDay(room, planned));
                }
                save(room, complete);
            }
            case TRANSFER_WINDOW -> {
                inRoom(room, () -> {
                    roomLeague.startSeason();
                    return null;
                });
                room.setStatus(RoomStatus.SEASON);
                room.setNextEventAt(firstMatchDay(room, now));
                save(room, true);
            }
            case LOBBY -> {
                room.setNextEventAt(null);
                save(room, false);
            }
        }
    }

    /** after + 10 dk'dan sonraki ilk maç saati. */
    Instant firstMatchDay(Room room, Instant after) {
        ZoneId zone = ZoneId.of(room.getTimeZone());
        Instant earliest = after.plus(FIRST_MATCH_LEAD);
        ZonedDateTime candidate = earliest.atZone(zone).toLocalDate().atTime(room.getMatchTime()).atZone(zone);
        if (candidate.toInstant().isBefore(earliest)) {
            candidate = candidate.toLocalDate().plusDays(1).atTime(room.getMatchTime()).atZone(zone);
        }
        return candidate.toInstant();
    }

    /** Planlanan günün ertesi günü, maç saatinde (yaz saati geçişlerinde de yerel saat korunur). */
    private static Instant nextDay(Room room, Instant planned) {
        ZoneId zone = ZoneId.of(room.getTimeZone());
        return planned.atZone(zone).toLocalDate().plusDays(1).atTime(room.getMatchTime()).atZone(zone).toInstant();
    }

    private void save(Room room, boolean phaseChanged) {
        roomRepository.save(room);
        markDue(room.getNextEventAt());
        if (phaseChanged) {
            memberRepository.resetReady(room.getId());
        }
    }

    private static <T> T inRoom(Room room, Supplier<T> work) {
        return RoomContext.call(RoomContext.Current.system(room.getId(), room.getCode(), room.getSchemaName()), work);
    }

    private ReentrantLock lock(Long roomId) {
        return locks.computeIfAbsent(roomId, id -> new ReentrantLock());
    }
}
