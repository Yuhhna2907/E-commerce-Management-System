package com.codegym.smartphonemanagement.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lắng nghe các sự kiện Session để đếm số người đang online theo thời gian thực.
 * Mỗi khi có người mở trình duyệt truy cập hệ thống → Session được tạo → tăng số.
 * Khi Session hết hạn hoặc user logout → giảm số.
 */
@Component
@WebListener
@Slf4j
public class ActiveSessionListener implements HttpSessionListener {

    private static final AtomicInteger ACTIVE_SESSIONS = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        int count = ACTIVE_SESSIONS.incrementAndGet();
        log.debug("Session created. Active sessions: {}", count);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        int count = ACTIVE_SESSIONS.decrementAndGet();
        if (count < 0) {
            ACTIVE_SESSIONS.set(0); // Safety guard
        }
        log.debug("Session destroyed. Active sessions: {}", count);
    }

    /**
     * Trả về số người đang online (số session đang hoạt động).
     */
    public static int getActiveSessionCount() {
        return ACTIVE_SESSIONS.get();
    }
}
