package com.reactive.crud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductEventDto(
        @JsonProperty("eventType")
        String eventType,

        @JsonProperty("productId")
        Long productId,

        @JsonProperty("productName")
        String productName,

        @JsonProperty("timestamp")
        String timestamp
) {
    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}
