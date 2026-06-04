package com.servicehub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseNotificationService {

    public static final String EVENT_TICKET_UPDATED = "TICKET_UPDATED";
    public static final String EVENT_TICKET_ASSIGNED = "TICKET_ASSIGNED";
    public static final String EVENT_SLA_BREACHED   = "SLA_BREACHED";

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ObjectMapper objectMapper;

    // List per userId so multiple browser tabs all receive the event.
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final AtomicLong eventIdCounter = new AtomicLong(0);

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> removeEmitter(userId, emitter));

        return emitter;
    }

    public void notify(Long userId, String eventType, Long requestId, String detail) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "type", eventType,
                    "requestId", requestId,
                    "detail", detail
            ));
        } catch (Exception e) {
            log.warn("Failed to serialize SSE payload for user {}", userId, e);
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(eventIdCounter.incrementAndGet()))
                        .name(eventType)
                        .data(json, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                // Catches both IOException (client disconnect) and IllegalStateException
                // (emitter already completed — e.g. browser tab closed before callback fired)
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) {
            userEmitters.removeAll(dead);
            if (userEmitters.isEmpty()) emitters.remove(userId);
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) emitters.remove(userId);
        }
    }
}
