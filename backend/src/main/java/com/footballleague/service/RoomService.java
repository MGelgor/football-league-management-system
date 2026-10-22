package com.footballleague.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.footballleague.config.RoomContext;
import com.footballleague.config.StartupDataInitializer;
import com.footballleague.dto.CreateRoomRequest;
import com.footballleague.dto.JoinRoomRequest;
import com.footballleague.dto.RoomJoinResponse;
import com.footballleague.dto.RoomResponse;
import com.footballleague.entity.Room;
import com.footballleague.entity.RoomMember;
import com.footballleague.exception.RoomAccessException;
import com.footballleague.exception.RoomConflictException;
import com.footballleague.exception.RoomNotFoundException;
import com.footballleague.repository.RoomMemberRepository;
import com.footballleague.repository.RoomRepository;

import jakarta.persistence.EntityManagerFactory;

/**
 * Oda kurma / katılma / ayrılma. Oda kurulunca odaya özel şema ve tablolar oluşturulur, lig başlangıç verisiyle
 * doldurulur. Bu servis yalnızca varsayılan şemadaki rooms / room_members tablolarıyla çalışır; bir odanın
 * içinden (RoomContext dolu iken) çağrılmamalı.
 */
@Service
public class RoomService {

    static final int LEAGUE_SIZE = 18;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final StartupDataInitializer leagueInitializer;
    private final int maxRooms;
    private final int maxMembers;

    public RoomService(RoomRepository roomRepository, RoomMemberRepository memberRepository,
            EntityManagerFactory entityManagerFactory, JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate, StartupDataInitializer leagueInitializer,
            @Value("${app.rooms.max-rooms}") int maxRooms, @Value("${app.rooms.max-members}") int maxMembers) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.leagueInitializer = leagueInitializer;
        this.maxRooms = maxRooms;
        this.maxMembers = maxMembers;
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

    @Transactional
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
        return new RoomJoinResponse(response(room, member.getId()), token);
    }

    @Transactional(readOnly = true)
    public RoomResponse get(String code, String token) {
        RoomMember member = requireMember(code, token);
        return response(member.getRoom(), member.getId());
    }

    @Transactional
    public void leave(String code, String token) {
        RoomMember member = requireMember(code, token);
        if (member.isOwner()) {
            throw new RoomConflictException("Oda sahibi odadan ayrılamaz; odayı silebilir");
        }
        memberRepository.delete(member);
    }

    /** Odayı, şemasını ve tüm verisini siler (yalnızca sahip). */
    public void delete(String code, String token) {
        Room room = transactionTemplate.execute(status -> {
            RoomMember member = requireMember(code, token);
            if (!member.isOwner()) {
                throw new RoomAccessException("Odayı yalnızca oda sahibi silebilir");
            }
            return Hibernate.unproxy(member.getRoom(), Room.class);
        });
        drop(room);
    }

    /** RoomFilter için: token bu odanın bir üyesine aitse odanın bağlamı. */
    @Transactional(readOnly = true)
    public Optional<RoomContext.Current> authenticate(String code, String token) {
        if (code == null || token == null) {
            return Optional.empty();
        }
        return memberRepository.findByRoomCodeAndTokenHash(normalizeCode(code), hash(token))
                .map(member -> new RoomContext.Current(member.getRoom().getId(), member.getRoom().getCode(),
                        member.getRoom().getSchemaName(), member.getId(), member.isOwner()));
    }

    private RoomMember requireMember(String code, String token) {
        if (!roomRepository.existsByCode(normalizeCode(code))) {
            throw new RoomNotFoundException(code);
        }
        if (token == null) {
            throw new RoomAccessException("Bu odaya erişiminiz yok. Davet koduyla yeniden katılın.");
        }
        return memberRepository.findByRoomCodeAndTokenHash(normalizeCode(code), hash(token))
                .orElseThrow(() -> new RoomAccessException("Bu odaya erişiminiz yok. Davet koduyla yeniden katılın."));
    }

    /** Şemayı ve tabloları oluşturur, sonra odanın içinde açılış verisini yükler (4 büyükler, 18 takım…). */
    private void provision(Room room) {
        entityManagerFactory.unwrap(SessionFactory.class).getSchemaManager()
                .forSchema(room.getSchemaName())
                .exportMappedObjects(true);
        RoomContext.call(new RoomContext.Current(room.getId(), room.getCode(), room.getSchemaName(), null, true),
                () -> {
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

    private RoomResponse response(Room room, Long you) {
        return new RoomResponse(room.getCode(), room.getName(), room.getCreatedAt(), maxMembers,
                memberRepository.findByRoomIdOrderByJoinedAtAsc(room.getId()).stream()
                        .map(member -> new RoomResponse.Member(member.getId(), member.getNickname(), member.isOwner(),
                                member.getJoinedAt()))
                        .toList(),
                you);
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
