package com.codegym.smartphonemanagement.service.product.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.Review;
import com.codegym.smartphonemanagement.model.User;
import com.codegym.smartphonemanagement.repository.user.*;
import com.codegym.smartphonemanagement.service.logicDiscount.DiscountService;
import com.codegym.smartphonemanagement.service.product.DTO.ComparisonItemDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductVariantResponseDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ReviewResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProductService implements IUserProductService {

    private final ProductRepositoryUser productRepository;
    private final DiscountService discountService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Page<ProductResponseDTO> searchProducts(
            String keyword,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortDirection
    ) {

        // Normalize dữ liệu
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        if (brand != null && brand.trim().isEmpty()) {
            brand = null;
        }

        Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by("price").descending()
                : Sort.by("price").ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.searchForUser(
                keyword,
                brand,
                minPrice,
                maxPrice,
                pageable
        );

        return productPage.map(this::convertToDTO);
    }

    public ReviewResponseDTO reviewProduct(Long userId, ReviewRequestDTO request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasBought = orderItemRepository
                .existsCompletedPurchase(userId, request.getProductId());

        if (!hasBought) {
            throw new RuntimeException("Bạn chưa mua sản phẩm này");
        }

        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }

        // 5️⃣ Tạo review
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());

        // FIX #6: Wrap trong try-catch để handle duplicate constraint violation
        try {
            reviewRepository.save(review);
            reviewRepository.flush(); // FIX #7: Force commit để query thấy review mới
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Double avg = reviewRepository.getAverageRating(product.getId());

        product.setAverageRating(
                avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0
        );

        product.setTotalReviews(
                product.getTotalReviews() == null ? 1 : product.getTotalReviews() + 1
        );

        productRepository.save(product);

        return ReviewResponseDTO.builder()
                .id(review.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .productId(product.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private ProductResponseDTO convertToDTO(Product product) {

        BigDecimal discountPrice = discountService.applyDiscount(product);
        String discountLabel = discountService.getDiscountLabel(product);

        Integer sold = product.getSold() != null ? product.getSold() : 0;
        Integer stock = product.getStock() != null ? product.getStock() : 0;
        Double averageRating = product.getAverageRating() != null ? product.getAverageRating() : 0.0;
        Integer totalReviews = product.getTotalReviews() != null ? product.getTotalReviews() : 0;

        List<ProductVariantResponseDTO> variantDTOs = product.getVariants() == null
                ? List.of()
                : product.getVariants().stream()
                .map(variant -> {
                    BigDecimal variantDiscountPrice = discountService.applyDiscountToVariant(product, variant.getSalePrice());

                    return ProductVariantResponseDTO.builder()
                            .variantId(variant.getVariantId())
                            .productId(product.getId())
                            .sku(variant.getSku())
                            .variantName(variant.getVariantName())
                            .color(variant.getColor())
                            .storage(variant.getStorage())
                            .ram(variant.getRam())
                            .costPrice(variant.getCostPrice())
                            .salePrice(variant.getSalePrice()) // Giá gốc của variant
                            .discountPrice(variantDiscountPrice) // <--- THÊM TRƯỜNG NÀY (Cần check DTO có chưa)
                            .stockQuantity(variant.getStockQuantity())
                            .active(variant.getIsActive())
                            .build();
                })
                .toList();

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(discountPrice)
                .discountLabel(discountLabel)
                .sold(sold)
                .stock(stock)
                .totalQuantity(stock + sold)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .active(product.getActive())
                .variants(variantDTOs)
                .build();
    }


    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return convertToDTO(product);
    }

    public List<ReviewResponseDTO> getReviewsByProductId(Long productId) {

        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(review -> ReviewResponseDTO.builder()
                        .id(review.getId())
                        .userId(review.getUser().getId())
                        .username(review.getUser().getUsername())
                        .productId(review.getProduct().getId())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build()
                )
                .toList();
    }

    @Override
    public List<ComparisonItemDTO> compareProducts(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        // Tìm kỷ lục (Max / Min)
        Integer maxAntutu = products.stream()
                .filter(p -> p.getSpecification() != null && p.getSpecification().getAntutuScore() != null)
                .map(p -> p.getSpecification().getAntutuScore())
                .max(Integer::compareTo).orElse(0);

        Double maxScreenSize = products.stream()
                .filter(p -> p.getSpecification() != null && p.getSpecification().getScreenSize() != null)
                .map(p -> p.getSpecification().getScreenSize())
                .max(Double::compareTo).orElse(0.0);

        Integer maxBattery = products.stream()
                .filter(p -> p.getSpecification() != null && p.getSpecification().getBatteryCapacity() != null)
                .map(p -> p.getSpecification().getBatteryCapacity())
                .max(Integer::compareTo).orElse(0);

        Integer maxCharging = products.stream()
                .filter(p -> p.getSpecification() != null && p.getSpecification().getChargingSpeed() != null)
                .map(p -> p.getSpecification().getChargingSpeed())
                .max(Integer::compareTo).orElse(0);

        Double minWeight = products.stream()
                .filter(p -> p.getSpecification() != null && p.getSpecification().getWeight() != null)
                .map(p -> p.getSpecification().getWeight())
                .min(Double::compareTo).orElse(Double.MAX_VALUE);

        return products.stream().map(p -> {
            ComparisonItemDTO dto = ComparisonItemDTO.builder()
                    .productId(p.getId())
                    .name(p.getName())
                    .imageUrl(p.getImageUrl())
                    .price(discountService.applyDiscount(p))
                    .build();

            if (p.getSpecification() != null) {
                com.codegym.smartphonemanagement.model.ProductSpecification spec = p.getSpecification();
                
                dto.setProcessor(spec.getProcessor());
                dto.setAntutuScore(spec.getAntutuScore());
                dto.setBestAntutu(spec.getAntutuScore() != null && spec.getAntutuScore().equals(maxAntutu) && maxAntutu > 0);

                dto.setScreenSize(spec.getScreenSize());
                dto.setScreenTech(spec.getScreenTech());
                dto.setBestScreenSize(spec.getScreenSize() != null && spec.getScreenSize().equals(maxScreenSize) && maxScreenSize > 0);

                dto.setCameraInfo(spec.getCameraInfo());

                dto.setBatteryCapacity(spec.getBatteryCapacity());
                dto.setBestBattery(spec.getBatteryCapacity() != null && spec.getBatteryCapacity().equals(maxBattery) && maxBattery > 0);

                dto.setChargingSpeed(spec.getChargingSpeed());
                dto.setBestCharging(spec.getChargingSpeed() != null && spec.getChargingSpeed().equals(maxCharging) && maxCharging > 0);

                dto.setOs(spec.getOs());
                
                dto.setWeight(spec.getWeight());
                dto.setLightestWeight(spec.getWeight() != null && spec.getWeight().equals(minWeight) && minWeight < Double.MAX_VALUE);
            }

            return dto;
        }).toList();
    }
}
