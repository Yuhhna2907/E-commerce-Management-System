package com.codegym.smartphonemanagement.service.notification;

import com.codegym.smartphonemanagement.model.NotificationType;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.model.MemberTier;
import com.codegym.smartphonemanagement.repository.user.LoyaltyAccountRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.EmailService;
import com.codegym.smartphonemanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final UserRepository userRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Executes the broadcast task in the background.
     * Use target "ALL", "BRONZE", "SILVER", "GOLD", "DIAMOND", or "SPECIFIC:username"
     */
    @Async("broadcastExecutor")
    public void executeBroadcast(String target, String subject, String message, String actionLink, boolean sendAppNotification, boolean sendEmail) {
        log.info("Starting broadcast. Target: {}, AppNotify: {}, EmailMap: {}", target, sendAppNotification, sendEmail);

        List<User> targetUsers = getTargetUsers(target);
        log.info("Found {} users matching the target criteria.", targetUsers.size());

        int count = 0;
        for (User user : targetUsers) {
            // Personalize the message
            String personalizedMessage = message
                    .replace("{{fullName}}", user.getFullName() != null ? user.getFullName() : user.getUsername())
                    .replace("{{username}}", user.getUsername());

            String personalizedSubject = subject
                    .replace("{{fullName}}", user.getFullName() != null ? user.getFullName() : user.getUsername())
                    .replace("{{username}}", user.getUsername());

            if (sendAppNotification) {
                try {
                    notificationService.sendNotification(user, personalizedMessage, NotificationType.PROMOTION, actionLink);
                } catch (Exception e) {
                    log.error("Error sending app notification to {}", user.getUsername(), e);
                }
            }

            if (sendEmail && user.getEmail() != null && !user.getEmail().isEmpty()) {
                try {
                    emailService.sendPromotionalEmail(user.getEmail(), user.getFullName() != null ? user.getFullName() : user.getUsername(), personalizedSubject, personalizedMessage, actionLink);
                } catch (Exception e) {
                    log.error("Error sending promotional email to {}", user.getEmail(), e);
                }
            }

            count++;
            if (count % 100 == 0) {
                log.info("Broadcast progress: {} / {} processed.", count, targetUsers.size());
            }
        }

        log.info("Broadcast execution completed. Processed {} users.", targetUsers.size());
    }

    private List<User> getTargetUsers(String target) {
        if ("ALL".equalsIgnoreCase(target)) {
            return userRepository.findAll();
        } else if (target.startsWith("SPECIFIC:")) {
            String username = target.split(":")[1].trim();
            return userRepository.findByUsername(username).map(List::of).orElse(List.of());
        }

        // Tier logic
        try {
            MemberTier tier = MemberTier.valueOf(target.toUpperCase());
            Integer minPoints = tier.getMinLifetimePoints();
            int nextOrdinal = tier.ordinal() + 1;

            if (nextOrdinal < MemberTier.values().length) {
                Integer maxPoints = MemberTier.values()[nextOrdinal].getMinLifetimePoints();
                return loyaltyAccountRepository.findUsersByLifetimePointsBetween(minPoints, maxPoints);
            } else {
                return loyaltyAccountRepository.findUsersByLifetimePointsGreaterThanEqual(minPoints);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unknown target: {}. Sending to empty list.", target);
            return new ArrayList<>();
        }
    }
}
