package com.codegym.smartphonemanagement.service.category;

import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.repository.seller.CategorySellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional // Đảm bảo mọi thao tác DB đều nằm trong Transaction
public class CategoryService implements ICategoryService {

    @Autowired
    private CategorySellerRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public void save(Category category) {
        // Xử lý logic id rỗng từ form gửi lên
        if (category.getId() != null && category.getId() <= 0) {
            category.setId(null);
        }
        categoryRepository.save(category);
    }

    @Override
    public void delete(Long id) {
        Category category = findById(id);
        if (category != null) {
            // FIX #10: Chỉ đếm sản phẩm active
            long activeProductCount = category.getProducts() != null 
                ? category.getProducts().stream()
                    .filter(p -> p.getActive() != null && p.getActive())
                    .count()
                : 0;
            
            if (activeProductCount > 0) {
                throw new RuntimeException("Danh mục đang có " + activeProductCount + " sản phẩm đang hoạt động, không thể xóa!");
            }
            categoryRepository.deleteById(id);
        }
    }

    @Override
    public void toggleStatus(Long id) {
        Category category = findById(id);
        if (category != null) {
            category.setActive(!category.getActive());
            categoryRepository.save(category);
        }
    }
}