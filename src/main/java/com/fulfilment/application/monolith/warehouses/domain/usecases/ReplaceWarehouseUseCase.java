package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // 1. Find the existing active warehouse by business unit code — must exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '"
              + newWarehouse.businessUnitCode
              + "' does not exist or is already archived.");
    }

    // 2. Validate the new warehouse's location
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Location '" + newWarehouse.location + "' is not a valid location.");
    }

    // 3. Capacity accommodation — new warehouse must be able to hold the old warehouse's stock
    if (newWarehouse.capacity < existing.stock) {
      throw new IllegalArgumentException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the stock ("
              + existing.stock
              + ") from the warehouse being replaced.");
    }

    // 4. Stock matching — new warehouse stock must equal old warehouse stock
    if (!newWarehouse.stock.equals(existing.stock)) {
      throw new IllegalArgumentException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock ("
              + existing.stock
              + ") of the warehouse being replaced.");
    }

    // 5. Check capacity does not exceed the remaining location capacity
    //    (exclude the existing warehouse since it's being archived)
    List<Warehouse> warehousesAtLocation =
        warehouseStore.findAllActiveByLocation(newWarehouse.location);
    int usedCapacity =
        warehousesAtLocation.stream()
            .filter(w -> !w.businessUnitCode.equals(existing.businessUnitCode))
            .mapToInt(w -> w.capacity)
            .sum();
    int remainingCapacity = location.maxCapacity - usedCapacity;
    if (newWarehouse.capacity > remainingCapacity) {
      throw new IllegalArgumentException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") exceeds the remaining capacity ("
              + remainingCapacity
              + ") at location '"
              + newWarehouse.location
              + "'.");
    }

    // 6. Stock must not exceed capacity
    if (newWarehouse.stock > newWarehouse.capacity) {
      throw new IllegalArgumentException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") exceeds its capacity ("
              + newWarehouse.capacity
              + ").");
    }

    // All validations passed — archive the old warehouse and create the new one
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
    warehouseStore.create(newWarehouse);
  }
}
