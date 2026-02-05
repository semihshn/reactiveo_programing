package com.reactive.crud.http;

import com.reactive.crud.dto.ExternalUserDto;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/users")
@RegisterRestClient(configKey = "external-api")
public interface ExternalApiClient {

    @GET
    Uni<List<ExternalUserDto>> getAllUsers();

    @GET
    @Path("/{id}")
    Uni<ExternalUserDto> getUserById(@PathParam("id") Long id);
}
