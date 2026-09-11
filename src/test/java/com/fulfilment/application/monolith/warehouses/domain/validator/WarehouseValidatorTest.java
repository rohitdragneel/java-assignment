package com.fulfilment.application.monolith.warehouses.domain.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link WarehouseValidator}.
 * No Quarkus runtime needed — instant feedback.
 */
public class WarehouseValidatorTest {

  private WarehouseValidator validator;

  private static Warehouse warehouse(String code, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  private static Location location(String id, int maxWarehouses, int maxCapacity) {
    return new Location(id, maxWarehouses, maxCapacity);
  }

  @BeforeEach
  void setUp() {
    validator = new WarehouseValidator();
  }

  // ---------- Creation — happy paths ----------

  @Test
  public void testCreate_happyPath_passes() {
    Warehouse w = warehouse("NEW-001", "ZWOLLE-001", 30, 10);
    Location loc = location("ZWOLLE-001", 2, 80);
    assertDoesNotThrow(() -> validator.validateForCreation(w, null, loc, List.of()));
  }

  @Test
  public void testCreate_noStock_passes() {
    Warehouse w = warehouse("NEW-001", "ZWOLLE-001", 30, 0);
    Location loc = location("ZWOLLE-001", 2, 80);
    assertDoesNotThrow(() -> validator.validateForCreation(w, null, loc, List.of()));
  }

  // ---------- Creation — duplicate code ----------

  @Test
  public void testCreate_duplicateCode_fails() {
    Warehouse existing = warehouse("WH-001", "ZWOLLE-001", 20, 5);
    Warehouse w = warehouse("WH-001", "ZWOLLE-001", 30, 10);
    Location loc = location("ZWOLLE-001", 2, 80);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForCreation(w, existing, loc, List.of()));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  // ---------- Creation — invalid location ----------

  @Test
  public void testCreate_invalidLocation_fails() {
    Warehouse w = warehouse("NEW-001", "INVALID-LOC", 30, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForCreation(w, null, null, List.of()));
    assertTrue(ex.getMessage().contains("not a valid location"));
  }

  // ---------- Creation — max warehouses per location ----------

  @Test
  public void testCreate_maxWarehousesReached_fails() {
    Warehouse existing = warehouse("EXISTING-001", "ZWOLLE-001", 20, 5);
    Warehouse w = warehouse("NEW-001", "ZWOLLE-001", 10, 0);
    Location loc = location("ZWOLLE-001", 1, 80); // max 1 warehouse

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForCreation(w, null, loc, List.of(existing)));
    assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
  }

  // ---------- Creation — capacity exceeded ----------

  @Test
  public void testCreate_capacityExceedsRemaining_fails() {
    Warehouse existing = warehouse("EXISTING-001", "ZWOLLE-001", 70, 5);
    Warehouse w = warehouse("NEW-001", "ZWOLLE-001", 50, 0); // only 10 remaining
    Location loc = location("ZWOLLE-001", 2, 80);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForCreation(w, null, loc, List.of(existing)));
    assertTrue(ex.getMessage().contains("exceeds the remaining capacity"));
  }

  // ---------- Creation — stock > capacity ----------

  @Test
  public void testCreate_stockExceedsCapacity_fails() {
    Warehouse w = warehouse("NEW-001", "ZWOLLE-001", 20, 50);
    Location loc = location("ZWOLLE-001", 2, 200);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForCreation(w, null, loc, List.of()));
    assertTrue(ex.getMessage().contains("stock"));
    assertTrue(ex.getMessage().contains("capacity"));
  }

  // ---------- Replacement — happy path ----------

  @Test
  public void testReplace_happyPath_passes() {
    Warehouse existing = warehouse("WH-001", "AMSTERDAM-001", 50, 10);
    Warehouse newWh = warehouse("WH-001", "AMSTERDAM-001", 60, 10);
    Location loc = location("AMSTERDAM-001", 5, 200);

    assertDoesNotThrow(() ->
        validator.validateForReplacement(newWh, existing, loc, List.of(existing)));
  }

  // ---------- Replacement — invalid location ----------

  @Test
  public void testReplace_invalidLocation_fails() {
    Warehouse existing = warehouse("WH-001", "AMSTERDAM-001", 50, 10);
    Warehouse newWh = warehouse("WH-001", "INVALID-LOC", 60, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForReplacement(newWh, existing, null, List.of(existing)));
    assertTrue(ex.getMessage().contains("not a valid location"));
  }

  // ---------- Replacement — capacity cannot hold existing stock ----------

  @Test
  public void testReplace_capacityTooSmallForExistingStock_fails() {
    Warehouse existing = warehouse("WH-001", "AMSTERDAM-001", 50, 30);
    Warehouse newWh = warehouse("WH-001", "AMSTERDAM-001", 20, 30); // capacity < existing.stock (30)
    Location loc = location("AMSTERDAM-001", 5, 200);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForReplacement(newWh, existing, loc, List.of(existing)));
    assertTrue(ex.getMessage().contains("cannot accommodate the stock"));
  }

  // ---------- Replacement — stock mismatch ----------

  @Test
  public void testReplace_stockMismatch_fails() {
    Warehouse existing = warehouse("WH-001", "AMSTERDAM-001", 50, 10);
    Warehouse newWh = warehouse("WH-001", "AMSTERDAM-001", 60, 25); // stock != existing.stock
    Location loc = location("AMSTERDAM-001", 5, 200);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForReplacement(newWh, existing, loc, List.of(existing)));
    assertTrue(ex.getMessage().contains("must match the stock"));
  }

  // ---------- Replacement — remaining location capacity exceeded ----------

  @Test
  public void testReplace_capacityExceedsRemainingAtLocation_fails() {
    Warehouse existing = warehouse("WH-001", "AMSTERDAM-001", 50, 10);
    Warehouse other = warehouse("WH-002", "AMSTERDAM-001", 60, 5); // other already uses 60
    Warehouse newWh = warehouse("WH-001", "AMSTERDAM-001", 90, 10); // 60+90 = 150 > maxCapacity 100
    Location loc = location("AMSTERDAM-001", 5, 100);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForReplacement(newWh, existing, loc, List.of(existing, other)));
    assertTrue(ex.getMessage().contains("exceeds the remaining capacity"));
  }

  // ---------- Replacement — stock > new capacity ----------

  @Test
  public void testReplace_stockExceedsNewCapacity_fails() {
    Warehouse existing = warehouse("WH-001", "AMSTERDAM-001", 50, 30);
    Warehouse newWh = warehouse("WH-001", "AMSTERDAM-001", 20, 30); // stock(30) > capacity(20)
    Location loc = location("AMSTERDAM-001", 5, 200);

    // capacity < existing.stock is caught first, but let's test the combined scenario
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> validator.validateForReplacement(newWh, existing, loc, List.of(existing)));
    assertTrue(ex.getMessage() != null);
  }
}
