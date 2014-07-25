package com.github.kristofa.brave.client;

import org.apache.commons.lang.Validate;

import com.github.kristofa.brave.ClientRequestAdapter;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.SpanId;
import com.google.common.base.Optional;

/**
 * Intercepts a client request and takes care of tracing the request. It will decide if we need to trace current request and
 * if so CS annotation will be submitted. You will also have to use {@link ClientResponseInterceptor} which will set CR
 * annotation.
 * <p/>
 * Abstraction on top of {@link ClientTracer}. If you want to integrate a library with brave it is advised to use this
 * abstraction instead of ClientTracer directly.
 * </p>
 * You will have to implement your own ClientRequestAdapter to provide necessary context.
 * 
 * @see ClientRequestAdapter
 * @see ClientResponseInterceptor
 */
public class ClientRequestInterceptor {

    private final ClientTracer clientTracer;
    private static final String REQUEST_ANNOTATION = "request";

    public ClientRequestInterceptor(final ClientTracer clientTracer) {
        Validate.notNull(clientTracer);
        this.clientTracer = clientTracer;
    }

    /**
     * Handles a client request.
     * 
     * @param clientRequestAdapter Provides context about the request.
     * @param serviceNameOverride Optional name of the service the client request calls. In case it is not specified the name
     *            will be derived from the URI of the request. It is important the used service name should be same on client
     *            as on server side.
     */
    public void handle(final ClientRequestAdapter clientRequestAdapter, final Optional<String> serviceNameOverride) {
        final String spanName = getSpanName(clientRequestAdapter, serviceNameOverride);
        final SpanId newSpanId = clientTracer.startNewSpan(spanName);
        ClientRequestHeaders.addTracingHeaders(clientRequestAdapter, newSpanId);
        final Optional<String> serviceName = getServiceName(clientRequestAdapter, serviceNameOverride);
        if (serviceName.isPresent()) {
            clientTracer.setCurrentClientServiceName(serviceName.get());
        }
        clientTracer.submitBinaryAnnotation(REQUEST_ANNOTATION, clientRequestAdapter.getMethod() + " "
            + clientRequestAdapter.getUri());
        
        additionalHandling(clientRequestAdapter, serviceNameOverride);
        
        clientTracer.setClientSent();
    }

    private Optional<String> getServiceName(final ClientRequestAdapter clientRequestAdapter,
        final Optional<String> serviceNameOverride) {
        Optional<String> serviceName;
        if (serviceNameOverride.isPresent()) {
            serviceName = serviceNameOverride;
        } else {
            final String path = clientRequestAdapter.getUri().getPath();
            final String[] split = path.split("/");
            if (split.length > 2 && path.startsWith("/")) {
                // If path starts with '/', then context is between first two '/'. Left over is path for service.
                final int contextPathSeparatorIndex = path.indexOf("/", 1);
                serviceName = Optional.of(path.substring(1, contextPathSeparatorIndex));
            } else {
                serviceName = Optional.absent();
            }
        }
        return serviceName;
    }

    private String getSpanName(final ClientRequestAdapter clientRequestAdapter, final Optional<String> serviceNameOverride) {
        String spanName;
        final Optional<String> spanNameFromRequest = clientRequestAdapter.getSpanName();
        final String path = clientRequestAdapter.getUri().getPath();
        if (spanNameFromRequest.isPresent()) {
            spanName = spanNameFromRequest.get();
        } else if (serviceNameOverride.isPresent()) {
            spanName = path;
        } else {
            final String[] split = path.split("/");
            if (split.length > 2 && path.startsWith("/")) {
                // If path starts with '/', then context is between first two '/'. Left over is path for service.
                final int contextPathSeparatorIndex = path.indexOf("/", 1);
                spanName = path.substring(contextPathSeparatorIndex);
            } else {
                spanName = path;
            }
        }
        return spanName;
    }

    /**
     * Additional handling of Client Request. Subclasses can override this to hook in.
     * Basic implementation does nothing.
     *
     * @param clientRequestAdapter the {@link ClientRequestAdapter}.
     * @param serviceNameOverride  Optional name of the service the client request calls. In case it is not specified
     *                             the name will be derived from the URI of the request. It is important the used
     *                             service name should be same on client as on server side.
     */
    protected void additionalHandling(ClientRequestAdapter clientRequestAdapter, final Optional<String> serviceNameOverride) {
    }

    /**
     * Allow subclass to use clientTracer.
     *
     * @return a {@link ClientTracer}
     */
    protected ClientTracer getClientTracer() {
        return clientTracer;
    }
}
