package com.codegym.smartphonemanagement.service.profile;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.RecentlyViewed;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.RecentlyViewedRepository;
import com.codegym.smartphonemanagement.repository.user.UserRepository;
import com.codegym.smartphonemanagement.repository.user.ProductRepositoryUser;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements IRecentlyViewedService {

    private final RecentlyViewedRepository recentlyViewedRepository;
    private final UserRepository userRepository;
    private final ProductRepositoryUser productRepository;

    private static final int MAX_HISTORY = 20;

    @Override
    @Transactional
    public void trackView(Long userId, Long productId) {
        // 1. Tìm bản ghi đã tồn tại
        Optional<RecentlyViewed> existing =
                recentlyViewedRepository.findByUserIdAndProductId(userId, productId);

        if (existing.isPresent()) {
            // Upsert: Cập nhật timestamp để đẩy sản phẩm lên đầu
            existing.get().setViewedAt(LocalDateTime.now());
            recentlyViewedRepository.save(existing.get());
        } else {
            // 2. Kiểm tra limit trước khi thêm mới
            long count = recentlyViewedRepository.countByUserId(userId);
            if (count >= MAX_HISTORY) {
                recentlyViewedRepository.findFirstByUserIdOrderByViewedAtAsc(userId)
                        .ifPresent(recentlyViewedRepository::delete);
            }

            // 3. Tạo bản ghi mới
            User user = userRepository.findById(userId).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (user == null || product == null) return;

            RecentlyViewed rv = RecentlyViewed.builder()
                    .user(user)
                    .product(product)
                    .viewedAt(LocalDateTime.now())
                    .build();
            recentlyViewedRepository.save(rv);
        }
    }

    @Override
    public List<ProductResponseDTO> getRecentProducts(Long userId, int limit) {
        return recentlyViewedRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(rv -> mapToDTO(rv.getProduct()))
                .collect(Collectors.toList());
    }

    // ===== MAPPER nội bộ (Product → ProductResponseDTO) =====
    private ProductResponseDTO mapToDTO(Product p) {
        return ProductResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .brand(p.getBrand())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .sold(p.getSold())
                .imageUrl(p.getImageUrl())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .active(p.getActive())
                .averageRating(p.getAverageRating())
                .totalReviews(p.getTotalReviews())
                .build();
    }
}
