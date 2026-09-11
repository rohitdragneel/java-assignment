package com.fulfilment.application.monolith.warehouses.domain.validator;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public class WarehouseValidator {

  public void validateForCreation(
      Warehouse warehouse,
      Warehouse existingByCode,
      Location location,
      List<Warehouse> activeAtLocation) {

    // 1. Business Unit Code must be unique among active warehouses
    if (existingByCode != null) {
      throw new IllegalArgumentException(
          "A warehouse with business unit code '"
              + warehouse.businessUnitCode
              + "' already exists.");
    }

    // 2. Location must be a valid, known location
    if (location == null) {
      throw new IllegalArgumentException(
          "Location '" + warehouse.location + "' is not a valid location.");
    }

    // 3. Maximum number of warehouses for this location must not be exceeded
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ") has been reached for location '"
              + warehouse.location
              + "'.");
    }

    // 4. The new warehouse capacity must not exceed the remaining location capacity
    int usedCapacity = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
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

    // 5. Stock must not exceed the warehouse's own capacity
    validateStockWithinCapacity(warehouse);
  }

  public void validateForReplacement(
      Warehouse newWarehouse,
      Warehouse existing,
      Location location,
      List<Warehouse> activeAtLocation) {

    // 1. Location must be valid
    if (location == null) {
      throw new IllegalArgumentException(
          "Location '" + newWarehouse.location + "' is not a valid location.");
    }

    // 2. New capacity must accommodate the existing stock
    if (newWarehouse.capacity < existing.stock) {
      throw new IllegalArgumentException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the stock ("
              + existing.stock
              + ") from the warehouse being replaced.");
    }

    // 3. Stock of the new warehouse must match the stock of the warehouse being replaced
    if (!newWarehouse.stock.equals(existing.stock)) {
      throw new IllegalArgumentException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock ("
              + existing.stock
              + ") of the warehouse being replaced.");
    }

    // 4. Remaining location capacity (excluding the warehouse being archived) must not be exceeded
    int usedCapacity =
        activeAtLocation.stream()
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

    // 5. Stock must not exceed the new warehouse's own capacity
    validateStockWithinCapacity(newWarehouse);
  }

  // ---------- Shared rule ----------

  private void validateStockWithinCapacity(Warehouse warehouse) {
    if (warehouse.stock != null && warehouse.capacity != null && warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException(
          "Warehouse stock ("
              + warehouse.stock
              + ") exceeds its capacity ("
              + warehouse.capacity
              + ").");
    }
  }
}
