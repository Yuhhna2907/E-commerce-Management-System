package com.codegym.smartphonemanagement.service.logicDiscount;

import com.codegym.smartphonemanagement.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DiscountService {
    
    // FIX #11 & #12: Sửa logic giảm giá để không mâu thuẫn và không cho giá âm
    public BigDecimal applyDiscount(Product product) {
        BigDecimal price = product.getPrice();
        BigDecimal discountPrice;

        // Case 1: Giá rẻ < 10 triệu → trừ thẳng 500k (nhưng không cho âm)
        if (price.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
            discountPrice = price.subtract(BigDecimal.valueOf(500_000));
            // Đảm bảo không âm, tối thiểu là 10% của giá gốc
            BigDecimal minPrice = price.multiply(BigDecimal.valueOf(0.10));
            discountPrice = discountPrice.max(minPrice);
        }
        // Case 2: Hàng xa xỉ ≥ 30 triệu → giảm 15%
        else if (price.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) {
            discountPrice = price.multiply(BigDecimal.valueOf(0.85));
        }
        // Case 3: Theo thương hiệu (10M - 30M)
        else if ("Apple".equalsIgnoreCase(product.getBrand())) {
            discountPrice = price.multiply(BigDecimal.valueOf(0.85));
        } else if ("Samsung".equalsIgnoreCase(product.getBrand())) {
            discountPrice = price.multiply(BigDecimal.valueOf(0.88));
        } else {
            discountPrice = price.multiply(BigDecimal.valueOf(0.90));
        }

        // Đảm bảo không âm (safety check)
        return discountPrice.max(BigDecimal.ZERO);
    }

    public String getDiscountLabel(Product product) {
        BigDecimal price = product.getPrice();

        if (price.compareTo(BigDecimal.valueOf(10_000_000)) < 0) return "500.000 ₫";
        if (price.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) return "15%";
        if ("Apple".equalsIgnoreCase(product.getBrand())) return "15%";
        if ("Samsung".equalsIgnoreCase(product.getBrand())) return "12%";
        return "10%";
    }

    public BigDecimal applyDiscountToVariant(Product product, BigDecimal variantPrice) {
        if (variantPrice == null) return BigDecimal.ZERO;

        BigDecimal discountPrice;

        // Áp dụng cùng logic như applyDiscount
        if (product.getPrice().compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
            discountPrice = variantPrice.subtract(BigDecimal.valueOf(500_000));
            // Đảm bảo không âm, tối thiểu là 10% của giá gốc
            BigDecimal minPrice = variantPrice.multiply(BigDecimal.valueOf(0.10));
            discountPrice = discountPrice.max(minPrice);
        } else if (product.getPrice().compareTo(BigDecimal.valueOf(30_000_000)) >= 0) {
            discountPrice = variantPrice.multiply(BigDecimal.valueOf(0.85));
        } else if ("Apple".equalsIgnoreCase(product.getBrand())) {
            discountPrice = variantPrice.multiply(BigDecimal.valueOf(0.85));
        } else if ("Samsung".equalsIgnoreCase(product.getBrand())) {
            discountPrice = variantPrice.multiply(BigDecimal.valueOf(0.88));
        } else {
            discountPrice = variantPrice.multiply(BigDecimal.valueOf(0.90));
        }

        return discountPrice.max(BigDecimal.ZERO);
    }
}
