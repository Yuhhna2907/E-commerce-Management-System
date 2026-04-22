package com.codegym.smartphonemanagement.service.dashboard.seller;

import com.codegym.smartphonemanagement.model.LoyaltyAccount;
import com.codegym.smartphonemanagement.model.MemberTier;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.dashboard.DTO.CustomerAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAnalyticsService {

    private final UserRepository userRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final OrderRepository orderRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserWalletRepository userWalletRepository;
    private final ReviewRepository reviewRepository;
    private final ProductQuestionRepository questionRepository;
    private final UserAddressRepository addressRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "customerAnalytics", key = "'allTime'") // using simple cache key as data is generic
    public CustomerAnalyticsDTO getCustomerAnalytics() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // KPI Headers
        long totalUsers = userRepository.count();
        long newUsers = userRepository.countNewUsersSince(thirtyDaysAgo);
        long bannedUsers = userRepository.countBannedOrRiskyUsers();

        // Demographics
        List<Object[]> genderData = userRepository.countByGender();
        long male = 0, female = 0, other = 0;
        for (Object[] r : genderData) {
            String gen = r[0] != null ? r[0].toString() : "OTHER";
            long count = ((Number) r[1]).longValue();
            if (gen.equalsIgnoreCase("MALE") || gen.equalsIgnoreCase("NAM")) male += count;
            else if (gen.equalsIgnoreCase("FEMALE") || gen.equalsIgnoreCase("NỮ")) female += count;
            else other += count;
        }

        // Age logic
        List<LocalDate> dobs = userRepository.getAllUserBirthDates();
        long genZ = 0, millennial = 0, genX = 0;
        LocalDate now = LocalDate.now();
        for (LocalDate dob : dobs) {
            int age = Period.between(dob, now).getYears();
            if (age < 25) genZ++;
            else if (age <= 40) millennial++;
            else genX++;
        }

        // Registration Trend (Last 30 days)
        List<Object[]> trendRaw = userRepository.getUserRegistrationTrend(thirtyDaysAgo, LocalDateTime.now());
        List<String> trendLabels = new ArrayList<>();
        List<Long> trendData = new ArrayList<>();
        for (Object[] row : trendRaw) {
            trendLabels.add(row[0].toString());
            trendData.add(((Number) row[1]).longValue());
        }

        // RFM & Sales
        BigDecimal aov = orderRepository.calculateAverageOrderValue();
        
        List<Object[]> freqData = orderRepository.countUserPurchaseFrequencies();
        long singlePurchase = 0, repeatPurchase = 0;
        for (Object[] row : freqData) {
            long freq = ((Number) row[1]).longValue();
            if (freq == 1) singlePurchase++;
            else if (freq > 1) repeatPurchase++;
        }

        long usersWithPurchase = singlePurchase + repeatPurchase;
        BigDecimal sumGmv = orderRepository.sumNetRevenueInPeriod(LocalDateTime.now().minusYears(10), LocalDateTime.now()); // All time
        BigDecimal avgLtv = usersWithPurchase > 0 ? sumGmv.divide(BigDecimal.valueOf(usersWithPurchase), 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // RFM Bubble Chart
        List<Object[]> rfmRaw = orderRepository.getRFMData();
        List<CustomerAnalyticsDTO.RfmBubble> bubbles = new ArrayList<>();
        for (Object[] r : rfmRaw) {
            String uname = r[0].toString();
            long f = ((Number) r[1]).longValue();
            BigDecimal m = new BigDecimal(r[2].toString());
            LocalDateTime lastPurchase = (LocalDateTime) r[3];
            int recency = (int) ChronoUnit.DAYS.between(lastPurchase, LocalDateTime.now());
            bubbles.add(new CustomerAnalyticsDTO.RfmBubble(uname, f, m, recency));
        }

        // Loyalty
        long totalPointsLiability = loyaltyAccountRepository.sumExtantPoints();
        // Fallback for missing point transaction query: just dummy if not present or let's omit Earn/Burn
        
        long bronze = loyaltyAccountRepository.countUsersInPointRange(0, 1000);
        long silver = loyaltyAccountRepository.countUsersInPointRange(1000, 5000);
        long gold = loyaltyAccountRepository.countUsersInPointRange(5000, 20000);
        long diamond = loyaltyAccountRepository.countUsersBeyondPoints(20000);

        long savedVouchers = userWalletRepository.count();

        // Geographical Stats
        List<String> provinces = addressRepository.getAllProvinces();
        Map<String, Long> provinceCounts = provinces.stream()
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        
        List<CustomerAnalyticsDTO.LocationStat> topLocs = provinceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new CustomerAnalyticsDTO.LocationStat(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Top Vips
        List<LoyaltyAccount> tops = loyaltyAccountRepository.findTopAccounts(PageRequest.of(0, 5));
        List<CustomerAnalyticsDTO.UserMetric> vipList = tops.stream().map(la -> 
            CustomerAnalyticsDTO.UserMetric.builder()
                .userId(la.getUser().getId())
                .username(la.getUser().getUsername())
                .maskedPhone(maskStr(la.getUser().getPhone()))
                .maskedEmail(maskEmail(la.getUser().getEmail()))
                .tierOrRole(MemberTier.fromLifetimePoints(la.getLifetimePoints()).getLabel())
                .metricName("Lifetime Points")
                .metricValue(String.valueOf(la.getLifetimePoints()))
                .build()
        ).toList();

        // High Return Rate
        List<Object[]> badUsersRaw = orderRepository.getHighReturnRateUsers(PageRequest.of(0, 5));
        List<CustomerAnalyticsDTO.UserMetric> badUsers = new ArrayList<>();
        for (Object[] row : badUsersRaw) {
            badUsers.add(CustomerAnalyticsDTO.UserMetric.builder()
                .username(row[0].toString())
                .metricName("Returns/Cancels")
                .metricValue(row[1].toString() + " Orders")
                .maskedPhone("***")
                .tierOrRole("Warning")
                .build());
        }

        return CustomerAnalyticsDTO.builder()
            .totalUsers(totalUsers)
            .activeUsers(totalUsers - bannedUsers) // approx
            .newUsersThisMonth(newUsers)
            .bannedUsers(bannedUsers)
            .growthLabels(trendLabels)
            .growthData(trendData)
            .maleCount(male)
            .femaleCount(female)
            .otherGenderCount(other)
            .genZCount(genZ)
            .millennialCount(millennial)
            .genXCount(genX)
            .topLocations(topLocs)
            .averageOrderValue(aov)
            .averageLifetimeValue(avgLtv)
            .singlePurchaseUsers(singlePurchase)
            .repeatPurchaseUsers(repeatPurchase)
            .rfmBubbles(bubbles)
            .totalPointsLiability(totalPointsLiability)
            .bronzeCount(bronze)
            .silverCount(silver)
            .goldCount(gold)
            .diamondCount(diamond)
            .totalWalletVouchersSaved(savedVouchers)
            .topSpendersVIPs(vipList)
            .highReturnRateUsers(badUsers)
            .build();
    }

    private String maskStr(String str) {
        if (str == null || str.length() < 4) return str;
        return str.substring(0, 3) + "***" + str.substring(str.length() - 3);
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) return email;
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }
}
