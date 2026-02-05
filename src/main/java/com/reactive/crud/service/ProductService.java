package com.reactive.crud.service;

import com.reactive.crud.cache.ProductCacheService;
import com.reactive.crud.dto.ProductRequest;
import com.reactive.crud.dto.ProductResponse;
import com.reactive.crud.entity.Product;
import com.reactive.crud.mail.MailService;
import com.reactive.crud.messaging.ProductEventProducer;
import com.reactive.crud.repository.ProductRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);
    private static final String ADMIN_EMAIL = "admin@reactive-crud.com";

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductCacheService cacheService;

    @Inject
    ProductEventProducer eventProducer;

    @Inject
    MailService mailService;

    public Multi<ProductResponse> getAllProducts() {
        LOG.debug("Fetching all products");
        return productRepository.findAll()
                .onItem().transform(ProductResponse::from);
    }

    public Uni<ProductResponse> getProductById(Long id) {
        LOG.debugf("Fetching product with id: %d", id);

        // Try cache first
        return cacheService.get(id)
                .onItem().ifNull().switchTo(() -> {
                    // Cache miss - fetch from DB
                    return productRepository.findById(id)
                            .onItem().ifNotNull().transformToUni(product -> {
                                ProductResponse response = ProductResponse.from(product);
                                // Update cache
                                return cacheService.set(id, response)
                                        .replaceWith(response);
                            });
                });
    }

    public Uni<ProductResponse> createProduct(ProductRequest request) {
        LOG.debugf("Creating new product: %s", request.name());

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());

        return productRepository.create(product)
                .onItem().transform(ProductResponse::from)
                .call(response ->
                        // Cache the new product
                        cacheService.set(response.id(), response)
                                .replaceWithVoid()
                )
                .call(response ->
                        // Send Kafka event (non-blocking)
                        eventProducer.sendProductCreated(response.id(), response.name())
                                .onFailure().recoverWithNull()
                )
                .call(response ->
                        // Send email notification (non-blocking)
                        mailService.sendProductCreatedNotification(response.id(), response.name(), ADMIN_EMAIL)
                                .onFailure().recoverWithNull()
                )
                .invoke(response -> LOG.infof("Product created with id: %d", response.id()));
    }

    public Uni<ProductResponse> updateProduct(Long id, ProductRequest request) {
        LOG.debugf("Updating product with id: %d", id);

        return productRepository.findById(id)
                .onItem().ifNotNull().transformToUni(existingProduct -> {
                    existingProduct.setName(request.name());
                    existingProduct.setDescription(request.description());
                    existingProduct.setPrice(request.price());

                    return productRepository.update(existingProduct)
                            .onItem().transform(ProductResponse::from)
                            .call(response ->
                                    // Update cache
                                    cacheService.set(id, response)
                                            .replaceWithVoid()
                            )
                            .call(response ->
                                    // Send Kafka event (non-blocking)
                                    eventProducer.sendProductUpdated(response.id(), response.name())
                                            .onFailure().recoverWithNull()
                            )
                            .call(response ->
                                    // Send email notification (non-blocking)
                                    mailService.sendProductUpdatedNotification(response.id(), response.name(), ADMIN_EMAIL)
                                            .onFailure().recoverWithNull()
                            )
                            .invoke(response -> LOG.infof("Product updated with id: %d", response.id()));
                });
    }

    public Uni<Boolean> deleteProduct(Long id) {
        LOG.debugf("Deleting product with id: %d", id);

        // First get the product name for events
        return productRepository.findById(id)
                .onItem().ifNotNull().transformToUni(product ->
                        productRepository.delete(id)
                                .call(deleted -> {
                                    if (deleted) {
                                        // Delete from cache
                                        return cacheService.delete(id)
                                                .replaceWithVoid();
                                    }
                                    return Uni.createFrom().voidItem();
                                })
                                .call(deleted -> {
                                    if (deleted) {
                                        // Send Kafka event (non-blocking)
                                        return eventProducer.sendProductDeleted(product.getId(), product.getName())
                                                .onFailure().recoverWithNull();
                                    }
                                    return Uni.createFrom().voidItem();
                                })
                                .call(deleted -> {
                                    if (deleted) {
                                        // Send email notification (non-blocking)
                                        return mailService.sendProductDeletedNotification(product.getId(), product.getName(), ADMIN_EMAIL)
                                                .onFailure().recoverWithNull();
                                    }
                                    return Uni.createFrom().voidItem();
                                })
                                .invoke(deleted -> {
                                    if (deleted) {
                                        LOG.infof("Product deleted with id: %d", id);
                                    } else {
                                        LOG.warnf("Product with id %d not found for deletion", id);
                                    }
                                })
                )
                .onItem().ifNull().continueWith(false);
    }
}
