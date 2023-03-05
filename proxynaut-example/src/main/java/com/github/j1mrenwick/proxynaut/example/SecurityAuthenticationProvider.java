package com.github.j1mrenwick.proxynaut.example;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.reactivex.Flowable;
import java.util.ArrayList;
import javax.inject.Singleton;
import org.reactivestreams.Publisher;

@Singleton
public class SecurityAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest,
                                                          AuthenticationRequest<?, ?> authenticationRequest) {
        return Flowable.fromCallable(() -> {
            if (authenticationRequest.getIdentity().equals("letmein")) {
                return new UserDetails("me", new ArrayList<>());
            } else {
                return new AuthenticationFailed("username must be: letmein");
            }
        });
    }
}
