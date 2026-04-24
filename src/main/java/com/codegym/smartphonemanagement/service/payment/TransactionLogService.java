package com.codegym.smartphonemanagement.service.payment;

import com.codegym.smartphonemanagement.model.TransactionLog;
import com.codegym.smartphonemanagement.model.TransactionLogStatus;
import com.codegym.smartphonemanagement.model.TransactionLogType;
import com.codegym.smartphonemanagement.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransaction(String transactionReference, TransactionLogType type, BigDecimal amount,
                               Long userId, TransactionLogStatus status, String description) {
        log.info("Logging transaction: ref={}, type={}, amount={}, userId={}, status={}",
                transactionReference, type, amount, userId, status);

        try {
            String ipAddress = getClientIpAddress();

            TransactionLog logEntry = TransactionLog.builder()
                    .transactionReference(transactionReference)
                    .type(type)
                    .amount(amount)
                    .userId(userId)
                    .status(status)
                    .ipAddress(ipAddress)
                    .description(description)
                    .build();

            transactionLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log transaction {}", transactionReference, e);
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
                    return request.getRemoteAddr();
                }
                return xfHeader.split(",")[0];
            }
        } catch (Exception e) {
            log.warn("Could not get client IP address", e);
        }
        return "UNKNOWN";
    }
}
