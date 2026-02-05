package com.reactive.crud.repository;

import com.reactive.crud.entity.Product;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ProductRepositoryTest {

    @Inject
    ProductRepository productRepository;

    @Test
    void shouldFindAllProducts() {
        // When
        var subscriber = productRepository.findAll()
                .collect().asList()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var products = subscriber.awaitItem().getItem();
        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(p -> p.getId() != null);
    }

    @Test
    void shouldFindProductById() {
        // Given - using sample data from init.sql
        Long existingId = 1L;

        // When
        var subscriber = productRepository.findById(existingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var product = subscriber.awaitItem().getItem();
        assertThat(product).isNotNull();
        assertThat(product.getId()).isEqualTo(existingId);
        assertThat(product.getName()).isNotBlank();
    }

    @Test
    void shouldReturnNullWhenProductNotFound() {
        // Given
        Long nonExistingId = 99999L;

        // When
        var subscriber = productRepository.findById(nonExistingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var product = subscriber.awaitItem().getItem();
        assertThat(product).isNull();
    }

    @Test
    void shouldCreateProduct() {
        // Given
        Product newProduct = new Product();
        newProduct.setName("Test Product");
        newProduct.setDescription("Test Description");
        newProduct.setPrice(new BigDecimal("99.99"));

        // When
        var subscriber = productRepository.create(newProduct)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var savedProduct = subscriber.awaitItem().getItem();
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateProduct() {
        // Given - create a product first
        Product newProduct = new Product();
        newProduct.setName("Original Name");
        newProduct.setDescription("Original Description");
        newProduct.setPrice(new BigDecimal("50.00"));

        var createdProduct = productRepository.create(newProduct)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        // When - update the product
        createdProduct.setName("Updated Name");
        createdProduct.setPrice(new BigDecimal("75.00"));

        var subscriber = productRepository.update(createdProduct)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var updatedProduct = subscriber.awaitItem().getItem();
        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.getId()).isEqualTo(createdProduct.getId());
        assertThat(updatedProduct.getName()).isEqualTo("Updated Name");
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(updatedProduct.getUpdatedAt()).isAfter(updatedProduct.getCreatedAt());
    }

    @Test
    void shouldDeleteProduct() {
        // Given - create a product first
        Product newProduct = new Product();
        newProduct.setName("To Be Deleted");
        newProduct.setDescription("Will be deleted");
        newProduct.setPrice(new BigDecimal("10.00"));

        var createdProduct = productRepository.create(newProduct)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().getItem();

        Long productId = createdProduct.getId();

        // When - delete the product
        var deleteSubscriber = productRepository.delete(productId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var deleted = deleteSubscriber.awaitItem().getItem();
        assertThat(deleted).isTrue();

        // Verify product is gone
        var findSubscriber = productRepository.findById(productId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        var product = findSubscriber.awaitItem().getItem();
        assertThat(product).isNull();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistingProduct() {
        // Given
        Long nonExistingId = 99999L;

        // When
        var subscriber = productRepository.delete(nonExistingId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Then
        var deleted = subscriber.awaitItem().getItem();
        assertThat(deleted).isFalse();
    }
}
