package com.github.j1mrenwick.proxynaut.core

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpMethod

import java.util.stream.Collectors

@Slf4j
class ProxyConfigItem {

    final static String ASTERISK = "*"
    final static Set<String> ALL_METHODS = EnumSet.allOf(HttpMethod).collect{it.name()}

    String invokeUsingMethod
    String qualifier
    int timeoutMs = 30_000
    String routeFrom
    String routeTo
    Set<String> allowedMethods = ALL_METHODS
    Set<String> includeRequestHeaders = []
    Set<String> includeRequestCookies = []
    Set<String> includeResponseHeaders = []
    Set<String> includeResponseCookies = []

    void setRouteTo(String routeTo) throws MalformedURLException {
        this.routeTo = routeTo
        try {
            routeTo.toURL()
        } catch (Exception e) {
            log.error("route-to value: $routeTo is not a valid URL")
        }
    }

    private static String safeUpper(String s) {
        return s.toUpperCase(Locale.ENGLISH)
    }

    void setAllowedMethods(Set<String> allowedMethods) {
        if (allowedMethods.contains(ASTERISK)) {
            this.allowedMethods = ALL_METHODS
        } else {
            this.allowedMethods = allowedMethods.stream()
                    .map(ProxyConfigItem::safeUpper)
                    .filter{ALL_METHODS.contains(it)}
                    .collect(Collectors.toSet())
        }
    }

    boolean shouldAllowMethod(HttpMethod method) {
        return allowedMethods.contains(method.name())
    }

    boolean shouldIncludeRequestHeader(String headerName) {
        return includeRequestHeaders.find{it.equalsIgnoreCase(headerName)} != null
    }

    boolean shouldIncludeResponseHeader(String headerName) {
        return includeResponseHeaders.find{it.equalsIgnoreCase(headerName)} != null
    }

    boolean shouldIncludeRequestCookie(String cookieName) {
        return includeRequestCookies.find{it.equalsIgnoreCase(cookieName)} != null
    }

    boolean shouldIncludeResponseCookie(String cookieName) {
        return includeResponseCookies.find{it.equalsIgnoreCase(cookieName)} != null
    }

}
