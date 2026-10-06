package com.footballleague.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.dto.RecordResponse;
import com.footballleague.service.RecordService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Records", description = "Tüm sezonların lig maçlarından tarihsel rekorlar")
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    public List<RecordResponse> getRecords() {
        return recordService.getRecords();
    }
}
