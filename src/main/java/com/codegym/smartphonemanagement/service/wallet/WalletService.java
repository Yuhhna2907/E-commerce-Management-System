package com.codegym.smartphonemanagement.service.wallet;

import com.codegym.smartphonemanagement.exception.BadRequestException;
import com.codegym.smartphonemanagement.exception.EntityNotFoundException;
import com.codegym.smartphonemanagement.model.*;
import com.codegym.smartphonemanagement.repository.SzWalletRepository;
import com.codegym.smartphonemanagement.repository.SzWalletTransactionRepository;
import com.codegym.smartphonemanagement.repository.SzWithdrawalRequestRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.service.wallet.dto.WalletDTO;
import com.codegym.smartphonemanagement.service.wallet.dto.WalletStatsDTO;
import com.codegym.smartphonemanagement.service.wallet.dto.WithdrawalRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final SzWalletRepository walletRepo;
    private final SzWalletTransactionRepository txRepo;
    private final SzWithdrawalRequestRepository withdrawalRepo;
    private final UserRepository userRepo;

    // ==================== CORE WALLET OPS ====================

    /** Lấy hoặc tạo ví cho user */
    @Transactional
    public SzWallet getOrCreate(Long userId) {
        return walletRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));
            SzWallet wallet = SzWallet.builder().user(user).build();
            SzWallet saved = walletRepo.save(wallet);
            log.info("Created new SzWallet for user {}", userId);
            return saved;
        });
    }

    /** Lấy WalletDTO cho user (tạo ví nếu chưa có) */
    @Transactional
    public WalletDTO getWalletDTO(Long userId) {
        return toDTO(getOrCreate(userId));
    }

    /**
     * Admin bơm tiền vào ví user (bồi thường, tặng thưởng...).
     */
    @Transactional
    public void adminTopUp(Long userId, BigDecimal amount, String adminNote) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền nạp phải lớn hơn 0!");
        }
        SzWallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalIn(wallet.getTotalIn().add(amount));
        walletRepo.save(wallet);

        saveTransaction(wallet, WalletTransactionType.ADMIN_TOP_UP, amount,
                adminNote != null ? adminNote : "Admin nạp tiền vào ví", adminNote, null);

        log.info("Admin top-up {} VND to wallet of user {}, new balance={}", amount, userId, wallet.getBalance());
    }

    /**
     * Hoàn tiền từ đơn hàng vào ví (tự động khi đơn COD bị huỷ/hoàn trả).
     */
    @Transactional
    public void refundToWallet(Long userId, BigDecimal amount, Long orderId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        SzWallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalIn(wallet.getTotalIn().add(amount));
        walletRepo.save(wallet);

        saveTransaction(wallet, WalletTransactionType.REFUND_IN, amount, description, null, orderId);
        log.info("Refund {} VND into wallet of user {} from order #{}", amount, userId, orderId);
    }

    /**
     * Thanh toán đơn hàng bằng SmartZone Xu.
     * @throws BadRequestException nếu số dư không đủ
     */
    @Transactional
    public void payByWallet(Long userId, BigDecimal amount, Long orderId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        SzWallet wallet = getOrCreate(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException(String.format(
                    "Số dư SmartZone Xu không đủ. Cần: %,.0f₫ | Có: %,.0f₫", amount, wallet.getBalance()));
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setTotalOut(wallet.getTotalOut().add(amount));
        walletRepo.save(wallet);

        saveTransaction(wallet, WalletTransactionType.PAYMENT, amount,
                "Thanh toán đơn hàng #" + orderId + " bằng SmartZone Xu", null, orderId);
        log.info("Wallet payment {} VND by user {} for order #{}", amount, userId, orderId);
    }

    // ==================== WITHDRAWAL ====================

    /**
     * User tạo yêu cầu rút tiền.
     */
    @Transactional
    public SzWithdrawalRequest requestWithdrawal(Long userId, BigDecimal amount,
                                                  String bankName, String bankAccount,
                                                  String bankHolder, String userNote) {
        if (amount.compareTo(BigDecimal.valueOf(50_000)) < 0) {
            throw new BadRequestException("Số tiền rút tối thiểu là 50.000₫!");
        }
        SzWallet wallet = getOrCreate(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Số dư SmartZone Xu không đủ để rút!");
        }

        // Tạm giữ tiền (trừ khỏi balance, chờ Admin duyệt)
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepo.save(wallet);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại: " + userId));

        SzWithdrawalRequest req = SzWithdrawalRequest.builder()
                .user(user)
                .amount(amount)
                .bankName(bankName)
                .bankAccount(bankAccount)
                .bankHolder(bankHolder)
                .userNote(userNote)
                .status(WithdrawalStatus.PENDING)
                .build();
        SzWithdrawalRequest saved = withdrawalRepo.save(req);

        saveTransaction(wallet, WalletTransactionType.WITHDRAWAL, amount,
                "Yêu cầu rút tiền #" + saved.getId() + " đang chờ duyệt", null, null);

        log.info("User {} requested withdrawal of {} VND (request #{})", userId, amount, saved.getId());
        return saved;
    }

    /**
     * Admin duyệt rút tiền — tiền đã bị trừ rồi, chỉ cập nhật status + ghi note.
     */
    @Transactional
    public void approveWithdrawal(Long requestId, String adminNote) {
        SzWithdrawalRequest req = getWithdrawalOrThrow(requestId);
        if (req.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý rồi!");
        }
        req.setStatus(WithdrawalStatus.APPROVED);
        req.setAdminNote(adminNote);
        req.setProcessedAt(LocalDateTime.now());
        withdrawalRepo.save(req);

        // Cập nhật totalOut trong ví
        SzWallet wallet = getOrCreate(req.getUser().getId());
        wallet.setTotalOut(wallet.getTotalOut().add(req.getAmount()));
        walletRepo.save(wallet);

        log.info("Admin APPROVED withdrawal #{} of {} VND for user {}", requestId, req.getAmount(), req.getUser().getId());
    }

    /**
     * Admin từ chối rút tiền — hoàn tiền lại vào ví.
     */
    @Transactional
    public void rejectWithdrawal(Long requestId, String adminNote) {
        SzWithdrawalRequest req = getWithdrawalOrThrow(requestId);
        if (req.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý rồi!");
        }
        req.setStatus(WithdrawalStatus.REJECTED);
        req.setAdminNote(adminNote);
        req.setProcessedAt(LocalDateTime.now());
        withdrawalRepo.save(req);

        // Hoàn tiền về ví user (tiền đã bị giữ khi tạo request)
        SzWallet wallet = getOrCreate(req.getUser().getId());
        wallet.setBalance(wallet.getBalance().add(req.getAmount()));
        walletRepo.save(wallet);

        saveTransaction(wallet, WalletTransactionType.REFUND_IN, req.getAmount(),
                "Hoàn tiền từ yêu cầu rút #" + requestId + " bị từ chối", adminNote, null);

        log.info("Admin REJECTED withdrawal #{}, refunded {} VND to user {}", requestId, req.getAmount(), req.getUser().getId());
    }

    // ==================== QUERIES ====================

    public Page<SzWalletTransaction> getTransactions(Long userId, Pageable pageable) {
        SzWallet wallet = getOrCreate(userId);
        return txRepo.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
    }

    public List<SzWithdrawalRequest> getPendingWithdrawals() {
        return withdrawalRepo.findByStatusOrderByCreatedAtAsc(WithdrawalStatus.PENDING);
    }

    public List<SzWalletTransaction> getRecentGlobalTransactions() {
        return txRepo.findTop10ByOrderByCreatedAtDesc();
    }

    public WalletStatsDTO getAdminStats() {
        BigDecimal totalBalance = walletRepo.sumAllBalances();
        long walletsWithBalance = walletRepo.countWalletsWithBalance();
        long pendingWithdrawals = withdrawalRepo.countByStatus(WithdrawalStatus.PENDING);

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal refundedThisMonth = txRepo.sumByTypeAndPeriod(WalletTransactionType.REFUND_IN, monthStart, now);
        BigDecimal withdrawnThisMonth = txRepo.sumByTypeAndPeriod(WalletTransactionType.WITHDRAWAL, monthStart, now);

        return WalletStatsDTO.builder()
                .totalBalanceInCirculation(totalBalance)
                .walletsWithBalance(walletsWithBalance)
                .pendingWithdrawals(pendingWithdrawals)
                .refundedThisMonth(refundedThisMonth)
                .withdrawnThisMonth(withdrawnThisMonth)
                .build();
    }

    // ==================== PRIVATE HELPERS ====================

    private SzWithdrawalRequest getWithdrawalOrThrow(Long id) {
        return withdrawalRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Yêu cầu rút tiền không tồn tại: " + id));
    }

    private void saveTransaction(SzWallet wallet, WalletTransactionType type, BigDecimal amount,
                                  String description, String adminNote, Long orderId) {
        SzWalletTransaction tx = SzWalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .adminNote(adminNote)
                .relatedOrderId(orderId)
                .build();
        txRepo.save(tx);
    }

    // ==================== MAPPING ====================

    public WalletDTO toDTO(SzWallet wallet) {
        User u = wallet.getUser();
        return WalletDTO.builder()
                .walletId(wallet.getId())
                .userId(u.getId())
                .username((u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getUsername())
                .email(u.getEmail())
                .balance(wallet.getBalance())
                .totalIn(wallet.getTotalIn())
                .totalOut(wallet.getTotalOut())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    public WithdrawalRequestDTO toWithdrawalDTO(SzWithdrawalRequest req) {
        return WithdrawalRequestDTO.builder()
                .id(req.getId())
                .userId(req.getUser().getId())
                .username(req.getUser().getFullName() != null ? req.getUser().getFullName() : req.getUser().getUsername())
                .email(req.getUser().getEmail())
                .amount(req.getAmount())
                .bankName(req.getBankName())
                .bankAccount(req.getBankAccount())
                .bankHolder(req.getBankHolder())
                .status(req.getStatus())
                .userNote(req.getUserNote())
                .adminNote(req.getAdminNote())
                .createdAt(req.getCreatedAt())
                .processedAt(req.getProcessedAt())
                .build();
    }
}
