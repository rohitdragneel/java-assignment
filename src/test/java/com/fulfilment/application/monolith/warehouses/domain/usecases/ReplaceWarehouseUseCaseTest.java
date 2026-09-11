package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link ReplaceWarehouseUseCase} using hand-rolled stubs.
 * No Quarkus runtime, no Mockito.
 */
public class ReplaceWarehouseUseCaseTest {

  static class StubWarehouseStore implements WarehouseStore {
    final Map<String, Warehouse> store = new HashMap<>();
    Warehouse updatedWarehouse;
    Warehouse createdWarehouse;

    @Override public List<Warehouse> getAll() { return new ArrayList<>(store.values()); }
    @Override public void create(Warehouse w) { createdWarehouse = w; store.put(w.businessUnitCode + "_new", w); }
    @Override public void update(Warehouse w) { updatedWarehouse = w; store.put(w.businessUnitCode, w); }
    @Override public void remove(Warehouse w) { store.remove(w.businessUnitCode); }
    @Override public Warehouse findByBusinessUnitCode(String code) { return store.get(code); }
    @Override public List<Warehouse> findAllActiveByLocation(String loc) {
      return store.values().stream().filter(w -> loc.equals(w.location)).toList();
    }
  }

  static class StubLocationResolver implements LocationResolver {
    final Map<String, Location> locations = new HashMap<>();
    @Override public Location resolveByIdentifier(String id) { return locations.get(id); }
    void add(Location l) { locations.put(l.identification, l); }
  }

  private StubWarehouseStore warehouseStore;
  private StubLocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  private static Warehouse wh(String code, String loc, int cap, int stock) {
    Warehouse w = new Warehouse(); w.businessUnitCode = code; w.location = loc;
    w.capacity = cap; w.stock = stock; return w;
  }

  @BeforeEach void setUp() {
    warehouseStore = new StubWarehouseStore();
    locationResolver = new StubLocationResolver();
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test public void testReplace_success() {
    locationResolver.add(new Location("AMSTERDAM-001", 5, 200));
    warehouseStore.store.put("WH-001", wh("WH-001", "AMSTERDAM-001", 50, 10));
    useCase.replace(wh("WH-001", "AMSTERDAM-001", 60, 10));
    assertTrue(warehouseStore.updatedWarehouse != null);
    assertTrue(warehouseStore.createdWarehouse != null);
  }

  @Test public void testReplace_noExistingWarehouse_fails() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.replace(wh("WH-NONEXIST", "AMSTERDAM-001", 60, 10)));
    assertTrue(ex.getMessage().contains("does not exist or is already archived"));
    assertTrue(warehouseStore.updatedWarehouse == null);
  }

  @Test public void testReplace_invalidLocation_fails() {
    warehouseStore.store.put("WH-001", wh("WH-001", "AMSTERDAM-001", 50, 10));
    // location resolver has no entries → null
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.replace(wh("WH-001", "INVALID-LOC", 60, 10)));
    assertTrue(ex.getMessage().contains("not a valid location"));
    assertTrue(warehouseStore.updatedWarehouse == null);
  }

  @Test public void testReplace_stockMismatch_fails() {
    locationResolver.add(new Location("AMSTERDAM-001", 5, 200));
    warehouseStore.store.put("WH-001", wh("WH-001", "AMSTERDAM-001", 50, 10));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.replace(wh("WH-001", "AMSTERDAM-001", 60, 25)));
    assertTrue(ex.getMessage().contains("must match the stock"));
  }

  @Test public void testReplace_capacityTooSmallForStock_fails() {
    locationResolver.add(new Location("AMSTERDAM-001", 5, 200));
    warehouseStore.store.put("WH-001", wh("WH-001", "AMSTERDAM-001", 50, 30));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.replace(wh("WH-001", "AMSTERDAM-001", 20, 30)));
    assertTrue(ex.getMessage().contains("cannot accommodate the stock"));
  }

  @Test public void testReplace_exceedsLocationCapacity_fails() {
    locationResolver.add(new Location("AMSTERDAM-001", 5, 100));
    warehouseStore.store.put("WH-001", wh("WH-001", "AMSTERDAM-001", 50, 10));
    warehouseStore.store.put("WH-002", wh("WH-002", "AMSTERDAM-001", 80, 5));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.replace(wh("WH-001", "AMSTERDAM-001", 90, 10)));
    assertTrue(ex.getMessage().contains("exceeds the remaining capacity"));
  }
}
