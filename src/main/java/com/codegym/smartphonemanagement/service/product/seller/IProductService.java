package com.codegym.smartphonemanagement.service.product.seller;

import com.codegym.smartphonemanagement.model.Product;

public interface IProductService {

    ProductResponseDTO create(ProductRequestDTO request);

    Product updateProduct(Long id, Product product);

    void deleteProduct(Long id);
}