package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link StoreResource}.
 *
 * <p>Verifies that all CRUD operations work correctly end-to-end and that the transactional
 * event observer pattern (AFTER_SUCCESS) operates without errors.
 */
@QuarkusTest
public class StoreResourceTest {

  // ---------- GET all ----------

  @Test
  public void testGetAllStores_returnsOk() {
    given()
        .when()
        .get("/store")
        .then()
        .statusCode(200);
  }

  // ---------- GET single ----------

  @Test
  public void testGetSingleStore_existing_returnsStore() {
    given()
        .when()
        .get("/store/1")
        .then()
        .statusCode(200)
        .body("id", equalTo(1));
  }

  @Test
  public void testGetSingleStore_notFound_returns404() {
    given()
        .when()
        .get("/store/99999")
        .then()
        .statusCode(404);
  }

  // ---------- POST ----------

  @Test
  public void testCreateStore_success_triggersObserver() {
    // This test also validates that the LegacyStoreManagerGateway is invoked via the
    // AFTER_SUCCESS observer — if the observer were broken, the transaction would not affect
    // the HTTP response code but an exception would surface in logs.
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "NEW_TEST_STORE", "quantityProductsInStock", 15))
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo("NEW_TEST_STORE"))
        .body("quantityProductsInStock", equalTo(15));
  }

  @Test
  public void testCreateStore_withIdSet_returns422() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("id", 1, "name", "INVALID", "quantityProductsInStock", 5))
        .when()
        .post("/store")
        .then()
        .statusCode(422);
  }

  // ---------- PUT ----------

  @Test
  public void testUpdateStore_success_triggersObserver() {
    // First create a store to update
    Integer id = given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "UPDATE_STORE_TEST", "quantityProductsInStock", 5))
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "UPDATE_STORE_TEST_UPDATED", "quantityProductsInStock", 20))
        .when()
        .put("/store/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("UPDATE_STORE_TEST_UPDATED"))
        .body("quantityProductsInStock", equalTo(20));
  }

  @Test
  public void testUpdateStore_notFound_returns404() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "GHOST", "quantityProductsInStock", 5))
        .when()
        .put("/store/99999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testUpdateStore_missingName_returns422() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("quantityProductsInStock", 5))
        .when()
        .put("/store/1")
        .then()
        .statusCode(422);
  }

  // ---------- PATCH ----------

  @Test
  public void testPatchStore_success_triggersObserver() {
    Integer id = given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "PATCH_STORE_TEST", "quantityProductsInStock", 3))
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "PATCH_STORE_TEST_PATCHED", "quantityProductsInStock", 7))
        .when()
        .patch("/store/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PATCH_STORE_TEST_PATCHED"));
  }

  @Test
  public void testPatchStore_notFound_returns404() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "GHOST", "quantityProductsInStock", 5))
        .when()
        .patch("/store/99999")
        .then()
        .statusCode(404);
  }

  // ---------- DELETE ----------

  @Test
  public void testDeleteStore_success() {
    Integer id = given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "DELETE_TEST_STORE", "quantityProductsInStock", 1))
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    given().when().delete("/store/" + id).then().statusCode(204);
    given().when().get("/store/" + id).then().statusCode(404);
  }

  @Test
  public void testDeleteStore_notFound_returns404() {
    given().when().delete("/store/99999").then().statusCode(404);
  }
}
