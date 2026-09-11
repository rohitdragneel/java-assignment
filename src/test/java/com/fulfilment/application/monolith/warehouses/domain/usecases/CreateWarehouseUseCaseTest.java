package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
 * Pure unit tests for {@link CreateWarehouseUseCase} using hand-rolled stubs.
 * No Quarkus runtime, no Mockito.
 */
public class CreateWarehouseUseCaseTest {

  // ---------- Hand-rolled stubs ----------

  static class StubWarehouseStore implements WarehouseStore {
    final Map<String, Warehouse> store = new HashMap<>();
    Warehouse createdWarehouse;

    @Override public List<Warehouse> getAll() { return new ArrayList<>(store.values()); }
    @Override public void create(Warehouse w) { createdWarehouse = w; store.put(w.businessUnitCode, w); }
    @Override public void update(Warehouse w) { store.put(w.businessUnitCode, w); }
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
  private CreateWarehouseUseCase useCase;

  private static Warehouse wh(String code, String loc, int cap, int stock) {
    Warehouse w = new Warehouse(); w.businessUnitCode = code; w.location = loc;
    w.capacity = cap; w.stock = stock; return w;
  }

  @BeforeEach void setUp() {
    warehouseStore = new StubWarehouseStore();
    locationResolver = new StubLocationResolver();
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test public void testCreate_success() {
    locationResolver.add(new Location("ZWOLLE-001", 2, 80));
    assertDoesNotThrow(() -> useCase.create(wh("NEW-001", "ZWOLLE-001", 30, 10)));
    assertTrue(warehouseStore.createdWarehouse != null);
  }

  @Test public void testCreate_duplicateCode_fails() {
    locationResolver.add(new Location("ZWOLLE-001", 2, 80));
    warehouseStore.store.put("EXISTING", wh("EXISTING", "ZWOLLE-001", 20, 5));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(wh("EXISTING", "ZWOLLE-001", 30, 10)));
    assertTrue(ex.getMessage().contains("already exists"));
    assertTrue(warehouseStore.createdWarehouse == null);
  }

  @Test public void testCreate_invalidLocation_fails() {
    // no location added → resolver returns null
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(wh("NEW-001", "INVALID-LOC", 30, 10)));
    assertTrue(ex.getMessage().contains("not a valid location"));
  }

  @Test public void testCreate_maxWarehousesReached_fails() {
    locationResolver.add(new Location("ZWOLLE-001", 1, 80));
    warehouseStore.store.put("EXISTING", wh("EXISTING", "ZWOLLE-001", 20, 5));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(wh("NEW-001", "ZWOLLE-001", 10, 0)));
    assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
  }

  @Test public void testCreate_capacityExceedsRemaining_fails() {
    locationResolver.add(new Location("ZWOLLE-001", 2, 80));
    warehouseStore.store.put("EXISTING", wh("EXISTING", "ZWOLLE-001", 70, 5));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(wh("NEW-001", "ZWOLLE-001", 50, 0)));
    assertTrue(ex.getMessage().contains("exceeds the remaining capacity"));
  }

  @Test public void testCreate_stockExceedsCapacity_fails() {
    locationResolver.add(new Location("ZWOLLE-001", 2, 200));
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(wh("NEW-001", "ZWOLLE-001", 20, 50)));
    assertTrue(ex.getMessage().contains("stock"));
  }
}
