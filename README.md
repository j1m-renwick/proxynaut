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
- Navigate to `http://localhost:8080/proxy/page1`

### To Do
- Migrate [proxynaut-example](proxynaut-example) to use Gradle
- Resolve all WARNING messages at build time (`WARNING: Usages of deprecated annotation javax.annotation.Nullable found. You should use io.micronaut.core.annotation.Nullable instead`) 
- Update package names
- Resolve potential memory leak issue / investigate whether ByteBuffer data needs to be manually releasing (see TODO)
- Review / implement additional Proxy configuration options - the following are advertised in the code but not implemented:
    - `include-request-headers`
    - `exclude-request-headers`
    - `include-response-headers`
    - `exclude-response-headers`
- Investigate additional Proxy configuration options
  - Ability to point a proxy at a particular method to utilise inbuilt annotations such as `@Secured`

License: Apache 2.0
