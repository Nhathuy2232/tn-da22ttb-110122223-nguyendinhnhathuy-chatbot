package com.kltn.chatbot.event;

import com.kltn.chatbot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lắng nghe RiskDetectedEvent và tự động gửi thông báo vào Moodle Message drawer.
 *
 * Dùng {@link TransactionPhase#AFTER_COMMIT} để đảm bảo warning đã được persist
 * trước khi gửi notification (tránh gửi thông báo cho warning bị rollback).
 *
 * {@code @Async} để không block luồng xử lý risk analysis.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRiskDetected(RiskDetectedEvent event) {
        if (event == null) return;

        log.info("Received RiskDetectedEvent: warningId={}, student={}, course={}, level={}",
                event.getWarningId(), event.getStudentId(), event.getCourseId(), event.getRiskLevel());

        try {
            int sent = notificationService.notifyFromEvent(event);
            log.info("Notification dispatch result: {} messages sent for warningId={}",
                    sent, event.getWarningId());
        } catch (Exception e) {
            log.error("Failed to send notification for warningId={}: {}",
                    event.getWarningId(), e.getMessage(), e);
        }
    }
}
