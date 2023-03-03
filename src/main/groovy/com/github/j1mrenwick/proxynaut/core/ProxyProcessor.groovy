package com.github.j1mrenwick.proxynaut.core

import groovy.util.logging.Slf4j
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Executable
import io.micronaut.core.io.buffer.ByteBuffer
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpResponseWrapper
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.client.RxStreamingHttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.cookie.Cookies
import io.micronaut.http.simple.SimpleHttpRequestFactory
import io.micronaut.http.simple.SimpleHttpResponseFactory
import io.reactivex.Flowable
import io.reactivex.processors.UnicastProcessor
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import io.micronaut.core.annotation.Nullable
import javax.inject.Singleton
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Singleton
@Slf4j
class ProxyProcessor implements Closeable {

    private final Collection<ProxyConfigItem> configs

    private Map<String, RxStreamingHttpClient> proxyMap = Collections.synchronizedMap(new HashMap<>())

    private BeanContext beanContext

	ProxyProcessor(ProxyConfiguration proxyConfiguration, BeanContext beanContext) {
        this.configs = proxyConfiguration.proxies
        this.beanContext = beanContext
    }

    @Executable
	HttpResponse<Flowable<byte[]>> serve(HttpRequest<ByteBuffer<?>> request, @Nullable String path) {
		path = path ?: ""
		String proxyContextPath
		// strip trailing "/" from request path if it exists, before getting the path root
		if (request.path.lastIndexOf("/") == request.path.length() - 1) {
			proxyContextPath = request.path.substring(0, request.path.length() - 1)
		} else {
			proxyContextPath = request.path.substring(0, request.path.length() - path.length())
		}
        Optional<ProxyConfigItem> configOptional = findConfigForRequest(proxyContextPath)
        if (!configOptional.present) {
        	// This should never happen, only if Micronaut's router somehow was confused
        	List<String> prefixes = configs.collect{it.context}
            log.warn("Matched " + request.method + " " + request.path + " to the proxy, but no configuration is found. Prefixes found in config: " + prefixes)
            return HttpResponse.status(HttpStatus.BAD_REQUEST, "Unknown proxy path: " + proxyContextPath)
        }

		ProxyConfigItem config = configOptional.get()

        MutableHttpRequest<Object> upstreamRequest = buildRequest(request, path, config)

        RxStreamingHttpClient client = findOrCreateClient(config)
        log.info("About to pivot proxy call to " + config.uri + path)
        Flowable<HttpResponse<ByteBuffer<?>>> upstreamResponseFlowable = client.exchangeStream(upstreamRequest).serialize()

        CompletableFuture<HttpResponse<Flowable<byte[]>>> futureResponse = buildResponse(config, upstreamResponseFlowable)

        try {
			return futureResponse.get(config.timeoutMs, TimeUnit.MILLISECONDS)
		} catch (ExecutionException e) {
			return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
		} catch (TimeoutException e) {
			log.error("Timeout occurred before getting upstream headers (configured to {} millisecond(s)", config.timeoutMs)
			return HttpResponse.status(HttpStatus.BAD_GATEWAY)
		} catch (Throwable e) {
			log.error("Exception occurred while proxying - ${e.toString()}")
			return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
		}
    }

	private CompletableFuture<HttpResponse<Flowable<byte[]>>> buildResponse(ProxyConfigItem config,
			Flowable<HttpResponse<ByteBuffer<?>>> upstreamResponseFlowable) {
		CompletableFuture<HttpResponse<Flowable<byte[]>>> futureResponse = new CompletableFuture<>()
        UnicastProcessor<byte[]> responseBodyFlowable = UnicastProcessor.create()

        upstreamResponseFlowable.subscribe(new Subscriber<HttpResponse<ByteBuffer<?>>>() {

			private Subscription subscription

			@Override
			void onSubscribe(Subscription s) {
				this.subscription = s
				s.request(1)
			}

			@Override
			void onNext(HttpResponse<ByteBuffer<?>> upstreamResponse) {
				if (log.isTraceEnabled()) {
					log.trace("************ Read Response from {}", upstreamResponse.body().toString(StandardCharsets.UTF_8))
				}
				// When the upstream first first packet comes in, complete the response
				if (! futureResponse.isDone()) {
					log.info("Completed pivot: " + upstreamResponse.status)
					HttpResponse response = makeResponse(upstreamResponse, responseBodyFlowable, config)
					futureResponse.complete(response)
				}
				ByteBuffer<?> byteBuffer = upstreamResponse.body()
				responseBodyFlowable.onNext(byteBuffer.toByteArray())
//				// TODO is this needed? yes, likely.
//				if (byteBuffer instanceof ReferenceCounted) {
//					((ReferenceCounted)byteBuffer).release()
//				}
				subscription.request(1)
			}

			@Override
			void onError(Throwable t) {
				if (t instanceof HttpClientResponseException && ! futureResponse.done) {
					HttpClientResponseException upstreamException = (HttpClientResponseException) t
					log.info("HTTP error from upstream: " + upstreamException.status.reason)
			    	HttpResponse<ByteBuffer<?>> upstreamResponse = (HttpResponse<ByteBuffer<?>>) upstreamException.response

					log.info("Completed pivot: " + upstreamResponse.status)
					HttpResponse response = makeErrorResponse(upstreamResponse, config)
					futureResponse.complete((HttpResponse<Flowable<byte[]>>) response)
				} else {
					log.info("Proxy got unknown error from upstream: " + t.message, t)
					responseBodyFlowable.onError(t)
				}
			}

			@Override
			void onComplete() {
				log.trace("Upstream response body done")
				responseBodyFlowable.onComplete()
			}
		})
		return futureResponse
	}

	private MutableHttpRequest<Object> buildRequest(HttpRequest<ByteBuffer<?>> request, String path,
			ProxyConfigItem config) {
		String originPath = config.uri.toString() + path
        String queryPart = request.uri.query
        String originUri = queryPart ? "$originPath?$queryPart" : originPath
        log.debug("Proxy'ing incoming " + request.method + " " + request.path + " -> " + originPath)
        MutableHttpRequest<Object> upstreamRequest = SimpleHttpRequestFactory.INSTANCE.create(request.method,
                originUri)

		// TODO limitation: only supports one header value
		for (Map.Entry<String, List<String>> header : request.headers) {
			if (config.shouldIncludeRequestHeader(header.key)) {
				upstreamRequest.header(header.key, header.value.first())
			}
		}

		for (Map.Entry<String, Cookie> cookie : request.cookies) {
			if (config.shouldIncludeRequestCookie(cookie.key)) {
				upstreamRequest.cookie(cookie.value)
			}
		}

		Optional<?> body = request.getBody()
        if (HttpMethod.permitsRequestBody(request.method)) {
            body.ifPresent((Object b) -> upstreamRequest.body(b))
        }
		return upstreamRequest
	}

	protected HttpResponse makeResponse(HttpResponse<?> upstreamResponse,
    		Flowable<byte[]> responseFlowable,
			ProxyConfigItem config) {
		MutableHttpResponse<Flowable<byte[]>> httpResponse = SimpleHttpResponseFactory.INSTANCE.status(upstreamResponse.status, responseFlowable)
		upstreamResponse.contentType.ifPresent(mediaType -> httpResponse.contentType(mediaType))

		// TODO limitation: only supports one header value
		for (Map.Entry<String, List<String>> header : upstreamResponse.headers) {
			if (config.shouldIncludeResponseHeader(header.key)) {
				httpResponse.header(header.key, header.value.first())
			}
		}

		Cookies cookies = upstreamResponse instanceof HttpResponseWrapper ?
				upstreamResponse.delegate.cookies :
				upstreamResponse.cookies

		for (Map.Entry<String, Cookie> cookie : cookies) {
			if (config.shouldIncludeResponseCookie(cookie.key)) {
				httpResponse.cookie(cookie.value)
			}
		}

		return httpResponse
	}

	protected HttpResponse makeErrorResponse(HttpResponse<?> upstreamResponse,
			ProxyConfigItem config) {
		MutableHttpResponse<Flowable<byte[]>> httpResponse = SimpleHttpResponseFactory.INSTANCE.status(upstreamResponse.status, Flowable.empty())
		upstreamResponse.contentType.ifPresent(mediaType -> httpResponse.contentType(mediaType))
		return httpResponse
	}


	private RxStreamingHttpClient findOrCreateClient(ProxyConfigItem config) {
        return proxyMap.computeIfAbsent("${config.qualifier}:${config.invokeUsingMethod}", n -> {
            log.debug("Creating proxy for " + config.url)
            return beanContext.createBean(RxStreamingHttpClient, new Object[]{config.url})
        })
    }

    private Optional<ProxyConfigItem> findConfigForRequest(String prefix) {
        return Optional.ofNullable(configs.find{it.context == prefix})
    }

    @Override
	void close() throws IOException {
        proxyMap.values().forEach(client -> client.stop())
        proxyMap.clear()
    }
}
