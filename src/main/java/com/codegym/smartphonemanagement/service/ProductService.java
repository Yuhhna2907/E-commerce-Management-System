package com.codegym.smartphonemanagement.service;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;

    // 🔹 CREATE
    @Override
    public Product createProduct(Product product) {

        if (product.getStock() != null && product.getStock() < 0) {
            throw new RuntimeException("Stock không được âm");
        }

        return productRepository.save(product);
    }

    // 🔹 UPDATE
    @Override
    public Product updateProduct(Long id, Product newProduct) {

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Nếu có active thì nên check
        if (existing.getActive() != null && !existing.getActive()) {
            throw new RuntimeException("Sản phẩm đã bị xóa");
        }

        existing.setName(newProduct.getName());
        existing.setBrand(newProduct.getBrand());
        existing.setPrice(newProduct.getPrice());
        existing.setStock(newProduct.getStock());
        existing.setStorage(newProduct.getStorage());
        existing.setColor(newProduct.getColor());
        existing.setDescription(newProduct.getDescription());

        return productRepository.save(existing);
    }

    // 🔹 DELETE (SOFT DELETE)
    @Override
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Nếu đã có active thì dùng soft delete
        if (product.getActive() != null) {
            product.setActive(false);
            productRepository.save(product);
        } else {
            // fallback nếu chưa có field active
            productRepository.delete(product);
        }
    }
}