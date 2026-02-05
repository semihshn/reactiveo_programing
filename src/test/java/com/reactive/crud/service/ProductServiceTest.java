package com.reactive.crud.service;

import com.reactive.crud.dto.ProductRequest;
import com.reactive.crud.dto.ProductResponse;
import com.reactive.crud.entity.Product;
import com.reactive.crud.repository.ProductRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductServiceTest {

    @InjectMock
    ProductRepository productRepository;

    @Inject
    ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        Mockito.reset(productRepository);

        sampleProduct = new Product(
                1L,
                "Test Product",
                "Test Description",
                new BigDecimal("99.99"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void shouldGetAllProducts() {
        // Given
        Product product2 = new Product(
                2L,
                "Product 2",
                "Description 2",
                new BigDecimal("49.99"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(productRepository.findAll())
                .thenReturn(Multi.createFrom().items(sampleProduct, product2));

        // When
        var subscriber = productService.getAllProducts()
                .collect().asList()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        List<ProductResponse> products = subscriber.awaitItem().getItem();
        assertThat(products).hasSize(2);
        assertThat(products.get(0).id()).isEqualTo(1L);
        assertThat(products.get(0).name()).isEqualTo("Test Product");
        assertThat(products.get(1).id()).isEqualTo(2L);
    }

    @Test
    void shouldGetProductById() {
        // Given
        when(productRepository.findById(1L))
                .thenReturn(Uni.createFrom().item(sampleProduct));

        // When
        var subscriber = productService.getProductById(1L)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        ProductResponse response = subscriber.awaitItem().getItem();
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test Product");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void shouldReturnNullWhenProductNotFound() {
        // Given
        when(productRepository.findById(999L))
                .thenReturn(Uni.createFrom().nullItem());

        // When
        var subscriber = productService.getProductById(999L)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        ProductResponse response = subscriber.awaitItem().getItem();
        assertThat(response).isNull();
    }

    @Test
    void shouldCreateProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "New Product",
                "New Description",
                new BigDecimal("149.99")
        );

        Product createdProduct = new Product(
                3L,
                request.name(),
                request.description(),
                request.price(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productRepository.create(any(Product.class)))
                .thenReturn(Uni.createFrom().item(createdProduct));

        // When
        var subscriber = productService.createProduct(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        ProductResponse response = subscriber.awaitItem().getItem();
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("New Product");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("149.99"));
    }

    @Test
    void shouldUpdateProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("199.99")
        );

        when(productRepository.findById(1L))
                .thenReturn(Uni.createFrom().item(sampleProduct));

        Product updatedProduct = new Product(
                1L,
                request.name(),
                request.description(),
                request.price(),
                sampleProduct.getCreatedAt(),
                LocalDateTime.now()
        );

        when(productRepository.update(any(Product.class)))
                .thenReturn(Uni.createFrom().item(updatedProduct));

        // When
        var subscriber = productService.updateProduct(1L, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        ProductResponse response = subscriber.awaitItem().getItem();
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Updated Product");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("199.99"));
    }

    @Test
    void shouldReturnNullWhenUpdatingNonExistingProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("199.99")
        );

        when(productRepository.findById(999L))
                .thenReturn(Uni.createFrom().nullItem());

        // When
        var subscriber = productService.updateProduct(999L, request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        ProductResponse response = subscriber.awaitItem().getItem();
        assertThat(response).isNull();
    }

    @Test
    void shouldDeleteProduct() {
        // Given
        when(productRepository.delete(1L))
                .thenReturn(Uni.createFrom().item(true));

        // When
        var subscriber = productService.deleteProduct(1L)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        Boolean deleted = subscriber.awaitItem().getItem();
        assertThat(deleted).isTrue();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistingProduct() {
        // Given
        when(productRepository.delete(999L))
                .thenReturn(Uni.createFrom().item(false));

        // When
        var subscriber = productService.deleteProduct(999L)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        Boolean deleted = subscriber.awaitItem().getItem();
        assertThat(deleted).isFalse();
    }
}
