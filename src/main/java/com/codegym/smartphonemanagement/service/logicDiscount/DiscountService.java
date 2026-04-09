package com.codegym.smartphonemanagement.service.logicDiscount;

import com.codegym.smartphonemanagement.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DiscountService {
    public BigDecimal applyDiscount(Product product) {
        BigDecimal price = product.getPrice();
        BigDecimal discountPrice = price;

        // Ưu tiên 1: Hàng xa xỉ ≥ 30 triệu → giảm 15%
        if (price.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) {
            discountPrice = price.multiply(BigDecimal.valueOf(0.85));
        }
        // Ưu tiên 2: Theo thương hiệu
        else if ("Apple".equalsIgnoreCase(product.getBrand())) {
            discountPrice = price.multiply(BigDecimal.valueOf(0.85));
        } else if ("Samsung".equalsIgnoreCase(product.getBrand())) {
            discountPrice = price.multiply(BigDecimal.valueOf(0.88));
        } else {
            discountPrice = price.multiply(BigDecimal.valueOf(0.90));
        }
        // Ưu tiên 3: Giá rẻ < 10 triệu → trừ thẳng 500k
        if (price.compareTo(BigDecimal.valueOf(10_000_000)) < 0) {
            discountPrice = price.subtract(BigDecimal.valueOf(500_000));
        }

        return discountPrice;
    }

    public String getDiscountLabel(Product product) {
        BigDecimal price = product.getPrice();

        if (price.compareTo(BigDecimal.valueOf(10_000_000)) < 0) return "500.000 ₫";
        if (price.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) return "15%";
        if ("Apple".equalsIgnoreCase(product.getBrand())) return "15%";
        if ("Samsung".equalsIgnoreCase(product.getBrand())) return "12%";
        return "10%";
    }
}
