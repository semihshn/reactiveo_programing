package com.reactive.crud.cache;

import com.reactive.crud.dto.ProductResponse;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;

@ApplicationScoped
public class ProductCacheService {

    private static final Logger LOG = Logger.getLogger(ProductCacheService.class);
    private static final String CACHE_KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ReactiveValueCommands<String, ProductResponse> cache;

    @Inject
    public ProductCacheService(ReactiveRedisDataSource redis) {
        this.cache = redis.value(ProductResponse.class);
    }

    public Uni<ProductResponse> get(Long productId) {
        String key = CACHE_KEY_PREFIX + productId;
        return cache.get(key)
                .onItem().invoke(product -> {
                    if (product != null) {
                        LOG.debugf("Cache HIT for product id: %d", productId);
                    } else {
                        LOG.debugf("Cache MISS for product id: %d", productId);
                    }
                });
    }

    public Uni<Void> set(Long productId, ProductResponse product) {
        String key = CACHE_KEY_PREFIX + productId;
        return cache.set(key, product, new SetArgs().ex(CACHE_TTL))
                .replaceWithVoid()
                .invoke(() -> LOG.debugf("Cached product id: %d", productId));
    }

    public Uni<Boolean> delete(Long productId) {
        String key = CACHE_KEY_PREFIX + productId;
        return cache.getdel(key)
                .onItem().transform(deleted -> deleted != null)
                .invoke(deleted -> {
                    if (deleted) {
                        LOG.debugf("Deleted cache for product id: %d", productId);
                    }
                });
    }

    public Uni<Void> clear() {
        // In production, you might want to delete by pattern
        // For simplicity, we'll just log
        LOG.info("Cache clear requested (pattern-based clear not implemented)");
        return Uni.createFrom().voidItem();
    }
}
