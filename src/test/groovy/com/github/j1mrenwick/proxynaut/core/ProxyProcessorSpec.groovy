package com.github.j1mrenwick.proxynaut.core

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.netty.cookies.NettyCookie

class ProxyProcessorSpec extends AbstractOperation {

	@Override
	protected String getPrefixUnderTest() {
		return "/proxyOrigin"
	}

	void "testGET200Headers"() {
		when:
		HttpRequest request = HttpRequest.GET(getPrefixUnderTest() + "/okechoheader")
				.header("INCLUDE-HEADER", "myHeaderValue")
				.header("DONT-INCLUDE-HEADER", "someOtherValue")
		HttpResponse<String> response = client.toBlocking().exchange(request, String)

		then:
		response.body() == "Origin says 'ok'"
		response.header("INCLUDE-HEADER") == "myHeaderValue"
		!response.header("DONT-INCLUDE-HEADER")
	}

	void "testGET200Cookies"() {
		when:
		HttpRequest request = HttpRequest.GET(getPrefixUnderTest() + "/okechocookie")
				.cookie(new NettyCookie("includeThisCookie", "myCookieValue"))
				.cookie(new NettyCookie("dontIncludeThisCookie", "someOtherCookieValue"))
		HttpResponse<String> response = client.toBlocking().exchange(request, String)

		then:
		response.body() == "Origin says 'ok'"
		Optional<Cookie> cookie = response.getCookie("includeThisCookie")
		cookie.present
		cookie.get().value == "myCookieValue"
		!response.getCookie("dontIncludeThisCookie").present
	}

}
