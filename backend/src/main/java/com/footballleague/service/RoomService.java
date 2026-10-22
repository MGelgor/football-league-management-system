package com.footballleague.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.footballleague.config.RoomContext;
import com.footballleague.config.RoomSchemas;
import com.footballleague.config.StartupDataInitializer;
import com.footballleague.dto.CreateRoomRequest;
import com.footballleague.dto.JoinRoomRequest;
import com.footballleague.dto.RoomJoinResponse;
import com.footballleague.dto.RoomResponse;
import com.footballleague.dto.RoomSettingsRequest;
import com.footballleague.entity.Room;
import com.footballleague.entity.RoomMember;
import com.footballleague.entity.RoomStatus;
import com.footballleague.exception.RoomAccessException;
import com.footballleague.exception.RoomConflictException;
import com.footballleague.exception.RoomNotFoundException;
import com.footballleague.repository.RoomMemberRepository;
import com.footballleague.repository.RoomRepository;

/**
 * Oda kurma / katılma / ayrılma, ayarlar, başlatma ve "hazırım". Oda kurulunca odaya özel şema ve tablolar
 * oluşturulur, lig başlangıç verisiyle doldurulur. Bu servis varsayılan şemadaki rooms / room_members
 * tablolarıyla çalışır; bir odanın içinden (RoomContext dolu iken) çağrılmamalı. Takvimi değiştiren işler
 * RoomScheduler'a devredilir (açık transaction olmadan).
 */
@Service
public class RoomService implements ApplicationRunner {

    static final int LEAGUE_SIZE = 18;
    // Kadro ve taktik maç saatinden bu kadar önce kilitlenir
    static final Duration LINEUP_LOCK = Duration.ofMinutes(5);
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String NO_ACCESS = "Bu odaya erişiminiz yok. Davet koduyla yeniden katılın.";

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final RoomSchemas roomSchemas;
    private final RoomScheduler roomScheduler;
    private final RoomLeague roomLeague;
    private final MyTeamService myTeamService;
    private final Managers managers;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final StartupDataInitializer leagueInitializer;
    private final int maxRooms;
    private final int maxMembers;

    public RoomService(RoomRepository roomRepository, RoomMemberRepository memberRepository, RoomSchemas roomSchemas,
            RoomScheduler roomScheduler, RoomLeague roomLeague, MyTeamService myTeamService, Managers managers,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate, StartupDataInitializer leagueInitializer,
            @Value("${app.rooms.max-rooms}") int maxRooms, @Value("${app.rooms.max-members}") int maxMembers) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.roomSchemas = roomSchemas;
        this.roomScheduler = roomScheduler;
        this.roomLeague = roomLeague;
        this.myTeamService = myTeamService;
        this.managers = managers;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.leagueInitializer = leagueInitializer;
        this.maxRooms = maxRooms;
        this.maxMembers = maxMembers;
    }

    /** Açılışta: var olan odaların şemaları yeni sürümün tablolarına / sütunlarına güncellenir. */
    @Override
    public void run(ApplicationArguments args) {
        roomRepository.findAll().forEach(room -> roomSchemas.update(room.getSchemaName()));
    }

    /** Odayı ve şemasını oluşturur, ligi hazırlar; kuran kişi odanın sahibi olur. */
    public RoomJoinResponse create(CreateRoomRequest request) {
        if (roomRepository.count() >= maxRooms) {
            throw new RoomConflictException("Sunucudaki oda sınırına ulaşıldı (" + maxRooms + ")");
        }
        String token = newToken();
        RoomMember owner = transactionTemplate.execute(status -> {
            String code = newCode();
            Room room = roomRepository.save(Room.builder()
                    .code(code)
                    .name(request.roomName().trim())
                    .schemaName("room_" + code.toLowerCase(Locale.ROOT))
                    .createdAt(Instant.now())
                    .build());
            return memberRepository.save(member(room, request.nickname(), token, true));
        });
        Room room = owner.getRoom();
        try {
            provision(room);
        } catch (RuntimeException e) {
            drop(room);
            throw e;
        }
        return new RoomJoinResponse(response(room, owner.getId()), token);
    }

    public RoomJoinResponse join(String code, JoinRoomRequest request) {
        Room room = roomRepository.findByCode(normalizeCode(code)).orElseThrow(() -> new RoomNotFoundException(code));
        if (memberRepository.countByRoomId(room.getId()) >= maxMembers) {
            throw new RoomConflictException("Oda dolu (en fazla " + maxMembers + " kişi)");
        }
        String nickname = request.nickname().trim();
        if (memberRepository.existsByRoomIdAndNicknameIgnoreCase(room.getId(), nickname)) {
            throw new RoomConflictException("Bu takma ad odada kullanılıyor: " + nickname);
        }
        String token = newToken();
        RoomMember member = memberRepository.save(member(room, nickname, token, false));
        // Yanıt odanın şemasından da okur; açık bir transaction olmamalı
        return new RoomJoinResponse(response(room, member.getId()), token);
    }

    /** Oda bilgisi; zamanı gelmiş olaylar önce işlenir. */
    public RoomResponse get(String code, String token) {
        RoomMember member = requireMember(code, token);
        roomScheduler.tick(member.getRoom().getId());
        return response(reload(member.getRoom()), member.getId());
    }

    /** Üye odadan ayrılır; takımını bot devralır. */
    public void leave(String code, String token) {
        RoomMember member = requireMember(code, token);
        if (member.isOwner()) {
            throw new RoomConflictException("Oda sahibi odadan ayrılamaz; odayı silebilir");
        }
        removeMember(member, member.getRoom());
    }

    /** Oda sahibi bir üyeyi çıkarır; takımını bot devralır. */
    public RoomResponse kick(String code, String token, Long memberId) {
        RoomMember owner = requireOwner(code, token);
        RoomMember member = memberRepository.findById(memberId)
                .filter(found -> found.getRoom().getId().equals(owner.getRoom().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Üye bulunamadı"));
        if (member.isOwner()) {
            throw new RoomConflictException("Oda sahibi kendini çıkaramaz");
        }
        removeMember(member, owner.getRoom());
        return response(reload(owner.getRoom()), owner.getId());
    }

    /** room: üyenin odası (yüklenmiş hâliyle; üyenin kendi alanı lazy olabilir). */
    private void removeMember(RoomMember member, Room room) {
        roomScheduler.locked(room.getId(), () -> {
            RoomContext.call(new RoomContext.Current(room.getId(), room.getCode(), room.getSchemaName(),
                    member.getId(), false, null), () -> {
                        myTeamService.release(member.getId());
                        return null;
                    });
            memberRepository.delete(member);
            return null;
        });
    }

    /** Odayı, şemasını ve tüm verisini siler (yalnızca sahip). */
    public void delete(String code, String token) {
        drop(requireOwner(code, token).getRoom());
    }

    /** Maç saati, saat dilimi, transfer penceresi süresi (yalnızca sahip). */
    public RoomResponse updateSettings(String code, String token, RoomSettingsRequest request) {
        RoomMember owner = requireOwner(code, token);
        LocalTime matchTime = LocalTime.parse(request.matchTime());
        try {
            ZoneId.of(request.timeZone());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Geçersiz saat dilimi: " + request.timeZone());
        }
        Long roomId = owner.getRoom().getId();
        roomScheduler.locked(roomId, () -> {
            Room room = roomRepository.findById(roomId).orElseThrow();
            room.setMatchTime(matchTime);
            room.setTimeZone(request.timeZone());
            room.setTransferHours(request.transferHours());
            roomScheduler.reschedule(room);
            return roomRepository.save(room);
        });
        return response(reload(owner.getRoom()), owner.getId());
    }

    /** Lobiden ilk sezona geçer (yalnızca sahip; herkes hazır olunca da kendiliğinden). */
    public RoomResponse start(String code, String token) {
        RoomMember owner = requireOwner(code, token);
        if (owner.getRoom().getStatus() != RoomStatus.LOBBY) {
            throw new RoomConflictException("Lig zaten başladı");
        }
        roomScheduler.start(owner.getRoom().getId());
        return response(reload(owner.getRoom()), owner.getId());
    }

    /**
     * Sıradaki olayı beklemeden işler (yalnızca sahip): bugünün maç gününü şimdi oynatır ya da transfer
     * penceresini kapatır. Takvim kaymaz: erken oynatılan gün o gün tekrar oynanmaz.
     */
    public RoomResponse runNow(String code, String token) {
        RoomMember owner = requireOwner(code, token);
        if (owner.getRoom().getStatus() == RoomStatus.LOBBY) {
            throw new RoomConflictException("Önce ligi başlatın");
        }
        roomScheduler.runNow(owner.getRoom().getId());
        return response(reload(owner.getRoom()), owner.getId());
    }

    /**
     * Lobide "başlayalım", transfer penceresinde "işim bitti". Odadaki herkes hazırsa lig başlar ya da pencere
     * hemen kapanıp yeni sezon başlar.
     */
    public RoomResponse setReady(String code, String token, boolean ready) {
        RoomMember member = requireMember(code, token);
        member.setReady(ready);
        memberRepository.save(member);
        Room room = reload(member.getRoom());
        if (ready && !memberRepository.existsByRoomIdAndReadyFalse(room.getId())) {
            if (room.getStatus() == RoomStatus.LOBBY) {
                roomScheduler.start(room.getId());
            } else if (room.getStatus() == RoomStatus.TRANSFER_WINDOW) {
                roomScheduler.runNow(room.getId());
            }
        }
        return response(reload(room), member.getId());
    }

    /** RoomFilter için: token bu odanın bir üyesine aitse odanın bağlamı. */
    public Optional<RoomContext.Current> authenticate(String code, String token) {
        if (code == null || token == null) {
            return Optional.empty();
        }
        return memberRepository.findWithRoom(normalizeCode(code), hash(token)).map(member -> {
            Room room = member.getRoom();
            Instant lock = room.getStatus() == RoomStatus.SEASON && room.getNextEventAt() != null
                    ? room.getNextEventAt().minus(LINEUP_LOCK)
                    : null;
            return new RoomContext.Current(room.getId(), room.getCode(), room.getSchemaName(), member.getId(),
                    member.isOwner(), lock);
        });
    }

    private RoomMember requireMember(String code, String token) {
        if (!roomRepository.existsByCode(normalizeCode(code))) {
            throw new RoomNotFoundException(code);
        }
        if (token == null) {
            throw new RoomAccessException(NO_ACCESS);
        }
        return memberRepository.findWithRoom(normalizeCode(code), hash(token))
                .orElseThrow(() -> new RoomAccessException(NO_ACCESS));
    }

    private RoomMember requireOwner(String code, String token) {
        RoomMember member = requireMember(code, token);
        if (!member.isOwner()) {
            throw new RoomAccessException("Bu işlemi yalnızca oda sahibi yapabilir");
        }
        return member;
    }

    /** Şemayı ve tabloları oluşturur, sonra odanın içinde açılış verisini yükler (4 büyükler, 18 takım…). */
    private void provision(Room room) {
        roomSchemas.create(room.getSchemaName());
        inRoom(room, () -> {
            leagueInitializer.initializeLeague(LEAGUE_SIZE);
            return null;
        });
    }

    private void drop(Room room) {
        // Şema adı yalnızca harf / rakamdan üretildiği için sorguya doğrudan yazılabilir
        jdbcTemplate.execute("drop schema if exists " + room.getSchemaName() + " cascade");
        transactionTemplate.executeWithoutResult(status -> {
            memberRepository.deleteByRoomId(room.getId());
            roomRepository.deleteById(room.getId());
        });
    }

    private Room reload(Room room) {
        return roomRepository.findById(room.getId()).orElseThrow(() -> new RoomNotFoundException(room.getCode()));
    }

    private RoomResponse response(Room room, Long you) {
        boolean started = room.getStatus() != RoomStatus.LOBBY;
        String nextMatchDay = room.getStatus() == RoomStatus.SEASON ? inRoom(room, roomLeague::nextMatchDay) : null;
        Map<Long, String> teams = inRoom(room, managers::teamNamesByMember);
        Integer seasonNumber = started ? inRoom(room, roomLeague::seasonNumber).orElse(null) : null;
        return new RoomResponse(room.getCode(), room.getName(), room.getCreatedAt(), maxMembers,
                memberRepository.findByRoomIdOrderByJoinedAtAsc(room.getId()).stream()
                        .map(member -> new RoomResponse.Member(member.getId(), member.getNickname(), member.isOwner(),
                                member.getJoinedAt(), member.isReady(), teams.get(member.getId())))
                        .toList(),
                you, room.getStatus(), room.getMatchTime().toString(), room.getTimeZone(), room.getTransferHours(),
                room.getNextEventAt(), nextMatchDay, seasonNumber);
    }

    private static <T> T inRoom(Room room, Supplier<T> work) {
        return RoomContext.call(RoomContext.Current.system(room.getId(), room.getCode(), room.getSchemaName()), work);
    }

    private static RoomMember member(Room room, String nickname, String token, boolean owner) {
        return RoomMember.builder()
                .room(room)
                .nickname(nickname.trim())
                .tokenHash(hash(token))
                .owner(owner)
                .joinedAt(Instant.now())
                .build();
    }

    private String newCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
