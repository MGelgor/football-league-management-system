package com.footballleague.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.dto.InboxMessageResponse;
import com.footballleague.entity.InboxMessage;
import com.footballleague.entity.InboxMessageType;
import com.footballleague.entity.ManagerProfile;
import com.footballleague.entity.Player;
import com.footballleague.entity.Season;
import com.footballleague.entity.Team;
import com.footballleague.repository.InboxMessageRepository;
import com.footballleague.repository.SeasonRepository;

import lombok.RequiredArgsConstructor;

/**
 * Menajerlerin gelen kutusu. Her mesajın bir alıcısı (menajer profili) vardır; liste o anki menajerin
 * mesajlarıdır. Mesajlar yalnızca bir menajeri ilgilendirdiğinde yazılır (takımı insan yönetiyorsa).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InboxService {

    private final InboxMessageRepository inboxMessageRepository;
    private final Managers managers;
    private final SeasonRepository seasonRepository;

    /** Takımı bir insan menajer mi yönetiyor (değilse yapay zekâ / bot). */
    public boolean isManaged(Team team) {
        return managers.isHuman(team);
    }

    @Transactional(readOnly = true)
    public boolean hasOpenOffer(Player player) {
        return inboxMessageRepository.existsByTypeAndPlayerIdAndResolvedFalse(InboxMessageType.TRANSFER_OFFER,
                player.getId());
    }

    /** to: alıcı menajer; null ise (takımı bot yönetiyor) mesaj yazılmaz. */
    public InboxMessage post(ManagerProfile to, InboxMessageType type, String title, String body, Player player,
            Team team, Long amount) {
        if (to == null) {
            return null;
        }
        int seasonNumber = seasonRepository.findTopByOrderBySeasonNumberDesc().map(Season::getSeasonNumber).orElse(1);
        return inboxMessageRepository.save(InboxMessage.builder().type(type).title(title).body(body)
                .seasonNumber(seasonNumber).player(player).team(team).amount(amount).profile(to).build());
    }

    /** Takımı yöneten menajere yazar (takımı bot yönetiyorsa yazılmaz). */
    public InboxMessage postToTeam(Team managed, InboxMessageType type, String title, String body, Player player,
            Team team, Long amount) {
        return post(managers.ofTeam(managed).orElse(null), type, title, body, player, team, amount);
    }

    @Transactional(readOnly = true)
    public List<InboxMessageResponse> list() {
        return mine().stream().map(InboxService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return managers.current().map(profile -> inboxMessageRepository.countUnread(profile.getId())).orElse(0L);
    }

    public void markRead(Long id) {
        mine().stream().filter(message -> message.getId().equals(id)).forEach(message -> message.setRead(true));
    }

    public void markAllRead() {
        mine().forEach(message -> message.setRead(true));
    }

    /** Mesaj şu anki menajerin mi (başkasının teklifini kabul edemesin). */
    public boolean isMine(InboxMessage message) {
        ManagerProfile to = message.getProfile();
        return to == null || managers.current().map(profile -> profile.getId().equals(to.getId())).orElse(false);
    }

    private List<InboxMessage> mine() {
        return managers.current().map(profile -> inboxMessageRepository.findForProfile(profile.getId()))
                .orElse(List.of());
    }

    static InboxMessageResponse toResponse(InboxMessage message) {
        Player player = message.getPlayer();
        Team team = message.getTeam();
        boolean actionable = !message.isResolved() && (message.getType() == InboxMessageType.TRANSFER_OFFER
                || message.getType() == InboxMessageType.JOB_OFFER || message.getType() == InboxMessageType.YOUTH);
        return new InboxMessageResponse(message.getId(), message.getType(), message.getTitle(), message.getBody(),
                message.getSeasonNumber(), message.isRead(), message.isResolved(), actionable,
                player != null ? player.getId() : null, player != null ? player.getName() : null,
                team != null ? team.getId() : null, team != null ? team.getName() : null, message.getAmount());
    }
}
