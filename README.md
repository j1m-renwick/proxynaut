# Proxynaut - a proxy for Micronaut

A simple proxy for exposing other services through the main Micronaut server.
Just add the dependency and put in the configuration into ```application.yml```: 

```
# This will set up two proxied paths...
proxynaut:
  proxies:
    - context: /api
      uri: https://my-backend-api-service.some-cloud.com
      allowed-methods: ["GET"]
      timeoutMs: 30000
      invoke-using-method: "myMethod"
      qualifier: "myProxyClass"
    - context: /blobs
      uri: https://${my.bucket.name}.some-cloud.com
      qualifier: "myProxyClass"
      invoke-using-method: "myOtherMethod"
      allowed-methods: ["POST", "PUT"]
      include-request-headers: [ "X-OUT-HEADER" ]
      include-response-headers: [ "X-RETURN-HEADER" ]
      include-request-cookies: [ "outCookie" ]
      include-response-cookies: [ "returnCookie" ]
      timeoutMs: 60000
```

This config will invoke the methods in the below class, 
including invoking applicable annotations (such as the Security @Secured annotation in the example) :

```
import com.github.j1mrenwick.proxynaut.core.Proxy;
import com.github.j1mrenwick.proxynaut.core.ProxyFactory;
import com.github.j1mrenwick.proxynaut.core.ProxyProcessor;

@ProxyFactory("myProxyClass")
public class Test implements Proxy {

    @Inject
    protected ProxyProcessor proxy;

    // unsecured proxy
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> myMethod(HttpRequest<ByteBuffer<?>> request, String path) {
        return proxy.serve(request, path);
    }

    // secured proxy
    public HttpResponse<?> myOtherMethod(HttpRequest<ByteBuffer<?>> request, String path) {
        return proxy.serve(request, path);
    }
}
```

If the `qualifier` is absent, or does not match a `@ProxyFactory` value on a class implementing `Proxy`, 
then the default`ProxyProcessor.serve` method will be invoked by default for the proxy. 

### Usage

See the example project in [proxynaut-example](proxynaut-example).

### To dev / run
- In the root directory, run: 
    - `./gradlew clean assemble` (builds the project)
    - `./gradlew install` (publishes the Jars to your local Maven repo)
- Start the [proxynaut-example](proxynaut-example) app
- Navigate to `http://localhost:8080/proxy/another/page1`

### To Do
- Migrate [proxynaut-example](proxynaut-example) to use Gradle
- investigate whether ByteBuffer data needs to be manually released in stream (see TODO)

License: Apache 2.0
