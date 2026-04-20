package com.codegym.smartphonemanagement.repository.user;

import com.codegym.smartphonemanagement.model.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
}
