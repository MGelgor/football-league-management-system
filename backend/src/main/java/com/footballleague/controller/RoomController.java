package com.footballleague.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.footballleague.config.RoomFilter;
import com.footballleague.dto.CreateRoomRequest;
import com.footballleague.dto.JoinRoomRequest;
import com.footballleague.dto.RoomJoinResponse;
import com.footballleague.dto.RoomResponse;
import com.footballleague.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomJoinResponse create(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(request);
    }

    @PostMapping("/{code}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomJoinResponse join(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) {
        return roomService.join(code, request);
    }

    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable String code,
            @RequestHeader(value = RoomFilter.TOKEN_HEADER, required = false) String token) {
        return roomService.get(code, token);
    }

    @DeleteMapping("/{code}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String code,
            @RequestHeader(value = RoomFilter.TOKEN_HEADER, required = false) String token) {
        roomService.leave(code, token);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code,
            @RequestHeader(value = RoomFilter.TOKEN_HEADER, required = false) String token) {
        roomService.delete(code, token);
    }
}
