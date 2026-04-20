package com.codegym.smartphonemanagement.model;

/**
 * Hạng thành viên dựa trên Lifetime Points.
 * Chỉ tăng, không bao giờ giảm.
 */
public enum MemberTier {

    BRONZE("Đồng",     "#cd7f32", "bi-award",        0,      1.0),
    SILVER("Bạc",      "#94a3b8", "bi-award-fill",   1_000,  1.05),
    GOLD  ("Vàng",     "#f59e0b", "bi-star-fill",    5_000,  1.10),
    DIAMOND("Kim Cương","#6366f1","bi-gem",           20_000, 1.20);

    private final String label;
    private final String color;
    private final String icon;
    /** Lifetime points tối thiểu để đạt hạng này */
    private final int minLifetimePoints;
    /** Hệ số nhân điểm (1.0 = bình thường, 1.1 = +10%) */
    private final double bonusMultiplier;

    MemberTier(String label, String color, String icon, int minLifetimePoints, double bonusMultiplier) {
        this.label = label;
        this.color = color;
        this.icon = icon;
        this.minLifetimePoints = minLifetimePoints;
        this.bonusMultiplier = bonusMultiplier;
    }

    public String getLabel()             { return label; }
    public String getColor()             { return color; }
    public String getIcon()              { return icon; }
    public int    getMinLifetimePoints() { return minLifetimePoints; }
    public double getBonusMultiplier()   { return bonusMultiplier; }

    /**
     * Xác định Tier dựa trên tổng lifetime points.
     * Dùng reverse enum iteration để lấy hạng cao nhất phù hợp.
     */
    public static MemberTier fromLifetimePoints(int lifetimePoints) {
        MemberTier[] tiers = MemberTier.values();
        for (int i = tiers.length - 1; i >= 0; i--) {
            if (lifetimePoints >= tiers[i].minLifetimePoints) {
                return tiers[i];
            }
        }
        return BRONZE;
    }

    /**
     * Điểm cần thêm để đạt hạng tiếp theo (null nếu đã là Kim Cương).
     */
    public static Integer pointsToNextTier(int lifetimePoints) {
        MemberTier current = fromLifetimePoints(lifetimePoints);
        MemberTier[] tiers = MemberTier.values();
        int currentOrdinal = current.ordinal();
        if (currentOrdinal >= tiers.length - 1) return null; // Kim Cương rồi
        return tiers[currentOrdinal + 1].minLifetimePoints - lifetimePoints;
    }

    /** Tiến độ % đến hạng tiếp theo (0-100) */
    public static int progressToNextTier(int lifetimePoints) {
        MemberTier current = fromLifetimePoints(lifetimePoints);
        MemberTier[] tiers = MemberTier.values();
        int idx = current.ordinal();
        if (idx >= tiers.length - 1) return 100;
        int from = tiers[idx].minLifetimePoints;
        int to   = tiers[idx + 1].minLifetimePoints;
        return (int) Math.min(100, (double)(lifetimePoints - from) / (to - from) * 100);
    }
}
