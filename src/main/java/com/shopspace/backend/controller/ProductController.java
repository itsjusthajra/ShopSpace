package com.shopspace.backend.controller;

import com.shopspace.backend.dto.CreateProductRequest;
import com.shopspace.backend.dto.ProductResponse;
import com.shopspace.backend.dto.UpdateProductRequest;

import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;

import com.shopspace.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        return ResponseEntity.ok(
                productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id) {

        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ProductResponse>> getMyProducts(
            Authentication authentication) {

        return ResponseEntity.ok(productService.getProductsForSeller(authentication));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                productService.createProduct(request, authentication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                productService.convertToResponse(
                        productService.updateProduct(id, request, authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {

        productService.deleteProduct(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
