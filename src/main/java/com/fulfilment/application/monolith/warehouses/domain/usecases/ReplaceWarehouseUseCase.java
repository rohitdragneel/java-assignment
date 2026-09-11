package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidator validator;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.validator = new WarehouseValidator();
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    // 1. The existing active warehouse must exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '"
              + newWarehouse.businessUnitCode
              + "' does not exist or is already archived.");
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    List<Warehouse> activeAtLocation =
        warehouseStore.findAllActiveByLocation(newWarehouse.location);

    // All validation rules are enforced by the dedicated validator
    validator.validateForReplacement(newWarehouse, existing, location, activeAtLocation);

    // All validations passed — archive the old warehouse and create the new one
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
    warehouseStore.create(newWarehouse);
  }
}
