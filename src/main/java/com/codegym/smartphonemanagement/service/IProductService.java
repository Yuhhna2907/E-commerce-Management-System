package com.codegym.smartphonemanagement.service;

import com.codegym.smartphonemanagement.model.Product;

public interface IProductService {

    Product createProduct(Product product);

    Product updateProduct(Long id, Product product);

    void deleteProduct(Long id);
}