package com.github.j1mrenwick.proxynaut.example;

import com.github.j1mrenwick.proxynaut.core.Proxy;
import com.github.j1mrenwick.proxynaut.core.ProxyFactory;
import com.github.j1mrenwick.proxynaut.core.ProxyProcessor;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.core.annotation.Nullable;
import javax.inject.Inject;

@ProxyFactory("secureProxy")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class SecuredProxy implements Proxy {

    @Inject
    protected ProxyProcessor proxy;

    public HttpResponse<?> secured(HttpRequest<ByteBuffer<?>> request, @Nullable String path) {
        return proxy.serve(request, path);
    }
}