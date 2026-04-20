package com.codegym.smartphonemanagement.repository.seller;

import com.codegym.smartphonemanagement.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategorySellerRepository extends JpaRepository<Category, Long> {

    // Tìm các danh mục đang hoạt động
    List<Category> findAllByActiveTrue();

    // Kiểm tra tên trùng (dùng cho thêm/sửa)
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByName(String name);
    
    // Case-insensitive uniqueness checks
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    
    // Search methods with pagination
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Category> findAllByActive(Boolean active, Pageable pageable);
    Page<Category> findByNameContainingIgnoreCaseAndActive(String keyword, Boolean active, Pageable pageable);
}