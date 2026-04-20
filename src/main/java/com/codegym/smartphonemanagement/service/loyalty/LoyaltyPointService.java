package com.codegym.smartphonemanagement.service.loyalty;

import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.service.loyalty.dto.LoyaltyStatsDTO;
import com.codegym.smartphonemanagement.repository.CouponRepository;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.loyalty.dto.*;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyPointService implements ILoyaltyPointService {

    // ======================== CONSTANTS ========================
    /**
     * Tỉ lệ tích điểm: 1.000đ = 1 điểm
     * Ví dụ: đơn 500.000đ → 500 điểm
     */
    private static final int EARN_RATE_PER_VND = 1_000;

    /**
     * Tỉ lệ quy đổi: 10 điểm = 1.000đ
     * Ví dụ: 500 điểm → 50.000đ coupon
     */
    private static final int REDEEM_POINTS_PER_1000VND = 10;

    /**
     * Số điểm tối thiểu để có thể đổi
     */
    private static final int MIN_REDEEM_POINTS = 100;

    // ======================== DEPENDENCIES ========================

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    // ======================== PUBLIC API ========================

    @Override
    @Transactional
    public LoyaltyAccountDTO getOrCreateAccount(Long userId) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
                .orElseGet(() -> createNewAccount(userId));
        return toDTO(account);
    }

    @Override
    @Transactional
    public void earnPoints(Long userId, Order order) {
        if (order == null || order.getTotalPrice() == null) return;

        // Tính điểm: floor(totalPrice / 1000)
        int points = order.getTotalPrice()
                .divide(BigDecimal.valueOf(EARN_RATE_PER_VND), 0, java.math.RoundingMode.FLOOR)
                .intValue();

        if (points <= 0) {
            log.debug("Order #{} earns 0 points (totalPrice={}), skipping", order.getId(), order.getTotalPrice());
            return;
        }

        LoyaltyAccount account = getOrCreateAccountEntity(userId);

        // === TIER BONUS: Nhân hệ số theo hạng thành viên ===
        MemberTier tier = MemberTier.fromLifetimePoints(account.getLifetimePoints());
        int bonusPoints = (int) Math.floor(points * tier.getBonusMultiplier());
        if (bonusPoints != points) {
            log.info("Tier [{}] bonus applied: base={} pts → after bonus={} pts (x{})",
                    tier.getLabel(), points, bonusPoints, tier.getBonusMultiplier());
        }
        points = bonusPoints;
        // =====================================================

        account.setTotalPoints(account.getTotalPoints() + points);
        account.setLifetimePoints(account.getLifetimePoints() + points);
        loyaltyAccountRepository.save(account);

        // Lưu lại số điểm đã cộng vào đơn hàng (để trừ chính xác khi refund)
        order.setPointsEarned(points);

        // Ghi log giao dịch
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));
        String tierNote = tier != MemberTier.BRONZE ? String.format(" (Bonus hạng %s x%.2f)", tier.getLabel(), tier.getBonusMultiplier()) : "";
        saveTransaction(user, order, PointTransactionType.EARNED, points,
                String.format("Tích %d điểm từ đơn hàng #%d%s", points, order.getId(), tierNote));

        log.info("Earned {} points (tier bonus={}) for user {} from order #{}", points, tier.getLabel(), userId, order.getId());
    }

    @Override
    @Transactional
    public void deductPoints(Long userId, Order order) {
        if (order == null) return;

        int pointsToDeduct = order.getPointsEarned() != null ? order.getPointsEarned() : 0;
        if (pointsToDeduct <= 0) {
            log.debug("Order #{} has no points to deduct, skipping", order.getId());
            return;
        }

        LoyaltyAccount account = getOrCreateAccountEntity(userId);

        // Không để điểm âm
        int actualDeduction = Math.min(pointsToDeduct, account.getTotalPoints());
        account.setTotalPoints(account.getTotalPoints() - actualDeduction);
        loyaltyAccountRepository.save(account);

        // Ghi log giao dịch (points âm để phân biệt)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));
        saveTransaction(user, order, PointTransactionType.REFUND_DEDUCTED, -actualDeduction,
                String.format("Trừ %d điểm do hoàn trả đơn hàng #%d", actualDeduction, order.getId()));

        log.info("Deducted {} points from user {} due to refund of order #{}", actualDeduction, userId, order.getId());
    }

    @Override
    @Transactional
    public RedeemResultDTO redeemPoints(Long userId, int points) {
        // Validation
        if (points < MIN_REDEEM_POINTS) {
            throw new BadRequestException(
                    String.format("Cần tối thiểu %d điểm để đổi ưu đãi. Bạn muốn đổi %d điểm.", MIN_REDEEM_POINTS, points));
        }

        // FIX #13: Wrap trong try-catch để handle optimistic lock exception
        try {
            LoyaltyAccount account = getOrCreateAccountEntity(userId);
            if (account.getTotalPoints() < points) {
                throw new BadRequestException(
                        String.format("Điểm không đủ. Bạn có %d điểm, cần %d điểm.", account.getTotalPoints(), points));
            }

            // Tính giá trị coupon: 10 điểm = 1.000đ
            long discountVnd = (long) points / REDEEM_POINTS_PER_1000VND * 1_000L;
            BigDecimal discountValue = BigDecimal.valueOf(discountVnd);

            // Tạo Coupon phần thưởng với code unique
            String couponCode = generateRewardCouponCode(userId);
            Coupon rewardCoupon = Coupon.builder()
                    .code(couponCode)
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(discountValue)
                    .minOrderValue(BigDecimal.ZERO)
                    .maxUsageGlobal(1)          // Chỉ dùng được 1 lần
                    .perUserLimit(1)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusDays(30)) // Coupon hết hạn sau 30 ngày
                    .status(CouponStatus.ACTIVE)
                    .description(String.format("Phần thưởng đổi %d điểm tích lũy", points))
                    .build();
            couponRepository.save(rewardCoupon);

            // Trừ điểm
            account.setTotalPoints(account.getTotalPoints() - points);
            loyaltyAccountRepository.save(account);

            // Ghi log giao dịch
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));
            saveTransaction(user, null, PointTransactionType.REDEEMED, -points,
                    String.format("Đổi %d điểm lấy coupon %s (trị giá %,.0fđ)", points, couponCode, discountVnd));

            log.info("User {} redeemed {} points for coupon {} ({}đ)", userId, points, couponCode, discountVnd);

            return RedeemResultDTO.builder()
                    .couponCode(couponCode)
                    .discountValue(discountValue)
                    .pointsUsed(points)
                    .remainingPoints(account.getTotalPoints())
                    .message(String.format("Đổi điểm thành công! Coupon %s trị giá %,.0fđ đã được thêm vào tài khoản của bạn.", couponCode, (double) discountVnd))
                    .build();
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // FIX #13: Handle race condition khi đổi điểm
            throw new RuntimeException("Có người khác đang thao tác với điểm tích lũy của bạn. Vui lòng thử lại.");
        }
    }

    @Override
    public LoyaltyAccountDTO getAccountInfo(Long userId) {
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(userId)
                .orElse(null);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));

        if (account == null) {
            String displayName = (u.getFullName() != null && !u.getFullName().isBlank())
                    ? u.getFullName() : u.getUsername();

            return LoyaltyAccountDTO.builder()
                    .userId(u.getId())
                    .username(displayName)
                    .email(u.getEmail())
                    .fullName(u.getFullName())
                    .totalPoints(0)
                    .lifetimePoints(0)
                    .estimatedValue(BigDecimal.ZERO)
                    .tier(MemberTier.BRONZE)
                    .tierLabel(MemberTier.BRONZE.getLabel())
                    .tierColor(MemberTier.BRONZE.getColor())
                    .tierIcon(MemberTier.BRONZE.getIcon())
                    .bonusMultiplier(MemberTier.BRONZE.getBonusMultiplier())
                    .progressToNextTier(MemberTier.progressToNextTier(0))
                    .pointsToNextTier(MemberTier.pointsToNextTier(0))
                    .build();
        }
        return toDTO(account);
    }

    @Override
    public Page<PointTransactionDTO> getTransactionHistory(Long userId, Pageable pageable) {
        return pointTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toTransactionDTO);
    }

    @Override
    @Transactional
    public void adminAdjustPoints(Long userId, int delta, String reason) {
        if (delta == 0) throw new BadRequestException("Delta không được bằng 0");

        LoyaltyAccount account = getOrCreateAccountEntity(userId);

        int newTotal = account.getTotalPoints() + delta;
        if (newTotal < 0) {
            throw new BadRequestException(
                    String.format("Không thể trừ %d điểm. User chỉ có %d điểm.", Math.abs(delta), account.getTotalPoints()));
        }

        account.setTotalPoints(newTotal);
        // Chỉ cộng lifetime khi delta dương
        if (delta > 0) {
            account.setLifetimePoints(account.getLifetimePoints() + delta);
        }
        loyaltyAccountRepository.save(account);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));
        saveTransaction(user, null, PointTransactionType.ADMIN_ADJUST, delta,
                reason != null ? reason : "Admin điều chỉnh điểm thủ công");

        log.info("Admin adjusted {} points for user {}. New total: {}", delta, userId, newTotal);
    }

    // ======================== PRIVATE HELPERS ========================

    private LoyaltyAccount getOrCreateAccountEntity(Long userId) {
        return loyaltyAccountRepository.findByUserId(userId)
                .orElseGet(() -> createNewAccount(userId));
    }

    private LoyaltyAccount createNewAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));
        LoyaltyAccount account = LoyaltyAccount.builder()
                .user(user)
                .totalPoints(0)
                .lifetimePoints(0)
                .build();
        LoyaltyAccount saved = loyaltyAccountRepository.save(account);
        log.info("Created new loyalty account for user {}", userId);
        return saved;
    }

    private void saveTransaction(User user, Order order, PointTransactionType type, int points, String description) {
        PointTransaction tx = PointTransaction.builder()
                .user(user)
                .order(order)
                .type(type)
                .points(points)
                .description(description)
                .build();
        pointTransactionRepository.save(tx);
    }

    /**
     * Tạo code coupon phần thưởng unique
     * Format: REWARD-{userId}-{4 ký tự random viết hoa}
     */
    private String generateRewardCouponCode(Long userId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return String.format("REWARD-%d-%s", userId, suffix);
    }

    // ======================== MAPPING ========================

    private LoyaltyAccountDTO toDTO(LoyaltyAccount account) {
        // Giá trị quy đổi: 10 điểm = 1.000đ
        BigDecimal estimatedValue = BigDecimal.valueOf(
                (long) account.getTotalPoints() / REDEEM_POINTS_PER_1000VND * 1_000L);

        // Tier enrichment
        MemberTier tier = MemberTier.fromLifetimePoints(account.getLifetimePoints());
        Integer pointsToNext = MemberTier.pointsToNextTier(account.getLifetimePoints());
        int progress = MemberTier.progressToNextTier(account.getLifetimePoints());

        // User info (populate for admin views)
        User u = account.getUser();
        String displayName = (u.getFullName() != null && !u.getFullName().isBlank())
                ? u.getFullName() : u.getUsername();

        return LoyaltyAccountDTO.builder()
                .totalPoints(account.getTotalPoints())
                .lifetimePoints(account.getLifetimePoints())
                .estimatedValue(estimatedValue)
                .updatedAt(account.getUpdatedAt())
                // user identity
                .userId(u.getId())
                .username(displayName)
                .email(u.getEmail())
                .fullName(u.getFullName())
                // tier
                .tier(tier)
                .tierLabel(tier.getLabel())
                .tierColor(tier.getColor())
                .tierIcon(tier.getIcon())
                .bonusMultiplier(tier.getBonusMultiplier())
                .pointsToNextTier(pointsToNext)
                .progressToNextTier(progress)
                .build();
    }

    /**
     * Aggregate stats cho Admin Dashboard Loyalty.
     */
    public LoyaltyStatsDTO getLoyaltyStats() {
        long totalAccounts = userRepository.countStandardUsers();

        // Tổng điểm đang lưu hành
        long totalPointsInCirculation = loyaltyAccountRepository.findAll()
                .stream().mapToLong(a -> a.getTotalPoints()).sum();

        // Tổng lifetime points
        long totalLifetimePoints = loyaltyAccountRepository.findAll()
                .stream().mapToLong(a -> a.getLifetimePoints()).sum();

        // Điểm đã đổi (REDEEMED) — tất cả thời gian
        long totalPointsRedeemed = pointTransactionRepository.findAll()
                .stream()
                .filter(t -> t.getType() == PointTransactionType.REDEEMED)
                .mapToLong(t -> Math.abs(t.getPoints()))
                .sum();

        // Giá trị điểm đang lưu hành quy ra tiền
        BigDecimal circulationValue = BigDecimal.valueOf(totalPointsInCirculation / REDEEM_POINTS_PER_1000VND * 1_000L);

        return LoyaltyStatsDTO.builder()
                .totalAccounts(totalAccounts)
                .totalPointsInCirculation(totalPointsInCirculation)
                .totalLifetimePoints(totalLifetimePoints)
                .totalPointsRedeemed(totalPointsRedeemed)
                .circulationValue(circulationValue)
                .build();
    }

    private PointTransactionDTO toTransactionDTO(PointTransaction tx) {
        String typeDisplay;
        String typeIcon;
        String typeCssClass;

        switch (tx.getType()) {
            case EARNED:
                typeDisplay = "Tích điểm";
                typeIcon = "bi-star-fill";
                typeCssClass = "text-success";
                break;
            case REDEEMED:
                typeDisplay = "Đổi điểm";
                typeIcon = "bi-gift-fill";
                typeCssClass = "text-warning";
                break;
            case REFUND_DEDUCTED:
                typeDisplay = "Trừ điểm hoàn trả";
                typeIcon = "bi-arrow-counterclockwise";
                typeCssClass = "text-danger";
                break;
            case ADMIN_ADJUST:
                typeDisplay = "Điều chỉnh";
                typeIcon = "bi-sliders";
                typeCssClass = tx.getPoints() >= 0 ? "text-info" : "text-danger";
                break;
            default:
                typeDisplay = tx.getType().name();
                typeIcon = "bi-circle";
                typeCssClass = "text-secondary";
        }

        return PointTransactionDTO.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .typeDisplay(typeDisplay)
                .typeIcon(typeIcon)
                .typeCssClass(typeCssClass)
                .points(tx.getPoints())
                .description(tx.getDescription())
                .orderId(tx.getOrder() != null ? tx.getOrder().getId() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
