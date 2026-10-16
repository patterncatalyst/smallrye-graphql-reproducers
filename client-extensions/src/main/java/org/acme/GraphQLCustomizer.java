package org.acme;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport;
import graphql.execution.preparsed.persisted.InMemoryPersistedQueryCache;

/**
 * Uses SmallRye's documented hook (the GraphQL.Builder CDI event) to add two plain graphql-java features:
 * an instrumentation that echoes the request extensions graphql-java received, and graphql-java's own
 * Apollo automatic persisted query support.
 */
@ApplicationScoped
public class GraphQLCustomizer {

    static final String HELLO = "{ hello }";
    static final String HELLO_SHA256 = "001c3174e099bd72b729d0c0a529ba9f5a740c446e2a6e1d71b283cb84ec3065";

    void customize(@Observes GraphQL.Builder builder) {
        // GraphQL.Builder has no getters; build once to read SmallRye's own instrumentation chain and keep it.
        var smallRyeInstrumentation = builder.build().getInstrumentation();
        builder.instrumentation(new ChainedInstrumentation(List.of(smallRyeInstrumentation, new EchoExtensions())));
        // One known document, so a hash-only request for it should run without sending the query text.
        builder.preparsedDocumentProvider(new ApolloPersistedQuerySupport(
                InMemoryPersistedQueryCache.newInMemoryPersistedQueryCache()
                        .addQuery(HELLO_SHA256, HELLO).build()));
    }

    /** Adds {@code extensions.received}: the map graphql-java saw as ExecutionInput#getExtensions(). */
    static final class EchoExtensions extends SimplePerformantInstrumentation {
        @Override
        public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult result,
                InstrumentationExecutionParameters parameters, InstrumentationState state) {
            var received = parameters.getExecutionInput().getExtensions();
            return CompletableFuture.completedFuture(ExecutionResultImpl.newExecutionResult().from(result)
                    .addExtension("received", received == null ? Map.of() : received).build());
        }
    }
}
