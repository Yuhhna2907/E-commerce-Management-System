package com.codegym.smartphonemanagement.service.category;

import com.codegym.smartphonemanagement.model.Category;
import java.util.List;

public interface ICategoryService {
    List<Category> findAll();
    Category findById(Long id);
    void save(Category category);
    void delete(Long id);
    void toggleStatus(Long id);
}