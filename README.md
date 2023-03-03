# Proxynaut - a proxy for Micronaut

A simple proxy for exposing other services through the main Micronaut server.
Just add the dependency and put in the configuration into ```application.yml```: 

```
# This will set up two proxied paths...
proxynaut:
  proxies:
    - context: /api/
      uri: https://my-backend-api-service.some-cloud.com/
      allowed-methods: ["GET"]
      timeoutMs: 30000
      invoke-using-method: "myMethod"
      qualifier: "myProxyClass"
    - context: /blobs/
      uri: https://${my.bucket.name}.some-cloud.com/
      allowed-methods: *
      invoke-using-method: "myOtherMethod"
      qualifier: "myProxyClass"
      timeoutMs: 60000
```

This config will invoke the methods for the below class:

```
import com.github.j1mrenwick.proxynaut.core.Proxy;
import com.github.j1mrenwick.proxynaut.core.ProxyFactory;
import com.github.j1mrenwick.proxynaut.core.ProxyProcessor;

@ProxyFactory("myProxyClass")
public class Test implements Proxy {

    @Inject
    protected ProxyProcessor proxy;

    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<?> myMethod(HttpRequest<ByteBuffer<?>> request, String path) {
        return proxy.serve(request, path);
    }

    public HttpResponse<?> myOtherMethod(HttpRequest<ByteBuffer<?>> request, String path) {
        return proxy.serve(request, path);
    }
}
```

If a `qualifier` is specified that does not appear in a  `ProxyFactory` annotation on a class implementing `Proxy`, then the `ProxyProcessor.serve` method will be invoked by default for proxy config. 

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
