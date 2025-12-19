package com.ecommerce.product_service.service.impl;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.exception.InsufficientStockException;
import com.ecommerce.product_service.exception.ProductException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.service.ProductService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductRequest productRequest) {
        log.info("Creating product: {}", productRequest.getName());

        if (productRepository.existsBySku(productRequest.getSku())) {
            throw new ProductException("Product with SKU " + productRequest.getSku() + " already exists");
        }

        Product product = Product.builder()
                .sku(productRequest.getSku())
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .stockQuantity(productRequest.getStockQuantity())
                .category(productRequest.getCategory())
                .brand(productRequest.getBrand())
                .imageUrl(productRequest.getImageUrl())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());

        return convertToDTO(savedProduct);
    }

    @Override
    @Transactional
    public ProductDTO getProductById(Long id) {
        log.info("Getting product by ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO getProductBySku(String sku) {
        log.info("Getting product by SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductException("Product not found with SKU: " + sku));
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public List<ProductDTO> getAllProducts() {
        log.info("Getting all products");
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info("Getting products by category: {}", category);
        return productRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ProductDTO> searchProducts(String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        return productRepository.searchProducts(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, ProductRequest productRequest) {
        log.info("Updating product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));

        if (!product.getSku().equals(productRequest.getSku()) &&
                productRepository.existsBySku(productRequest.getSku())) {
            throw new ProductException("SKU " + productRequest.getSku() + " already exists");
        }

        product.setSku(productRequest.getSku());
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStockQuantity(productRequest.getStockQuantity());
        product.setCategory(productRequest.getCategory());
        product.setBrand(productRequest.getBrand());
        product.setImageUrl(productRequest.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: {}", id);

        return convertToDTO(updatedProduct);
    }

    @Override
    @Transactional
    public ProductDTO updateStock(Long id, Integer quantity) {
        log.info("Updating stock for product ID: {} by {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));

        if (quantity < 0 && Math.abs(quantity) > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    product.getName(),
                    Math.abs(quantity),
                    product.getStockQuantity()
            );
        }

        product.addStock(quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Stock updated. New quantity: {}", updatedProduct.getStockQuantity());

        return convertToDTO(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Product deactivated: {}", id);
    }

    @Override
    @Transactional
    public boolean productExists(Long id) {
        return productRepository.existsById(id);
    }

    @Override
    @Transactional
    public long countProducts() {
        return productRepository.count();
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "productService", fallbackMethod = "reserveInventoryFallback")
    public ProductDTO reserveInventory(Long productId, Integer quantity) {
        log.info("Reserving {} units of product ID: {}", quantity, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + productId));

        // Reserve stock (this will throw InsufficientStockException if not enough)
        product.reserveStock(quantity);

        Product updatedProduct = productRepository.save(product);
        log.info("Inventory reserved. Remaining stock: {}", updatedProduct.getStockQuantity());

        return convertToDTO(updatedProduct);
    }

    public ProductDTO reserveInventoryFallback(Long productId, Integer quantity, Exception e) {
        log.error("Circuit breaker fallback for reserveInventory: {}", e.getMessage());
        throw new ProductException("Cannot reserve inventory at the moment. Please try again later.");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "productService", fallbackMethod = "releaseInventoryFallback")
    public ProductDTO releaseInventory(Long productId, Integer quantity) {
        log.info("Releasing {} units of product ID: {}", quantity, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + productId));

        // Release stock back to inventory
        product.releaseStock(quantity);

        Product updatedProduct = productRepository.save(product);
        log.info("Inventory released. Current stock: {}", updatedProduct.getStockQuantity());

        return convertToDTO(updatedProduct);
    }

    public ProductDTO releaseInventoryFallback(Long productId, Integer quantity, Exception e) {
        log.error("Circuit breaker fallback for releaseInventory: {}", e.getMessage());
        throw new ProductException("Cannot release inventory at the moment. Please try again later.");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "productService", fallbackMethod = "checkStockFallback")
    public Integer checkStock(Long productId) {
        log.info("Checking stock for product ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + productId));

        return product.getStockQuantity();
    }

    public Integer checkStockFallback(Long productId, Exception e) {
        log.error("Circuit breaker fallback for checkStock: {}", e.getMessage());
        return 0;
    }

    @Override
    @Transactional
    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        log.info("Getting products with stock less than {}", threshold);
        return productRepository.findByStockQuantityGreaterThan(threshold).stream()
                .filter(product -> product.getStockQuantity() < threshold)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalProducts = productRepository.count();
        long activeProducts = productRepository.findByActiveTrue().size();

        // Calculate low stock products
        List<Product> allProducts = productRepository.findAll();
        long lowStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() < 10)
                .count();

        // Group by category
        Map<String, Long> productsByCategory = allProducts.stream()
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        Collectors.counting()
                ));

        stats.put("totalProducts", totalProducts);
        stats.put("activeProducts", activeProducts);
        stats.put("lowStockProducts", lowStockProducts);
        stats.put("productsByCategory", productsByCategory);

        return stats;
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}