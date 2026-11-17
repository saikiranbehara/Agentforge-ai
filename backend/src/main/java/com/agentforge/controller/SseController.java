package com.agentforge.controller;

import com.agentforge.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseNotificationService sseNotificationService;

    @GetMapping(value = "/projects/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToProjectEvents(@PathVariable Long id) {
        return sseNotificationService.subscribe(id);
    }
}
