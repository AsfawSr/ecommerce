package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.dto.ProductRequest;

import java.util.List;
import java.util.Map;

public interface ProductService {

    // Existing methods
    ProductDTO createProduct(ProductRequest productRequest);  // Changed from ProductDTO to ProductRequest
    ProductDTO getProductById(Long id);
    ProductDTO getProductBySku(String sku);
    List<ProductDTO> getAllProducts();
    List<ProductDTO> getProductsByCategory(String category);
    List<ProductDTO> searchProducts(String keyword);
    ProductDTO updateProduct(Long id, ProductRequest productRequest);  // Changed
    ProductDTO updateStock(Long id, Integer quantity);
    void deleteProduct(Long id);
    boolean productExists(Long id);
    long countProducts();

    ProductDTO reserveInventory(Long productId, Integer quantity);
    ProductDTO releaseInventory(Long productId, Integer quantity);
    Integer checkStock(Long productId);

    List<ProductDTO> getLowStockProducts(Integer threshold);
    Map<String, Object> getProductStatistics();
}