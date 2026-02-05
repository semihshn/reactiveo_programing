package com.reactive.crud.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactive.crud.dto.ProductEventDto;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class ProductEventProducer {

    private static final Logger LOG = Logger.getLogger(ProductEventProducer.class);

    @Inject
    @Channel("product-events")
    MutinyEmitter<String> eventEmitter;

    @Inject
    ObjectMapper objectMapper;

    public Uni<Void> sendProductCreated(Long productId, String productName) {
        return sendEvent(ProductEventDto.EventType.CREATED.name(), productId, productName);
    }

    public Uni<Void> sendProductUpdated(Long productId, String productName) {
        return sendEvent(ProductEventDto.EventType.UPDATED.name(), productId, productName);
    }

    public Uni<Void> sendProductDeleted(Long productId, String productName) {
        return sendEvent(ProductEventDto.EventType.DELETED.name(), productId, productName);
    }

    private Uni<Void> sendEvent(String eventType, Long productId, String productName) {
        ProductEventDto event = new ProductEventDto(
                eventType,
                productId,
                productName,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            return eventEmitter.send(eventJson)
                    .invoke(() -> LOG.infof("Sent Kafka event: %s for product %d", eventType, productId))
                    .onFailure().invoke(failure ->
                            LOG.errorf("Failed to send Kafka event: %s", failure.getMessage())
                    );
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to serialize event: %s", e.getMessage());
            return Uni.createFrom().failure(e);
        }
    }
}
