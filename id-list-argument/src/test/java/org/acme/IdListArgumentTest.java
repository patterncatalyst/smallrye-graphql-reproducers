package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class IdListArgumentTest {

    @Test
    void schemaDeclaresAListOfIds() {
        given().when().get("/graphql/schema.graphql")
                .then().statusCode(200)
                .body(containsString("echoIds(ids: [ID]): [String]"));
    }

    @Test
    void singleIdArgumentWorks() {
        post("{\"query\":\"{ echoId(id: \\\"a\\\") }\"}")
                .body("errors", nullValue())
                .body("data.echoId", equalTo("a"));
    }

    @Test
    void listOfIdsAsLiteral() {
        post("{\"query\":\"{ echoIds(ids: [\\\"a\\\", \\\"b\\\"]) }\"}")
                .body("errors", nullValue())
                .body("data.echoIds", contains("a", "b"));
    }

    @Test
    void listOfIdsAsVariable() {
        post("{\"query\":\"query($ids: [ID]) { echoIds(ids: $ids) }\",\"variables\":{\"ids\":[\"a\",\"b\"]}}")
                .body("errors", nullValue())
                .body("data.echoIds", contains("a", "b"));
    }

    private static io.restassured.response.ValidatableResponse post(String body) {
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/graphql")
                .then().log().ifValidationFails().statusCode(200);
    }
}
