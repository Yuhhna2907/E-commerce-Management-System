package com.codegym.smartphonemanagement.service.loyalty;

import com.codegym.smartphonemanagement.model.Order;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyAccountDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.PointTransactionDTO;
import com.codegym.smartphonemanagement.service.loyalty.dto.RedeemResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ILoyaltyPointService {

    /**
     * Lấy hoặc tạo mới tài khoản điểm cho user
     */
    LoyaltyAccountDTO getOrCreateAccount(Long userId);

    /**
     * Cộng điểm khi đơn hàng chuyển sang DELIVERED.
     * Tỉ lệ: 1.000đ = 1 điểm (tính trên totalPrice sau discount)
     */
    void earnPoints(Long userId, Order order);

    /**
     * Trừ điểm khi đơn hàng bị REFUNDED/PARTIAL_REFUNDED.
     * Trừ đúng số điểm đã cộng từ đơn (order.pointsEarned), không để điểm âm
     */
    void deductPoints(Long userId, Order order);

    /**
     * Đổi điểm lấy coupon ưu đãi.
     * Tỉ lệ: 10 điểm = 1.000đ. Min: 100 điểm.
     * Trả về RedeemResultDTO chứa coupon code vừa tạo.
     */
    RedeemResultDTO redeemPoints(Long userId, int points);

    /**
     * Lấy thông tin tài khoản điểm của user
     */
    LoyaltyAccountDTO getAccountInfo(Long userId);

    /**
     * Lịch sử giao dịch điểm (phân trang)
     */
    Page<PointTransactionDTO> getTransactionHistory(Long userId, Pageable pageable);

    /**
     * Admin điều chỉnh điểm thủ công (delta có thể âm hoặc dương)
     */
    void adminAdjustPoints(Long userId, int delta, String reason);
}
