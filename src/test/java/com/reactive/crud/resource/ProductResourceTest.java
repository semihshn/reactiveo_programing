package com.reactive.crud.resource;

import com.reactive.crud.dto.ProductRequest;
import com.reactive.crud.dto.ProductResponse;
import com.reactive.crud.service.ProductService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProductResourceTest {

    @InjectMock
    ProductService productService;

    private ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        Mockito.reset(productService);

        sampleResponse = new ProductResponse(
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
        ProductResponse response2 = new ProductResponse(
                2L,
                "Product 2",
                "Description 2",
                new BigDecimal("49.99"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.getAllProducts())
                .thenReturn(Multi.createFrom().items(sampleResponse, response2));

        // When & Then
        given()
                .when().get("/api/products")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("[0].id", is(1))
                .body("[0].name", is("Test Product"))
                .body("[0].price", is(99.99f))
                .body("[1].id", is(2))
                .body("[1].name", is("Product 2"));
    }

    @Test
    void shouldGetProductById() {
        // Given
        when(productService.getProductById(1L))
                .thenReturn(Uni.createFrom().item(sampleResponse));

        // When & Then
        given()
                .when().get("/api/products/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", is(1))
                .body("name", is("Test Product"))
                .body("description", is("Test Description"))
                .body("price", is(99.99f));
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        // Given
        when(productService.getProductById(999L))
                .thenReturn(Uni.createFrom().nullItem());

        // When & Then
        given()
                .when().get("/api/products/999")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldCreateProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "New Product",
                "New Description",
                new BigDecimal("149.99")
        );

        ProductResponse createdResponse = new ProductResponse(
                3L,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(Uni.createFrom().item(createdResponse));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/products")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", is(3))
                .body("name", is("New Product"))
                .body("price", is(149.99f));
    }

    @Test
    void shouldReturn400WhenCreatingProductWithInvalidData() {
        // Given - invalid request (missing name)
        ProductRequest invalidRequest = new ProductRequest(
                "",
                "Description",
                new BigDecimal("100.00")
        );

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when().post("/api/products")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldUpdateProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("199.99")
        );

        ProductResponse updatedResponse = new ProductResponse(
                1L,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                sampleResponse.getCreatedAt(),
                LocalDateTime.now()
        );

        when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                .thenReturn(Uni.createFrom().item(updatedResponse));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().put("/api/products/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", is(1))
                .body("name", is("Updated Product"))
                .body("price", is(199.99f));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistingProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "Updated Product",
                "Updated Description",
                new BigDecimal("199.99")
        );

        when(productService.updateProduct(eq(999L), any(ProductRequest.class)))
                .thenReturn(Uni.createFrom().nullItem());

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().put("/api/products/999")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldDeleteProduct() {
        // Given
        when(productService.deleteProduct(1L))
                .thenReturn(Uni.createFrom().item(true));

        // When & Then
        given()
                .when().delete("/api/products/1")
                .then()
                .statusCode(204);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistingProduct() {
        // Given
        when(productService.deleteProduct(999L))
                .thenReturn(Uni.createFrom().item(false));

        // When & Then
        given()
                .when().delete("/api/products/999")
                .then()
                .statusCode(404);
    }
}
