package com.reactive.crud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExternalUserDto(
        @JsonProperty("id")
        Long id,

        @JsonProperty("name")
        String name,

        @JsonProperty("email")
        String email,

        @JsonProperty("username")
        String username
) {
}
