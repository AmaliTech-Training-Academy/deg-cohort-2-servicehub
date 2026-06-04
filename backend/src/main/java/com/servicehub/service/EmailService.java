package com.servicehub.service;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ServiceRequestRepository requestRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    @Async
    @Transactional(readOnly = true)
    public void sendStatusChangeToRequester(Long requestId, RequestStatus previous) {
        ServiceRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null) return;
        sendStatusChangeEmail(
                request.getRequester(),
                request,
                previous,
                "Your service request status has been updated"
        );
    }

    @Async
    @Transactional(readOnly = true)
    public void sendAssignmentNotificationToAgent(Long requestId) {
        ServiceRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null || request.getAssignedTo() == null) return;
        sendStatusChangeEmail(
                request.getAssignedTo(),
                request,
                RequestStatus.OPEN,
                "A service request has been assigned to you"
        );
    }

    private void sendStatusChangeEmail(User recipient, ServiceRequest request,
                                       RequestStatus previous, String subject) {
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipient.getFullName());
            ctx.setVariable("requestId", request.getId());
            ctx.setVariable("requestTitle", request.getTitle());
            ctx.setVariable("previousStatus", formatStatus(previous));
            ctx.setVariable("newStatus", formatStatus(request.getStatus()));
            ctx.setVariable("priority", request.getPriority().name());
            ctx.setVariable("category", formatCategory(request.getCategory().name()));
            ctx.setVariable("ticketUrl", frontendUrl + "/requests/" + request.getId());

            String html = templateEngine.process("email/status-change", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient.getEmail());
            helper.setSubject("[ServiceHub] " + subject + " — #" + request.getId());
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Status change email sent to {} for request #{}", recipient.getEmail(), request.getId());
        } catch (MessagingException e) {
            log.error("Failed to send status change email to {} for request #{}: {}",
                    recipient.getEmail(), request.getId(), e.getMessage());
        }
    }

    private String formatStatus(RequestStatus status) {
        return switch (status) {
            case OPEN        -> "Open";
            case ASSIGNED    -> "Assigned";
            case IN_PROGRESS -> "In Progress";
            case RESOLVED    -> "Resolved";
            case CLOSED      -> "Closed";
        };
    }

    private String formatCategory(String raw) {
        return raw.replace("_", " ").toLowerCase()
                .substring(0, 1).toUpperCase()
                + raw.replace("_", " ").toLowerCase().substring(1);
    }
}
