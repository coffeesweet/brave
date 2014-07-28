package com.github.kristofa.brave.client;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static com.google.common.base.Optional.absent;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import org.mockito.InOrder;

import com.github.kristofa.brave.ClientRequestAdapter;
import com.github.kristofa.brave.ClientTracer;

import com.google.common.base.Optional;

public class ClientRequestInterceptorHookTest {

    private static final Optional<String> ABSENT_OPTIONAL_STRING = absent();
    private static final String ADDITIONAL_BINARY_ANNOTATION_KEY = "foo";
    private static final String ADDITIONAL_BINARY_ANNOTATION_VALUE = "bar";

    private ClientRequestInterceptor clientRequestInterceptor;

    private ClientTracer mockClientTracer;
    private ClientRequestAdapter mockRequestAdapter;

    @Before
    public void setUp() throws Exception {
        mockClientTracer = mock(ClientTracer.class);

        mockRequestAdapter = mock(ClientRequestAdapter.class);
        when(mockRequestAdapter.getUri()).thenReturn(URI.create("/path/to/resource"));
        when(mockRequestAdapter.getSpanName()).thenReturn(ABSENT_OPTIONAL_STRING);

        clientRequestInterceptor = new ClientRequestInterceptorHook(mockClientTracer);
    }

    @Test
    public void testAdditionalHandlingHooksInBeforeClientSent() throws Exception {
        clientRequestInterceptor.handle(mockRequestAdapter, ABSENT_OPTIONAL_STRING);

        final InOrder inOrder = inOrder(mockClientTracer);

        inOrder.verify(mockClientTracer).startNewSpan(anyString());
        inOrder.verify(mockClientTracer).setCurrentClientServiceName(anyString());
        inOrder.verify(mockClientTracer).submitBinaryAnnotation(eq("request"), anyString());

        // this is the hooked-in call
        inOrder.verify(mockClientTracer).submitBinaryAnnotation(eq(ADDITIONAL_BINARY_ANNOTATION_KEY),
            eq(ADDITIONAL_BINARY_ANNOTATION_VALUE));

        inOrder.verify(mockClientTracer).setClientSent();
        verifyNoMoreInteractions(mockClientTracer);
    }

    private static class ClientRequestInterceptorHook extends ClientRequestInterceptor {

        public ClientRequestInterceptorHook(final ClientTracer clientTracer) {
            super(clientTracer);
        }

        @Override
        protected void additionalHandling(final ClientRequestAdapter clientRequestAdapter,
                final Optional<String> serviceNameOverride) {
            getClientTracer().submitBinaryAnnotation(ADDITIONAL_BINARY_ANNOTATION_KEY,
                ADDITIONAL_BINARY_ANNOTATION_VALUE);
        }
    }
}
