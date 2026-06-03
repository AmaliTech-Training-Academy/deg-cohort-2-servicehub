package com.servicehub.service;

import com.servicehub.dto.CommentRequest;
import com.servicehub.dto.CommentResponse;
import com.servicehub.exception.ForbiddenException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.model.Comment;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.CommentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;

    public CommentResponse addComment(Long requestId, CommentRequest dto, String email) {
        var request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (author.getRole() == Role.EMPLOYEE
                && !request.getRequester().getId().equals(author.getId())) {
            throw new ForbiddenException("Employees can only comment on their own requests");
        }

        Comment comment = Comment.builder()
                .request(request)
                .author(author)
                .body(dto.getBody())
                .systemGenerated(false)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getComments(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new NotFoundException("Request not found");
        }
        return commentRepository.findByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(this::toResponse)
                .toList();
    }

    CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorName(comment.getAuthor().getFullName())
                .body(comment.getBody())
                .systemGenerated(comment.isSystemGenerated())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
