package com.codegym.smartphonemanagement.repository.seller;

import com.codegym.smartphonemanagement.model.Category;
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
}