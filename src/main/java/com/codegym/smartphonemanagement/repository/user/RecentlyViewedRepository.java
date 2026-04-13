package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.RecentlyViewed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
