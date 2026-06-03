package com.servicehub.controller;

import com.servicehub.dto.CommentRequest;
import com.servicehub.dto.CommentResponse;
import com.servicehub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests/{requestId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'EMPLOYEE')")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long requestId,
            @Valid @RequestBody CommentRequest dto,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(requestId, dto, email));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long requestId) {
        return ResponseEntity.ok(commentService.getComments(requestId));
    }
}
