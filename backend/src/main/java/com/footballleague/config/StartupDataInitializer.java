package com.footballleague.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.footballleague.service.TeamService;

import lombok.RequiredArgsConstructor;

/** Uygulama açılınca 4 büyükleri (ve eksik kadroları) veritabanına ekler. */
@Component
@RequiredArgsConstructor
public class StartupDataInitializer implements ApplicationRunner {

    private final TeamService teamService;

    @Override
    public void run(ApplicationArguments args) {
        teamService.ensureBigFourAndSquads();
    }
}
