package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link ArchiveWarehouseUseCase} using hand-rolled stubs.
 * No Quarkus runtime, no Mockito.
 */
public class ArchiveWarehouseUseCaseTest {

  static class StubWarehouseStore implements WarehouseStore {
    final Map<String, Warehouse> store = new HashMap<>();
    Warehouse updatedWarehouse;

    @Override public List<Warehouse> getAll() { return new ArrayList<>(store.values()); }
    @Override public void create(Warehouse w) { store.put(w.businessUnitCode, w); }
    @Override public void update(Warehouse w) { updatedWarehouse = w; store.put(w.businessUnitCode, w); }
    @Override public void remove(Warehouse w) { store.remove(w.businessUnitCode); }
    @Override public Warehouse findByBusinessUnitCode(String code) { return store.get(code); }
    @Override public List<Warehouse> findAllActiveByLocation(String loc) {
      return store.values().stream().filter(w -> loc.equals(w.location)).toList();
    }
  }

  private StubWarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach void setUp() {
    warehouseStore = new StubWarehouseStore();
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test public void testArchive_success() {
    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "WH-001";
    existing.stock = 10;
    warehouseStore.store.put("WH-001", existing);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "WH-001";
    useCase.archive(request);

    assertNotNull(warehouseStore.updatedWarehouse, "update should be called");
    assertNotNull(existing.archivedAt, "archivedAt should be set");
  }

  @Test public void testArchive_notFound_fails() {
    Warehouse request = new Warehouse();
    request.businessUnitCode = "NONEXIST";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.archive(request));
    assertTrue(ex.getMessage().contains("does not exist or is already archived"));
    assertTrue(warehouseStore.updatedWarehouse == null);
  }
}
