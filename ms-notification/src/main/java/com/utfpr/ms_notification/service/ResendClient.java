package com.utfpr.ms_notification.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.utfpr.ms_notification.entity.NotificationLog;
import com.utfpr.ms_notification.repository.NotificationLogRepository;

@Service
public class ResendClient {

    private static final Logger log = LoggerFactory.getLogger(ResendClient.class);
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;
    private final NotificationLogRepository notificationLogRepository;
    private final String apiKey;

    public ResendClient(@Value("${resend.api-key:${RESEND_APY_KEY:}}") String apiKey, NotificationLogRepository notificationLogRepository) {
        this.restTemplate = new RestTemplate();
        this.notificationLogRepository = notificationLogRepository;
        this.apiKey = apiKey;
    }

    public void sendEmail(String to, String subject, String text, Long promotionId) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key not configured, skipping email to {}", to);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "from", "onboarding@resend.dev",
            "to", new String[]{to},
            "subject", subject,
            "text", text
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String status = "sent";
        String errorMessage = null;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_URL, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                status = "failed";
                errorMessage = response.getBody();
                log.error("Resend API returned {} for email to {}: {}", response.getStatusCode(), to, errorMessage);
            }
        } catch (Exception e) {
            status = "failed";
            errorMessage = e.getMessage();
            log.error("Failed to send email to {}: {}", to, errorMessage);
        }

        if ("sent".equals(status)) {
            log.info("Email sent successfully to {} (subject: {})", to, subject);
        }

        NotificationLog notifLog = new NotificationLog();
        notifLog.setRecipientEmail(to);
        notifLog.setPromotionId(promotionId);
        notifLog.setStatus(status);
        notifLog.setErrorMessage(errorMessage);
        notificationLogRepository.save(notifLog);
    }
}
