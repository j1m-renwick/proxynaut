package com.github.j1mrenwick.proxynaut.core


import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.PropertySource
import io.micronaut.http.HttpMethod
import spock.lang.Specification

class ProxyConfigurationSpec extends Specification {

    void "test proxy configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(PropertySource.of(
                [
                        "proxynaut.proxies":[
                                [
                                        context: "/root",
                                        uri: "http://some.server/root",
                                        allowedMethods: ["get", "post"],
                                        includeRequestHeaders: ["Cookie-Control"],
                                        excludeResponseHeaders: ["Content-Disposition"]
                                ],
                                [
                                        context: "/root",
                                        uri: "http://some.server/root",
                                        allowedMethods: ["get", "put"],
                                        includeRequestHeaders: ["Authentication"],
                                        includeResponseHeaders: ["Cookie-Control"]
                                ],
                                [
                                        context: "/root",
                                        uri: "http://some.server/root",
                                        allowedMethods: ["*"],
                                        excludeRequestHeaders: ["Authentication"],
                                        excludeResponseHeaders: ["X-Powered-By"]
                                ]
                        ]
                ])
        )

        then:
        applicationContext.containsBean(ProxyConfiguration)
        ProxyConfiguration proxyConfig = applicationContext.getBean(ProxyConfiguration)

        ProxyConfigItem proxy = proxyConfig.proxies.get(0)

        proxy.getContext().toString() == "/root"
        proxy.getUri().toString() == "http://some.server/root"
        proxy.getAllowedMethods().contains("GET")
        proxy.shouldAllowMethod(HttpMethod.GET)
        !proxy.shouldAllowMethod(HttpMethod.PUT)
        proxy.shouldIncludeRequestHeader("Cookie-Control")
        !proxy.shouldIncludeRequestHeader("Authentication")
        !proxy.shouldIncludeResponseHeader("Content-Disposition")
        proxy.shouldIncludeResponseHeader("X-Powered-By")
    }

}
