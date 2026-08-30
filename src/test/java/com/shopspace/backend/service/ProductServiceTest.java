package com.shopspace.backend.service;

import com.shopspace.backend.dto.UpdateProductRequest;
import com.shopspace.backend.entity.Category;
import com.shopspace.backend.entity.Product;
import com.shopspace.backend.entity.User;
import com.shopspace.backend.repository.CategoryRepository;
import com.shopspace.backend.repository.ProductRepository;
import com.shopspace.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void updateProductUpdatesProductOwnedByAuthenticatedSeller() {
        User seller = seller(1L, "seller@shopspace.com");
        Product product = product(5L, seller);
        Category category = category(2L);
        UpdateProductRequest request = updateRequest();

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller@shopspace.com"))
                .thenReturn(Optional.of(seller));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);

        Product updated = productService.updateProduct(
                5L,
                request,
                new UsernamePasswordAuthenticationToken("seller@shopspace.com", null));

        assertEquals("Updated mouse", updated.getName());
        assertEquals(new BigDecimal("2299.99"), updated.getPrice());
        assertEquals(category, updated.getCategory());
        verify(productRepository).save(product);
    }

    @Test
    void updateProductRejectsAnotherSeller() {
        Product product = product(5L, seller(2L, "owner@shopspace.com"));
        User authenticatedSeller = seller(1L, "seller@shopspace.com");

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller@shopspace.com"))
                .thenReturn(Optional.of(authenticatedSeller));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(
                        5L,
                        updateRequest(),
                        new UsernamePasswordAuthenticationToken(
                                "seller@shopspace.com", null)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(productRepository, never()).save(product);
    }

    private Product product(Long id, User seller) {
        Product product = new Product();
        product.setId(id);
        product.setSeller(seller);
        return product;
    }

    private User seller(Long id, String email) {
        User seller = new User();
        seller.setId(id);
        seller.setEmail(email);
        return seller;
    }

    private Category category(Long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private UpdateProductRequest updateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated mouse");
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("2299.99"));
        request.setSku("GM-001");
        request.setImageUrl("https://example.com/mouse.jpg");
        request.setCategoryId(2L);
        return request;
    }
}
