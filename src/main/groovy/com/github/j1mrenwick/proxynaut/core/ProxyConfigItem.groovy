package com.github.j1mrenwick.proxynaut.core


import io.micronaut.http.HttpMethod

import java.util.stream.Collectors

class ProxyConfigItem {

    final static String ASTERISK = "*"
    final static Set<String> ALL_METHODS = EnumSet.allOf(HttpMethod).collect{it.name()}

    String invokeUsingMethod
    String qualifier
    int timeoutMs = 30_000
    String context = null
    URI uri = null
    Set<String> allowedMethods = ALL_METHODS
    Set<String> includeRequestHeaders = []
    Set<String> includeRequestCookies = []
    Set<String> includeResponseHeaders = []
    Set<String> includeResponseCookies = []
    URL url

    void setUri(URI uri) throws MalformedURLException {
        this.uri = uri
        this.url = uri.toURL()
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
