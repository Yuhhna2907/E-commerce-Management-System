package com.codegym.smartphonemanagement.service.product.seller;

import com.codegym.smartphonemanagement.model.Product;
import com.codegym.smartphonemanagement.service.product.DTO.ProductRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;

public interface IProductService {

    ProductResponseDTO create(ProductRequestDTO request);

    Product updateProduct(Long id, Product product);

    void deleteProduct(Long id);
}