package com.agentforge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseNotificationService {

    // ProjectId -> List of active SseEmitters
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long projectId) {
        // 30-minute timeout for agent execution streaming
        SseEmitter emitter = new SseEmitter(1800_000L);

        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(projectId, emitter));
        emitter.onTimeout(() -> removeEmitter(projectId, emitter));
        emitter.onError(e -> removeEmitter(projectId, emitter));

        try {
            // Initial handshake ping
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "Connected to AgentForge real-time monitoring stream for project " + projectId)));
        } catch (IOException e) {
            removeEmitter(projectId, emitter);
        }

        return emitter;
    }

    public void broadcastEvent(Long projectId, String eventName, Object data) {
        var projectEmitters = emitters.get(projectId);
        if (projectEmitters == null || projectEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : projectEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                removeEmitter(projectId, emitter);
            }
        }
    }

    private void removeEmitter(Long projectId, SseEmitter emitter) {
        var projectEmitters = emitters.get(projectId);
        if (projectEmitters != null) {
            projectEmitters.remove(emitter);
            if (projectEmitters.isEmpty()) {
                emitters.remove(projectId);
            }
        }
    }
}
