package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validator.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidator validator;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.validator = new WarehouseValidator();
  }

  @Override
  public void create(Warehouse warehouse) {
    Warehouse existingByCode = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    List<Warehouse> activeAtLocation = warehouseStore.findAllActiveByLocation(warehouse.location);

    // All validation rules are enforced by the dedicated validator
    validator.validateForCreation(warehouse, existingByCode, location, activeAtLocation);

    // All validations passed — persist the warehouse
    warehouseStore.create(warehouse);
  }
}
