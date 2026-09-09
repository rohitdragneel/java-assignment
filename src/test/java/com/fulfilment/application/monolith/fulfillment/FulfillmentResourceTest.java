package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FulfillmentResourceTest {

  @Inject FulfillmentRepository fulfillmentRepository;

  @BeforeEach
  @Transactional
  public void setUp() {
    fulfillmentRepository.deleteAll();
  }

  @Test
  public void testCreateFulfillmentSuccess() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("storeId", 1, "productId", 1, "warehouseId", 1))
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("storeId", equalTo(1))
        .body("productId", equalTo(1))
        .body("warehouseId", equalTo(1));
  }

  @Test
  public void testCreateFulfillmentWithBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("storeId", 1, "productId", 1, "warehouseBusinessUnitCode", "MWH.001"))
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("storeId", equalTo(1))
        .body("productId", equalTo(1));
  }

  @Test
  public void testCreateFulfillmentNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("storeId", 9999, "productId", 1, "warehouseId", 1))
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(404);
  }

  @Test
  public void testListFulfillment() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("storeId", 1, "productId", 1, "warehouseId", 1))
        .when()
        .post("/fulfillment")
        .then()
        .statusCode(201);

    given()
        .when()
        .get("/fulfillment?storeId=1")
        .then()
        .statusCode(200)
        .body("size()", equalTo(1));
  }

  @Test
  public void testDeleteFulfillment() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("storeId", 1, "productId", 1, "warehouseId", 1))
            .when()
            .post("/fulfillment")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete("/fulfillment/" + id).then().statusCode(204);

    given().when().get("/fulfillment").then().statusCode(200).body("size()", equalTo(0));
  }
}
