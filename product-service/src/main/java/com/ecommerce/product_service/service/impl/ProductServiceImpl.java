package com.ecommerce.product_service.service.impl;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.exception.ProductException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import com.ecommerce.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Creating product: {}", productDTO.getName());

        if (productRepository.existsBySku(productDTO.getSku())) {
            throw new ProductException("Product with SKU " + productDTO.getSku() + " already exists");
        }

        Product product = Product.builder()
                .sku(productDTO.getSku())
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .stockQuantity(productDTO.getStockQuantity())
                .category(productDTO.getCategory())
                .brand(productDTO.getBrand())
                .imageUrl(productDTO.getImageUrl())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());

        return convertToDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        log.info("Getting product by ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));
        return convertToDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductBySku(String sku) {
        log.info("Getting product by SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductException("Product not found with SKU: " + sku));
        return convertToDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.info("Getting all products");
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info("Getting products by category: {}", category);
        return productRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        return productRepository.searchProducts(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        log.info("Updating product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));

        if (!product.getSku().equals(productDTO.getSku()) &&
                productRepository.existsBySku(productDTO.getSku())) {
            throw new ProductException("SKU " + productDTO.getSku() + " already exists");
        }

        product.setSku(productDTO.getSku());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setCategory(productDTO.getCategory());
        product.setBrand(productDTO.getBrand());
        product.setImageUrl(productDTO.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: {}", id);

        return convertToDTO(updatedProduct);
    }

    @Override
    public ProductDTO updateStock(Long id, Integer quantity) {
        log.info("Updating stock for product ID: {} by {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));

        int newStock = product.getStockQuantity() + quantity;
        if (newStock < 0) {
            throw new ProductException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        product.setStockQuantity(newStock);
        Product updatedProduct = productRepository.save(product);
        log.info("Stock updated. New quantity: {}", newStock);

        return convertToDTO(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException("Product not found with ID: " + id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Product deactivated: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean productExists(Long id) {
        return productRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
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