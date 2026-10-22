package com.footballleague.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.config.RoomContext;
import com.footballleague.entity.ManagerProfile;
import com.footballleague.entity.Team;
import com.footballleague.repository.ManagerProfileRepository;

/**
 * İnsan menajerler. "Şu anki menajer": odada isteği yapan üyenin profili, tek kişilik ligde tek profil;
 * zamanlayıcının işinde (odada üye yok) boştur. "İnsan takımı": herhangi bir menajerin yönettiği takım;
 * diğer takımları yapay zekâ (bot) yönetir. Yalnızca repository'ye bağımlı (döngüsel bağımlılık olmasın).
 */
@Service
@Transactional(readOnly = true)
public class Managers {

    private final ManagerProfileRepository managerProfileRepository;

    public Managers(ManagerProfileRepository managerProfileRepository) {
        this.managerProfileRepository = managerProfileRepository;
    }

    public Optional<ManagerProfile> current() {
        RoomContext.Current room = RoomContext.get();
        if (room == null) {
            return managerProfileRepository.findFirstByOrderByIdAsc();
        }
        return room.memberId() == null ? Optional.empty() : managerProfileRepository.findByMemberId(room.memberId());
    }

    /** Şu anki menajer bir takımı yönetiyorsa. */
    public Optional<ManagerProfile> currentWithTeam() {
        return current().filter(profile -> profile.getTeam() != null);
    }

    public Optional<Team> currentTeam() {
        return currentWithTeam().map(ManagerProfile::getTeam);
    }

    /** Takımı yöneten menajerler (sezon olayları her biri için işlenir). */
    public List<ManagerProfile> withTeam() {
        return managerProfileRepository.findWithTeam();
    }

    public Optional<ManagerProfile> ofTeam(Team team) {
        return team == null ? Optional.empty() : managerProfileRepository.findByTeamId(team.getId());
    }

    public boolean isHuman(Team team) {
        return ofTeam(team).isPresent();
    }

    /** Odadaki üye → yönettiği takımın adı. */
    public Map<Long, String> teamNamesByMember() {
        return withTeam().stream().filter(profile -> profile.getMemberId() != null)
                .collect(Collectors.toMap(ManagerProfile::getMemberId, profile -> profile.getTeam().getName()));
    }

    public Set<Long> humanTeamIds() {
        return withTeam().stream().map(profile -> profile.getTeam().getId()).collect(Collectors.toSet());
    }
}
