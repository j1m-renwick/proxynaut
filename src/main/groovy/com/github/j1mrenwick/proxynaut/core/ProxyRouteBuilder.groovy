package com.github.j1mrenwick.proxynaut.core

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ExecutionHandleLocator
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.web.router.DefaultRouteBuilder

import javax.inject.Singleton

@Singleton
@Slf4j
class ProxyRouteBuilder extends DefaultRouteBuilder {

    ApplicationContext applicationContext

    ProxyRouteBuilder(
            ProxyConfiguration config,
            ExecutionHandleLocator executionHandleLocator, UriNamingStrategy uriNamingStrategy,
            ApplicationContext applicationContext) {
        super(executionHandleLocator, uriNamingStrategy)
        this.applicationContext = applicationContext
        buildProxyRoutes(config)
    }

    void buildProxyRoutes(ProxyConfiguration config) {
        if (log.isDebugEnabled()) {
            log.debug("Building proxy routes...")
        }
        for (ProxyConfigItem item : config.proxies) {
            String contextPath = item.routeFrom + "{+path:?}"
            for (HttpMethod method : HttpMethod.values()) {
                if (item.shouldAllowMethod(method)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding route: $method $contextPath")
                    }
                    Class clazz = null
                    String invokeUsingMethod = null
                    if (item.qualifier) {
                        try {
                            Proxy bean = applicationContext.getBean(Proxy, Qualifiers.byName(item.qualifier))
                            clazz = bean.class
                            invokeUsingMethod = item.invokeUsingMethod ?: "proxy"
                        } catch (NoSuchBeanException e) {
                            log.warn("Qualifier ${item.qualifier} did not return exactly one bean - using default class and method instead.")
                        }
                    }

                    clazz = clazz?: ProxyProcessor
                    invokeUsingMethod = invokeUsingMethod?: "serve"

                    buildRoute(method, contextPath, clazz, invokeUsingMethod, HttpRequest, String)
                }
            }
        }
    }
}
