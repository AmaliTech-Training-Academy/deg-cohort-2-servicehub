package com.servicehub.controller;

import com.servicehub.config.JwtService;
import com.servicehub.exception.NotFoundException;
import com.servicehub.exception.UnauthorizedException;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseNotificationService notificationService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Invalid or missing token");
        }
        String email = jwtService.extractEmail(token);
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getId();
        return notificationService.subscribe(userId);
    }
}
