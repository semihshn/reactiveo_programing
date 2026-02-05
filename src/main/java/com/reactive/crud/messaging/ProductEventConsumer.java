package com.reactive.crud.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactive.crud.dto.ProductEventDto;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductEventConsumer {

    private static final Logger LOG = Logger.getLogger(ProductEventConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Incoming("product-events-consumer")
    public Uni<Void> consumeProductEvent(String eventJson) {
        return Uni.createFrom().item(eventJson)
                .onItem().transform(json -> {
                    try {
                        return objectMapper.readValue(json, ProductEventDto.class);
                    } catch (Exception e) {
                        LOG.errorf("Failed to parse event: %s", e.getMessage());
                        return null;
                    }
                })
                .onItem().ifNotNull().invoke(event -> {
                    LOG.infof("Received Kafka event: %s for product %d (%s) at %s",
                            event.eventType(),
                            event.productId(),
                            event.productName(),
                            event.timestamp()
                    );

                    // Here you can add business logic based on event type
                    switch (event.eventType()) {
                        case "CREATED":
                            LOG.info("Processing CREATED event");
                            break;
                        case "UPDATED":
                            LOG.info("Processing UPDATED event");
                            break;
                        case "DELETED":
                            LOG.info("Processing DELETED event");
                            break;
                        default:
                            LOG.warnf("Unknown event type: %s", event.eventType());
                    }
                })
                .replaceWithVoid();
    }
}
