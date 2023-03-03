package com.github.j1mrenwick.proxynaut.example;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.netty.cookies.NettyCookie;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

@Controller("/")
@Secured(SecurityRule.IS_ANONYMOUS)
public class AppController {

    @Get(uri="/", produces=MediaType.TEXT_PLAIN)
    public HttpResponse<String> index(HttpRequest request) {

        MutableHttpResponse<String> response = HttpResponse.ok("This is the root");
        mapHeadersAndCookies(request, response);
        return response;
    }

    @Get(uri="/page1", produces=MediaType.TEXT_PLAIN)
    public HttpResponse<String> page1(HttpRequest request) {

        MutableHttpResponse<String> response = HttpResponse.ok("This is page 1 of your app");
        mapHeadersAndCookies(request, response);
        return response;
    }

    // do some arbitrary mapping to show header and cookie inclusion rules
    private void mapHeadersAndCookies(HttpRequest request, MutableHttpResponse response) {
        if (request.getHeaders().findFirst("X-OUT-HEADER").isPresent()) {
            response.header("X-RETURN-HEADER", "returnHeaderValue");
        }
        if (request.getCookies().get("outCookie") != null) {
            response.cookie(new NettyCookie("returnCookie", "returnCookieValue"));
        }
    }
}
