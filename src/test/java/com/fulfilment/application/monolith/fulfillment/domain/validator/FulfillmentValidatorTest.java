package com.fulfilment.application.monolith.fulfillment.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class FulfillmentValidatorTest {

  private FulfillmentValidator validator;

  @BeforeEach
  void setUp() {
    validator = new FulfillmentValidator();
  }

  // ---------- Happy path ----------

  @Test
  public void testValidConstraints_noViolations() {
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 1L,
            List.of(),           // 0 warehouses for product in store
            List.of(),           // 0 warehouses for store
            List.of()));         // 0 products in warehouse
  }

  @Test
  public void testSameWarehouseAlreadyInList_doesNotCountAgain() {
    // Warehouse already in lists (same assignment) — no constraint violation expected
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 10L,
            List.of(10L),        // already fulfilling product with warehouse 10
            List.of(10L),        // already fulfilling store with warehouse 10
            List.of(1L)));       // already stored product 1
  }

  // ---------- Constraint 1 ----------

  @Test
  public void testConstraint1_atLimit_allowsIfSameWarehouse() {
    // 2 warehouses already assigned for (store, product), but one is THE same warehouseId
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 5L,
            List.of(5L, 6L),    // warehouseId 5 is already in the list
            List.of(5L, 6L),
            List.of()));
  }

  @Test
  public void testConstraint1_atLimit_differentWarehouse_fails() {
    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        validator.validateConstraints(1L, 1L, 7L,
            List.of(5L, 6L),    // 2 different warehouses already assigned — 7 would be 3rd
            List.of(5L),
            List.of()));
    assertTrue(ex.getMessage().contains("already fulfilled by maximum of 2 warehouses"));
  }

  // ---------- Constraint 2 ----------

  @Test
  public void testConstraint2_atLimit_allowsIfSameWarehouse() {
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 5L,
            List.of(),
            List.of(5L, 6L, 7L), // 3 warehouses — but 5 is already there
            List.of()));
  }

  @Test
  public void testConstraint2_atLimit_newWarehouse_fails() {
    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        validator.validateConstraints(1L, 1L, 8L,
            List.of(),
            List.of(5L, 6L, 7L), // 3 different warehouses — 8 would be 4th
            List.of()));
    assertTrue(ex.getMessage().contains("already fulfilled by maximum of 3 warehouses"));
  }

  // ---------- Constraint 3 ----------

  @Test
  public void testConstraint3_atLimit_allowsIfSameProduct() {
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 3L, 1L,
            List.of(),
            List.of(),
            List.of(1L, 2L, 3L, 4L, 5L))); // 5 products, but 3 is already there
  }

  @Test
  public void testConstraint3_atLimit_newProduct_fails() {
    List<Long> fiveProducts = List.of(1L, 2L, 3L, 4L, 5L);
    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        validator.validateConstraints(1L, 6L, 1L,
            List.of(),
            List.of(),
            fiveProducts)); // 5 products — adding product 6 is too many
    assertTrue(ex.getMessage().contains("already stores maximum of 5 types of products"));
  }

  // ---------- Boundary checks ----------

  @Test
  public void testConstraint1_oneBeforeLimit_passes() {
    // 1 warehouse already, proposing 2nd (a new one) — should pass (limit is 2)
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 99L,
            List.of(5L),
            List.of(5L),
            List.of()));
  }

  @Test
  public void testConstraint2_twoBeforeLimit_passes() {
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 99L,
            List.of(),
            List.of(5L, 6L),
            List.of()));
  }

  @Test
  public void testConstraint3_fourBeforeLimit_passes() {
    assertDoesNotThrow(() ->
        validator.validateConstraints(1L, 1L, 1L,
            List.of(),
            List.of(),
            List.of(2L, 3L, 4L, 5L)));
  }

  @Test
  public void testAllConstraints_simultaneous_limit_fails_on_constraint1_first() {
    // Constraint 1 is checked first and should fail before constraint 2 is evaluated
    List<Long> twoWarehousesForProduct = List.of(10L, 11L);
    List<Long> threeWarehousesForStore = List.of(10L, 11L, 12L);
    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        validator.validateConstraints(1L, 1L, 13L,
            twoWarehousesForProduct,
            threeWarehousesForStore,
            List.of()));
    assertTrue(ex.getMessage().contains("already fulfilled by maximum of 2 warehouses"));
  }
}
