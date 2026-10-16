package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ClientExtensionsTest {

    private static final String PERSISTED =
            "\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + GraphQLCustomizer.HELLO_SHA256 + "\"}}";

    /** Control: the GraphQL.Builder customization is active (the echo instrumentation answers). */
    @Test
    void customizationIsActive() {
        post("{\"query\":\"{ hello }\"}")
                .body("data.hello", equalTo("world"))
                .body("extensions.received", anEmptyMap());
    }

    /** Request extensions sent by the client should reach graphql-java's ExecutionInput. */
    @Test
    void clientExtensionsReachExecutionInput() {
        post("{\"query\":\"{ hello }\",\"extensions\":{\"probe\":\"x\"}}")
                .body("data.hello", equalTo("world"))
                .body("extensions.received.probe", equalTo("x"));
    }

    /** Apollo APQ: a hash-only request for a document the server knows should run it. */
    @Test
    void hashOnlyRequestRunsKnownDocument() {
        post("{" + PERSISTED + "}")
                .body("errors", nullValue())
                .body("data.hello", equalTo("world"));
    }

    /** graphql-java's own convention for transports that require a query: PersistedQuerySupport.PERSISTED_QUERY_MARKER. */
    @Test
    void persistedQueryMarkerRunsKnownDocument() {
        post("{\"query\":\"PersistedQueryMarker\"," + PERSISTED + "}")
                .body("errors", nullValue())
                .body("data.hello", equalTo("world"));
    }

    /** Control: sending the full query with its hash works, because the query text alone is enough. */
    @Test
    void fullQueryWithHashRuns() {
        post("{\"query\":\"{ hello }\"," + PERSISTED + "}")
                .body("data.hello", equalTo("world"))
                .body("extensions", notNullValue());
    }

    private static io.restassured.response.ValidatableResponse post(String body) {
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/graphql")
                .then().log().ifValidationFails().statusCode(200);
    }
}
