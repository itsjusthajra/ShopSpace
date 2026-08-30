package com.shopspace.backend.service;

import com.shopspace.backend.dto.CategoryResponse;
import com.shopspace.backend.dto.CreateProductRequest;
import com.shopspace.backend.dto.ProductResponse;
import com.shopspace.backend.dto.SellerResponse;
import com.shopspace.backend.entity.Category;
import com.shopspace.backend.entity.Product;
import com.shopspace.backend.entity.User;
import com.shopspace.backend.repository.CategoryRepository;
import com.shopspace.backend.repository.ProductRepository;
import com.shopspace.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public Optional<ProductResponse> getProductById(Long id) {

        return productRepository.findById(id)
                .map(this::convertToResponse);
    }

    public ProductResponse createProduct(
            CreateProductRequest request,
            Authentication authentication) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        User seller = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setSeller(seller);
        product.setActive(true);

        Product savedProduct = productRepository.save(product);

        return convertToResponse(savedProduct);
    }

    private ProductResponse convertToResponse(Product product) {

        CategoryResponse category = new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription(),
                product.getCategory().isActive());

        SellerResponse seller = new SellerResponse(
                product.getSeller().getId(),
                product.getSeller().getFirstName()
                        + " "
                        + product.getSeller().getLastName());

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.getImageUrl(),
                product.isActive(),
                category,
                seller);
    }
}