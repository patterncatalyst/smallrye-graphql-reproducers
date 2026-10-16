package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class DeprecatedIntrospectionTest {

    @Test
    void printedSchemaMarksTheFieldsDeprecated() {
        given().when().get("/graphql/schema.graphql")
                .then().statusCode(200)
                .body(containsString("withSmallRye: String @deprecated(reason : \"Use `current`.\")"))
                .body(containsString("oldItem: Item @deprecated(reason : \"Use `item`.\")"));
    }

    @Test
    void introspectionReportsTheSmallRyeAnnotationOnAField() {
        post(fieldsQuery("Item", true))
                .body("data.__type.fields.find { it.name == 'withSmallRye' }.isDeprecated", equalTo(true))
                .body("data.__type.fields.find { it.name == 'withSmallRye' }.deprecationReason", equalTo("Use `current`."));
    }

    @Test
    void introspectionReportsJavaLangDeprecatedOnAField() {
        post(fieldsQuery("Item", true))
                .body("data.__type.fields.find { it.name == 'withJavaLang' }.isDeprecated", equalTo(true));
    }

    @Test
    void introspectionReportsTheSmallRyeAnnotationOnAQuery() {
        post("{\"query\":\"{ __schema { queryType { fields(includeDeprecated: true) { name isDeprecated deprecationReason } } } }\"}")
                .body("data.__schema.queryType.fields.find { it.name == 'oldItem' }.isDeprecated", equalTo(true))
                .body("data.__schema.queryType.fields.find { it.name == 'oldItem' }.deprecationReason", equalTo("Use `item`."));
    }

    @Test
    void deprecatedFieldsAreHiddenByDefault() {
        post(fieldsQuery("Item", false))
                .body("data.__type.fields.name", not(hasItem("withSmallRye")))
                .body("data.__type.fields.name", not(hasItem("withJavaLang")));
    }

    private static String fieldsQuery(String type, boolean includeDeprecated) {
        return "{\"query\":\"{ __type(name: \\\"" + type + "\\\") { fields(includeDeprecated: " + includeDeprecated
                + ") { name isDeprecated deprecationReason } } }\"}";
    }

    private static io.restassured.response.ValidatableResponse post(String body) {
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/graphql")
                .then().log().ifValidationFails().statusCode(200);
    }
}
