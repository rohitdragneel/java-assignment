package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    // 1. Business Unit Code must be unique — no active warehouse with this code should exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      throw new IllegalArgumentException(
          "A warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' already exists.");
    }

    // 2. Location must be valid
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Location '" + warehouse.location + "' is not a valid location.");
    }

    // 3. Check maximum number of warehouses at this location
    List<Warehouse> warehousesAtLocation =
        warehouseStore.findAllActiveByLocation(warehouse.location);
    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ") has been reached for location '"
              + warehouse.location
              + "'.");
    }

    // 4. Capacity must not exceed the remaining capacity at the location
    int usedCapacity = warehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();
    int remainingCapacity = location.maxCapacity - usedCapacity;
    if (warehouse.capacity > remainingCapacity) {
      throw new IllegalArgumentException(
          "Warehouse capacity ("
              + warehouse.capacity
              + ") exceeds the remaining capacity ("
              + remainingCapacity
              + ") at location '"
              + warehouse.location
              + "'.");
    }

    // 5. Stock must not exceed the warehouse capacity
    if (warehouse.stock != null && warehouse.capacity != null && warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException(
          "Warehouse stock ("
              + warehouse.stock
              + ") exceeds its capacity ("
              + warehouse.capacity
              + ").");
    }

    // All validations passed — create the warehouse
    warehouseStore.create(warehouse);
  }
}
