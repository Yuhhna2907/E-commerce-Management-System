package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.RecentlyViewed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, Long> {

    // Tìm bản ghi đã tồn tại (để upsert)
    Optional<RecentlyViewed> findByUserIdAndProductId(Long userId, Long productId);

    // Lấy danh sách xem gần nhất (có phân trang/giới hạn)
    List<RecentlyViewed> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    // Đếm số lượng bản ghi của user
    long countByUserId(Long userId);

    // Tìm bản ghi cũ nhất (để xóa thủ công - an toàn hơn subquery DELETE)
    Optional<RecentlyViewed> findFirstByUserIdOrderByViewedAtAsc(Long userId);
    // --- ANALYTICS QUERIES ---

    @Query("SELECT rv.product.id, COUNT(DISTINCT rv.user.id) " +
           "FROM RecentlyViewed rv WHERE rv.viewedAt >= :startDate AND rv.viewedAt <= :endDate " +
           "GROUP BY rv.product.id")
    List<Object[]> countUniqueViewsByProductInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT rv.product.category.id, COUNT(DISTINCT rv.user.id) " +
           "FROM RecentlyViewed rv WHERE rv.viewedAt >= :startDate AND rv.viewedAt <= :endDate " +
           "GROUP BY rv.product.category.id")
    List<Object[]> countUniqueViewsByCategoryInPeriod(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT rv.product.id, COUNT(rv) " +
           "FROM RecentlyViewed rv " +
           "WHERE rv.viewedAt >= :startDate AND rv.viewedAt <= :endDate " +
           "GROUP BY rv.product.id " +
           "ORDER BY COUNT(rv) DESC")
    List<Object[]> getHotTrendProducts(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate, Pageable pageable);
}
