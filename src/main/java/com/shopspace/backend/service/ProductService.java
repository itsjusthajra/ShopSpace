package com.shopspace.backend.service;

import com.shopspace.backend.dto.CategoryResponse;
import com.shopspace.backend.dto.ProductResponse;
import com.shopspace.backend.dto.SellerResponse;
import com.shopspace.backend.entity.Product;
import com.shopspace.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    private ProductResponse convertToResponse(Product product) {

        CategoryResponse category = new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription(),
                product.getCategory().isActive()
        );

        SellerResponse seller = new SellerResponse(
                product.getSeller().getId(),
                product.getSeller().getFirstName()
                        + " "
                        + product.getSeller().getLastName()
        );

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.getImageUrl(),
                product.isActive(),
                category,
                seller
        );
    }
}