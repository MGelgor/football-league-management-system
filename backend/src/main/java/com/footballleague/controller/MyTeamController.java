package com.footballleague.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.footballleague.dto.AcademyPlayerResponse;
import com.footballleague.dto.ActionResponse;
import com.footballleague.dto.CareerResponse;
import com.footballleague.dto.ContractOfferRequest;
import com.footballleague.dto.InboxMessageResponse;
import com.footballleague.dto.LineupRequest;
import com.footballleague.dto.LineupResponse;
import com.footballleague.dto.LiveSessionResponse;
import com.footballleague.dto.MyTeamRequest;
import com.footballleague.dto.MyTeamResponse;
import com.footballleague.dto.SecondHalfRequest;
import com.footballleague.service.CareerService;
import com.footballleague.service.InboxService;
import com.footballleague.service.InteractiveMatchService;
import com.footballleague.service.MyTeamService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/my-team")
@RequiredArgsConstructor
@Tag(name = "My Team", description = "\"Takımımı Yönet\" modu: pano, kadro seçimi, canlı maç (devre arası müdahale)")
public class MyTeamController {

    private final MyTeamService myTeamService;
    private final InteractiveMatchService interactiveMatchService;
    private final CareerService careerService;
    private final InboxService inboxService;

    @GetMapping
    public MyTeamResponse dashboard() {
        return myTeamService.dashboard();
    }

    @PostMapping
    public MyTeamResponse start(@Valid @RequestBody MyTeamRequest request) {
        return myTeamService.start(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop() {
        myTeamService.stop();
    }

    @GetMapping("/lineup")
    public LineupResponse lineup() {
        return myTeamService.lineup();
    }

    @PutMapping("/lineup")
    public LineupResponse saveLineup(@Valid @RequestBody LineupRequest request) {
        return myTeamService.saveLineup(request);
    }

    /** Maçı canlı izlemeden oynat (hafta / kupa turu tamamen oynanır). */
    @PostMapping("/play")
    public MyTeamResponse quickPlay() {
        return myTeamService.quickPlay();
    }

    @PostMapping("/live/start")
    public LiveSessionResponse startLive() {
        return interactiveMatchService.start();
    }

    @PostMapping("/live/second-half")
    public LiveSessionResponse secondHalf(@Valid @RequestBody SecondHalfRequest request) {
        return interactiveMatchService.secondHalf(request);
    }

    @GetMapping("/inbox")
    public List<InboxMessageResponse> inbox() {
        return inboxService.list();
    }

    @PostMapping("/inbox/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() {
        inboxService.markAllRead();
    }

    @PostMapping("/inbox/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id) {
        inboxService.markRead(id);
    }

    /** Teklifi kabul: transfer teklifi (oyuncu satılır), iş teklifi (takımı devral), altyapı genci (terfi). */
    @PostMapping("/inbox/{id}/accept")
    public ActionResponse accept(@PathVariable Long id) {
        return careerService.accept(id);
    }

    @PostMapping("/inbox/{id}/reject")
    public ActionResponse reject(@PathVariable Long id) {
        return careerService.reject(id);
    }

    @PostMapping("/contracts/{playerId}")
    public ActionResponse renewContract(@PathVariable Long playerId, @Valid @RequestBody ContractOfferRequest request) {
        return careerService.renewContract(playerId, request);
    }

    @PostMapping("/sell/{playerId}")
    public ActionResponse listForSale(@PathVariable Long playerId) {
        return careerService.listForSale(playerId);
    }

    @GetMapping("/academy")
    public List<AcademyPlayerResponse> academy() {
        return careerService.academyProspects().stream()
                .map(player -> new AcademyPlayerResponse(player.getId(), player.getName(), player.getPosition(),
                        player.getAge(), player.getStrength()))
                .toList();
    }

    @GetMapping("/career")
    public CareerResponse career() {
        return careerService.career();
    }
}
