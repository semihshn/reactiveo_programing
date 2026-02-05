package com.reactive.crud.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactive.crud.dto.ExternalUserDto;
import com.reactive.crud.dto.ProductRequest;
import com.reactive.crud.dto.ProductResponse;
import com.reactive.crud.file.FileService;
import com.reactive.crud.http.HttpClientService;
import com.reactive.crud.service.ProductService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Logger LOG = Logger.getLogger(ProductResource.class);

    @Inject
    ProductService productService;

    @Inject
    HttpClientService httpClientService;

    @Inject
    FileService fileService;

    @Inject
    ObjectMapper objectMapper;

    @GET
    public Multi<ProductResponse> getAllProducts() {
        LOG.debug("REST request to get all products");
        return productService.getAllProducts();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getProductById(@PathParam("id") Long id) {
        LOG.debugf("REST request to get product with id: %d", id);
        return productService.getProductById(id)
                .onItem().ifNotNull().transform(product -> Response.ok(product).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Uni<Response> createProduct(@Valid ProductRequest request) {
        LOG.debugf("REST request to create product: %s", request.name());
        return productService.createProduct(request)
                .onItem().transform(product ->
                        Response.status(Response.Status.CREATED).entity(product).build()
                );
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> updateProduct(@PathParam("id") Long id, @Valid ProductRequest request) {
        LOG.debugf("REST request to update product with id: %d", id);
        return productService.updateProduct(id, request)
                .onItem().ifNotNull().transform(product -> Response.ok(product).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteProduct(@PathParam("id") Long id) {
        LOG.debugf("REST request to delete product with id: %d", id);
        return productService.deleteProduct(id)
                .onItem().transform(deleted -> {
                    if (deleted) {
                        return Response.noContent().build();
                    } else {
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                });
    }

    // New endpoints demonstrating all reactive features

    @GET
    @Path("/export")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> exportProducts() {
        LOG.debug("REST request to export products to file");
        return productService.getAllProducts()
                .collect().asList()
                .chain(products -> {
                    try {
                        String json = objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(products);
                        return fileService.exportProductsToFile(json);
                    } catch (Exception e) {
                        return Uni.createFrom().failure(e);
                    }
                })
                .onItem().transform(filePath ->
                        Response.ok("Products exported to: " + filePath).build()
                )
                .onFailure().recoverWithItem(failure ->
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("Export failed: " + failure.getMessage())
                                .build()
                );
    }

    @GET
    @Path("/external/users")
    public Uni<List<ExternalUserDto>> getExternalUsers() {
        LOG.debug("REST request to fetch external users via HTTP client");
        return httpClientService.fetchAllUsers();
    }

    @GET
    @Path("/external/users/{userId}")
    public Uni<Response> getExternalUserById(@PathParam("userId") Long userId) {
        LOG.debugf("REST request to fetch external user %d via HTTP client", userId);
        return httpClientService.fetchUserById(userId)
                .onItem().transform(user -> Response.ok(user).build())
                .onFailure().recoverWithItem(failure ->
                        Response.status(Response.Status.NOT_FOUND)
                                .entity("User not found")
                                .build()
                );
    }
}
