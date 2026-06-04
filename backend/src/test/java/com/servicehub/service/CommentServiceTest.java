package com.servicehub.service;

import com.servicehub.dto.CommentRequest;
import com.servicehub.dto.CommentResponse;
import com.servicehub.exception.ForbiddenException;
import com.servicehub.exception.NotFoundException;
import com.servicehub.model.Comment;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.CommentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private ServiceRequestRepository requestRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CommentService commentService;

    private User agent;
    private User employee;
    private User otherEmployee;
    private ServiceRequest request;

    @BeforeEach
    void setUp() {
        agent = User.builder().id(1L).email("agent@test.com")
                .fullName("Agent One").role(Role.AGENT).password("pass").build();

        employee = User.builder().id(2L).email("emp@test.com")
                .fullName("Test Employee").role(Role.EMPLOYEE).password("pass").build();

        otherEmployee = User.builder().id(3L).email("other@test.com")
                .fullName("Other Employee").role(Role.EMPLOYEE).password("pass").build();

        request = ServiceRequest.builder()
                .id(1L).title("Fix printer")
                .category(RequestCategory.IT_SUPPORT).priority(Priority.HIGH)
                .status(RequestStatus.OPEN).requester(employee)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addComment_byAgent_savesAndReturnsResponse() {
        CommentRequest dto = new CommentRequest();
        dto.setBody("Looking into this now");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(commentRepository.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c = Comment.builder().id(10L).request(c.getRequest()).author(c.getAuthor())
                    .body(c.getBody()).systemGenerated(c.isSystemGenerated())
                    .createdAt(c.getCreatedAt()).build();
            return c;
        });

        CommentResponse result = commentService.addComment(1L, dto, "agent@test.com");

        assertThat(result.getBody()).isEqualTo("Looking into this now");
        assertThat(result.getAuthorName()).isEqualTo("Agent One");
        assertThat(result.isSystemGenerated()).isFalse();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void addComment_byEmployeeOnOwnRequest_succeeds() {
        CommentRequest dto = new CommentRequest();
        dto.setBody("Any update?");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse result = commentService.addComment(1L, dto, "emp@test.com");

        assertThat(result.getAuthorName()).isEqualTo("Test Employee");
    }

    @Test
    void addComment_byEmployeeOnOthersRequest_throwsForbidden() {
        CommentRequest dto = new CommentRequest();
        dto.setBody("Sneaky comment");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherEmployee));

        assertThatThrownBy(() -> commentService.addComment(1L, dto, "other@test.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own requests");
    }

    @Test
    void addComment_requestNotFound_throwsNotFoundException() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(99L, new CommentRequest(), "agent@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Request not found");
    }

    @Test
    void addComment_userNotFound_throwsNotFoundException() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(1L, new CommentRequest(), "nobody@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getComments_returnsListOldestFirst() {
        LocalDateTime t1 = LocalDateTime.now().minusMinutes(10);
        LocalDateTime t2 = LocalDateTime.now();

        Comment c1 = Comment.builder().id(1L).author(agent).body("First").systemGenerated(true).createdAt(t1).build();
        Comment c2 = Comment.builder().id(2L).author(employee).body("Second").systemGenerated(false).createdAt(t2).build();

        when(requestRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findByRequestIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c1, c2));

        List<CommentResponse> result = commentService.getComments(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBody()).isEqualTo("First");
        assertThat(result.get(0).isSystemGenerated()).isTrue();
        assertThat(result.get(1).getBody()).isEqualTo("Second");
    }

    @Test
    void getComments_requestNotFound_throwsNotFoundException() {
        when(requestRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getComments(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Request not found");
    }

    @Test
    void getComments_systemGeneratedEntryIncluded() {
        Comment systemComment = Comment.builder().id(1L).author(agent)
                .body("[OPEN → ASSIGNED] Picked up").systemGenerated(true)
                .createdAt(LocalDateTime.now()).build();

        when(requestRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findByRequestIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(systemComment));

        List<CommentResponse> result = commentService.getComments(1L);

        assertThat(result.get(0).isSystemGenerated()).isTrue();
        assertThat(result.get(0).getBody()).contains("ASSIGNED");
    }
}
