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
                                        routeFrom: "/root1",
                                        routeTo: "http://some.server1/root",
                                        allowedMethods: ["get", "post"],
                                        includeRequestHeaders: ["header1", "header2", "header3", "header4"],
                                        includeResponseHeaders: ["header5", "header6"]
                                ],
                                [
                                        routeFrom: "/root2",
                                        routeTo: "http://some.server2/root",
                                        allowedMethods: ["*"],
                                        includeRequestCookies: ["cookie1", "cookie2"],
                                        includeResponseCookies: ["cookie3"]
                                ]
                        ]
                ])
        )

        then:
        applicationContext.containsBean(ProxyConfiguration)
        ProxyConfiguration proxyConfig = applicationContext.getBean(ProxyConfiguration)

        ProxyConfigItem proxy1 = proxyConfig.proxies.get(0)

        proxy1.routeFrom == "/root1"
        proxy1.routeTo == "http://some.server1/root"
        proxy1.allowedMethods.containsAll(["GET", "POST"])
        proxy1.shouldAllowMethod(HttpMethod.GET)
        proxy1.shouldAllowMethod(HttpMethod.POST)

        EnumSet.allOf(HttpMethod).find{it != HttpMethod.GET && it != HttpMethod.POST }.each{
            assert !proxy1.shouldAllowMethod(it)
        }
        proxy1.includeRequestHeaders.size() == 4
        proxy1.shouldIncludeRequestHeader("header1")
        proxy1.shouldIncludeRequestHeader("header2")
        proxy1.shouldIncludeRequestHeader("header3")
        proxy1.shouldIncludeRequestHeader("header4")
        proxy1.includeResponseHeaders.size() == 2
        proxy1.shouldIncludeResponseHeader("header5")
        proxy1.shouldIncludeResponseHeader("header6")

        ProxyConfigItem proxy2 = proxyConfig.proxies.get(1)

        proxy2.routeFrom.toString() == "/root2"
        proxy2.routeTo.toString() == "http://some.server2/root"
        proxy2.shouldAllowMethod(HttpMethod.GET)
        proxy2.shouldAllowMethod(HttpMethod.PUT)

        EnumSet.allOf(HttpMethod).each{
            assert proxy2.shouldAllowMethod(it)
        }
        proxy2.includeRequestCookies.size() == 2
        proxy2.shouldIncludeRequestCookie("cookie1")
        proxy2.shouldIncludeRequestCookie("cookie2")
        proxy2.includeResponseCookies.size() == 1
        proxy2.shouldIncludeResponseCookie("cookie3")
    }

}
