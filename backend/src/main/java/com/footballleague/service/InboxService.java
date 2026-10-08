package com.footballleague.service;

import java.util.List;
import java.util.Optional;

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
import com.footballleague.repository.ManagerProfileRepository;
import com.footballleague.repository.SeasonRepository;

import lombok.RequiredArgsConstructor;

/** Menajerin gelen kutusu; mesajlar yalnızca menajer modu açıkken (ya da menajer işsizken) yazılır. */
@Service
@RequiredArgsConstructor
@Transactional
public class InboxService {

    private final InboxMessageRepository inboxMessageRepository;
    private final ManagerProfileRepository managerProfileRepository;
    private final SeasonRepository seasonRepository;

    /** Kullanıcının yönettiği takım (mod kapalıysa ya da menajer işsizse boş). */
    @Transactional(readOnly = true)
    public Optional<Team> managedTeam() {
        return managerProfileRepository.findFirstByOrderByIdAsc().map(ManagerProfile::getTeam);
    }

    public boolean isManaged(Team team) {
        return team != null && managedTeam().map(managed -> managed.getId().equals(team.getId())).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasOpenOffer(Player player) {
        return inboxMessageRepository.existsByTypeAndPlayerIdAndResolvedFalse(InboxMessageType.TRANSFER_OFFER,
                player.getId());
    }

    public InboxMessage post(InboxMessageType type, String title, String body, Player player, Team team, Long amount) {
        int seasonNumber = seasonRepository.findTopByOrderBySeasonNumberDesc().map(Season::getSeasonNumber).orElse(1);
        return inboxMessageRepository.save(InboxMessage.builder().type(type).title(title).body(body)
                .seasonNumber(seasonNumber).player(player).team(team).amount(amount).build());
    }

    @Transactional(readOnly = true)
    public List<InboxMessageResponse> list() {
        return inboxMessageRepository.findAllNewestFirst().stream().map(InboxService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return inboxMessageRepository.countByReadFalse();
    }

    public void markRead(Long id) {
        inboxMessageRepository.findById(id).ifPresent(message -> message.setRead(true));
    }

    public void markAllRead() {
        inboxMessageRepository.findAll().forEach(message -> message.setRead(true));
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
