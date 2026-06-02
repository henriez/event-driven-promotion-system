package com.utfpr.ms_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE client disconnected, active: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("SSE client timed out, active: {}", emitters.size());
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.warn("SSE client error, active: {}", emitters.size());
        });
        log.info("SSE client connected, active: {}", emitters.size());
        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder builder = SseEmitter.event();
                if (eventName != null) {
                    builder.name(eventName);
                }
                emitter.send(builder.data(data));
            } catch (Exception e) {
                emitters.remove(emitter);
                log.warn("Failed to send SSE event, removed emitter: {}", e.toString());
            }
        }
    }
}
