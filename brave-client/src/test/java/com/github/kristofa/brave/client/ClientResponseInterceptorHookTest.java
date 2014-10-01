package com.github.kristofa.brave.client;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import org.mockito.InOrder;

import com.github.kristofa.brave.ClientResponseAdapter;
import com.github.kristofa.brave.ClientTracer;

/**
 * @author  ahartmann
 */
public class ClientResponseInterceptorHookTest {

    private static final String ADDITIONAL_BINARY_ANNOTATION_KEY = "foo";
    private static final String ADDITIONAL_BINARY_ANNOTATION_VALUE = "bar";

    private ClientResponseInterceptor clientResponseInterceptor;

    private ClientTracer mockClientTracer;
    private ClientResponseAdapter mockRequestAdapter;

    @Before
    public void setUp() throws Exception {
        mockClientTracer = mock(ClientTracer.class);

        mockRequestAdapter = mock(ClientResponseAdapter.class);
        when(mockRequestAdapter.getStatusCode()).thenReturn(200);

        clientResponseInterceptor = new ClientResponseInterceptorHook(mockClientTracer);
    }

    @Test
    public void testAdditionalHandlingHooksInBeforeClientReceived() throws Exception {
        clientResponseInterceptor.handle(mockRequestAdapter);

        final InOrder inOrder = inOrder(mockClientTracer);

        inOrder.verify(mockClientTracer).submitBinaryAnnotation(eq("http.responsecode"), anyInt());

        // this is the hooked-in call
        inOrder.verify(mockClientTracer).submitBinaryAnnotation(eq(ADDITIONAL_BINARY_ANNOTATION_KEY),
            eq(ADDITIONAL_BINARY_ANNOTATION_VALUE));

        inOrder.verify(mockClientTracer).setClientReceived();
        verifyNoMoreInteractions(mockClientTracer);
    }

    private static class ClientResponseInterceptorHook extends ClientResponseInterceptor {

        public ClientResponseInterceptorHook(final ClientTracer clientTracer) {
            super(clientTracer);
        }

        @Override
        protected void additionalHandling(final ClientResponseAdapter clientResponseAdapter) {
            getClientTracer().submitBinaryAnnotation(ADDITIONAL_BINARY_ANNOTATION_KEY,
                ADDITIONAL_BINARY_ANNOTATION_VALUE);
        }
    }
}
