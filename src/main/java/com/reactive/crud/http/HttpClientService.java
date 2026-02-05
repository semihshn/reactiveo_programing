package com.reactive.crud.http;

import com.reactive.crud.dto.ExternalUserDto;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class HttpClientService {

    private static final Logger LOG = Logger.getLogger(HttpClientService.class);

    @Inject
    @RestClient
    ExternalApiClient externalApiClient;

    public Uni<List<ExternalUserDto>> fetchAllUsers() {
        LOG.debug("Fetching all users from external API");
        return externalApiClient.getAllUsers()
                .invoke(users -> LOG.infof("Fetched %d users from external API", users.size()))
                .onFailure().invoke(failure ->
                        LOG.errorf("Failed to fetch users: %s", failure.getMessage())
                );
    }

    public Uni<ExternalUserDto> fetchUserById(Long userId) {
        LOG.debugf("Fetching user %d from external API", userId);
        return externalApiClient.getUserById(userId)
                .invoke(user -> LOG.infof("Fetched user: %s", user.name()))
                .onFailure().invoke(failure ->
                        LOG.errorf("Failed to fetch user %d: %s", userId, failure.getMessage())
                );
    }

    public Uni<String> fetchUserEmailById(Long userId) {
        return fetchUserById(userId)
                .onItem().transform(ExternalUserDto::email);
    }
}
