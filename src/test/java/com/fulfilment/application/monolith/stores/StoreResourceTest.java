package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  @Test
  public void testCreateStoreTriggersObserver() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "NEW_TEST_STORE", "quantityProductsInStock", 15))
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo("NEW_TEST_STORE"));
  }
}
