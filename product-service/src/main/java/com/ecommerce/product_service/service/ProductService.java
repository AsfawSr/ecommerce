package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductDTO;

import java.util.List;

public interface ProductService {

    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO getProductById(Long id);
    ProductDTO getProductBySku(String sku);
    List<ProductDTO> getAllProducts();
    List<ProductDTO> getProductsByCategory(String category);
    List<ProductDTO> searchProducts(String keyword);
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    ProductDTO updateStock(Long id, Integer quantity);
    void deleteProduct(Long id);
    boolean productExists(Long id);
    long countProducts();
}