package com.footballleague.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footballleague.entity.Formation;
import com.footballleague.entity.Manager;
import com.footballleague.entity.Team;
import com.footballleague.repository.ManagerRepository;
import com.footballleague.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/** Her takımın bir teknik direktörü olur; takım dizilişi başlangıçta hocanın tercih ettiği dizilişti. */
@Service
@RequiredArgsConstructor
@Transactional
public class ManagerService {

    private static final int MIN_SKILL = 30;
    private static final int MAX_SKILL = 90;
    // 4 büyükler daha usta hocalarla başlar
    private static final int BIG_FOUR_MIN_SKILL = 60;
    private static final int BIG_FOUR_MAX_SKILL = 95;

    private final ManagerRepository managerRepository;
    private final TeamRepository teamRepository;
    private final SquadGenerator squadGenerator;

    /** Teknik direktörü olmayan aktif takımlara (bu özellikten önce oluşturulanlar dahil) hoca atar. */
    public void ensureManagers() {
        teamRepository.findByActiveTrue().stream()
                .filter(team -> team.getManager() == null)
                .forEach(this::hireFor);
    }

    public Manager hireFor(Team team) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Set<String> usedNames = managerRepository.findAll().stream().map(Manager::getName)
                .collect(Collectors.toCollection(HashSet::new));
        Formation preferred = List.of(Formation.values()).get(random.nextInt(Formation.values().length));
        Manager manager = managerRepository.save(Manager.builder()
                .name(squadGenerator.personName(usedNames))
                .tacticalSkill(team.isBigFour()
                        ? random.nextInt(BIG_FOUR_MIN_SKILL, BIG_FOUR_MAX_SKILL + 1)
                        : random.nextInt(MIN_SKILL, MAX_SKILL + 1))
                .preferredFormation(preferred)
                .build());
        team.setManager(manager);
        team.setFormation(preferred);
        return manager;
    }
}
