package com.codegym.smartphonemanagement.service.product.user;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.model.ProductSpecification;
import com.codegym.smartphonemanagement.model.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecificationUser {

    public static Specification<Product> filterProducts(
            String keyword,
            List<String> brands,
            List<String> rams,
            List<String> storages,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minScreen,
            Double maxScreen,
            Integer minBattery,
            Integer maxBattery,
            Double minWeight,
            Double maxWeight,
            List<String> osList,
            Boolean inStockOnly
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ hiện sản phẩm đang kích hoạt
            predicates.add(cb.isTrue(root.get("active")));

            // 2. Tìm kiếm theo từ khóa (tên)
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }

            // 3. Lọc theo nhiều hãng cùng lúc
            if (brands != null && !brands.isEmpty()) {
                predicates.add(root.get("brand").in(brands));
            }

            // 4. Lọc theo Giá (Dùng p.price hoặc join variant)
            // Ở đây tôi giả định dùng giá trung tâm của Product để lọc khoảng giá chung
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // 5. Lọc RAM & Storage (Join với ProductVariant)
            if ((rams != null && !rams.isEmpty()) || (storages != null && !storages.isEmpty()) || Boolean.TRUE.equals(inStockOnly)) {
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                query.distinct(true);

                if (rams != null && !rams.isEmpty()) {
                    predicates.add(variants.get("ram").in(rams));
                }
                if (storages != null && !storages.isEmpty()) {
                    predicates.add(variants.get("storage").in(storages));
                }
                if (Boolean.TRUE.equals(inStockOnly)) {
                    predicates.add(cb.greaterThan(variants.get("stockQuantity"), 0));
                }
            }

            // 6. Lọc Spec (Join với ProductSpecification)
            if (minScreen != null || maxScreen != null || minBattery != null || maxBattery != null || minWeight != null || maxWeight != null || (osList != null && !osList.isEmpty())) {
                Join<Product, ProductSpecification> specs = root.join("specification", JoinType.LEFT);
                
                if (minScreen != null) {
                    predicates.add(cb.greaterThanOrEqualTo(specs.get("screenSize"), minScreen));
                }
                if (maxScreen != null) {
                    predicates.add(cb.lessThanOrEqualTo(specs.get("screenSize"), maxScreen));
                }
                if (minBattery != null) {
                    predicates.add(cb.greaterThanOrEqualTo(specs.get("batteryCapacity"), minBattery));
                }
                if (maxBattery != null) {
                    predicates.add(cb.lessThanOrEqualTo(specs.get("batteryCapacity"), maxBattery));
                }
                if (minWeight != null) {
                    predicates.add(cb.greaterThanOrEqualTo(specs.get("weight"), minWeight));
                }
                if (maxWeight != null) {
                    predicates.add(cb.lessThanOrEqualTo(specs.get("weight"), maxWeight));
                }
                if (osList != null && !osList.isEmpty()) {
                    predicates.add(specs.get("os").in(osList));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
