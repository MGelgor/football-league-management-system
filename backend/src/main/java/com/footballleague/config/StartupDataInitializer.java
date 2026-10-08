package com.footballleague.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.footballleague.service.EconomyService;
import com.footballleague.service.ManagerService;
import com.footballleague.service.RefereeService;
import com.footballleague.service.TeamService;

import lombok.RequiredArgsConstructor;

/** Uygulama açılınca 4 büyükleri (ve eksik kadroları) hakem havuzunu, teknik direktörleri ve bütçe / sözleşmeleri veritabanına ekler. */
@Component
@RequiredArgsConstructor
public class StartupDataInitializer implements ApplicationRunner {

    private final TeamService teamService;
    private final RefereeService refereeService;
    private final ManagerService managerService;
    private final EconomyService economyService;

    @Override
    public void run(ApplicationArguments args) {
        teamService.ensureBigFourAndSquads();
        refereeService.ensureReferees();
        managerService.ensureManagers();
        economyService.ensureEconomy();
    }
}
