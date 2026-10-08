package com.footballleague.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.MarketPlayerResponse;
import com.footballleague.dto.OfferRequest;
import com.footballleague.dto.OfferResponse;
import com.footballleague.dto.SignRequest;
import com.footballleague.dto.TransferResponse;
import com.footballleague.dto.TransferWindowResponse;
import com.footballleague.service.TransferService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Transfer penceresi, piyasa, teklifler ve serbest oyuncular")
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/window")
    public TransferWindowResponse window() {
        return transferService.window();
    }

    @GetMapping("/market")
    public List<MarketPlayerResponse> market() {
        return transferService.market();
    }

    @GetMapping
    public List<TransferResponse> recent() {
        return transferService.recentTransfers();
    }

    @PostMapping("/offers")
    public OfferResponse offer(@Valid @RequestBody OfferRequest request) {
        return transferService.makeOffer(request);
    }

    @PostMapping("/free-agents/{playerId}/sign")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse sign(@PathVariable Long playerId, @Valid @RequestBody SignRequest request) {
        return transferService.signFreeAgent(playerId, request.teamId());
    }
}
