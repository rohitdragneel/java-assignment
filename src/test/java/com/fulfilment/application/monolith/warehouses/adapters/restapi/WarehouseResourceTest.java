package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseResourceTest {

  final String path = "warehouse";

  @Test
  @Order(1)
  public void testListAllWarehouses() {
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  public void testGetWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get(path + "/MWH.001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("location", equalTo("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  public void testGetWarehouseNotFound() {
    given()
        .when()
        .get(path + "/NON_EXISTENT_WH")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateWarehouseSuccess() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.TEST.01",
          "location": "HELMOND-001",
          "capacity": 40,
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("businessUnitCode", equalTo("MWH.TEST.01"))
        .body("location", equalTo("HELMOND-001"))
        .body("capacity", equalTo(40))
        .body("stock", equalTo(10));

    // Verify it is listed
    given()
        .when()
        .get(path + "/MWH.TEST.01")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.TEST.01"));
  }

  @Test
  @Order(5)
  public void testCreateWarehouseValidationDuplicateCode() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.001",
          "location": "AMSTERDAM-002",
          "capacity": 30,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(6)
  public void testCreateWarehouseValidationInvalidLocation() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.TEST.INV",
          "location": "INVALID-LOCATION-999",
          "capacity": 30,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(7)
  public void testCreateWarehouseValidationStockExceedsCapacity() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.TEST.OVERSTOCK",
          "location": "AMSTERDAM-002",
          "capacity": 20,
          "stock": 50
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(8)
  public void testCreateWarehouseValidationCapacityExceedsLocationLimit() {
    String payload =
        """
        {
          "businessUnitCode": "MWH.TEST.OVERCAPACITY",
          "location": "AMSTERDAM-002",
          "capacity": 200,
          "stock": 10
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(9)
  public void testCreateWarehouseValidationMaxWarehousesReached() {
    // ZWOLLE-001 allows max 1 warehouse and already has MWH.001
    String payload =
        """
        {
          "businessUnitCode": "MWH.TEST.MAXWH",
          "location": "ZWOLLE-001",
          "capacity": 20,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(10)
  public void testReplaceWarehouseSuccess() {
    // MWH.012 has stock = 5, location = AMSTERDAM-001, capacity = 50
    String replacementPayload =
        """
        {
          "businessUnitCode": "MWH.012",
          "location": "AMSTERDAM-001",
          "capacity": 60,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(replacementPayload)
        .when()
        .post(path + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.012"))
        .body("capacity", equalTo(60))
        .body("stock", equalTo(5));
  }

  @Test
  @Order(11)
  public void testReplaceWarehouseStockMismatch() {
    // Attempt to replace MWH.012 with stock that doesn't match current stock (5)
    String replacementPayload =
        """
        {
          "businessUnitCode": "MWH.012",
          "location": "AMSTERDAM-001",
          "capacity": 70,
          "stock": 25
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(replacementPayload)
        .when()
        .post(path + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(12)
  public void testArchiveWarehouseSuccess() {
    // Archive MWH.TEST.01 that was created in testCreateWarehouseSuccess
    given()
        .when()
        .delete(path + "/MWH.TEST.01")
        .then()
        .statusCode(204);

    // Should now return 404 because it is archived
    given()
        .when()
        .get(path + "/MWH.TEST.01")
        .then()
        .statusCode(404);

    // Archiving again should return 404
    given()
        .when()
        .delete(path + "/MWH.TEST.01")
        .then()
        .statusCode(404);
  }
}
