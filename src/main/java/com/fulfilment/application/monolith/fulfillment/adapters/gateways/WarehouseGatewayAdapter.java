package com.fulfilment.application.monolith.fulfillment.adapters.gateways;

import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.WarehouseResolver;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.NoSuchElementException;


@ApplicationScoped
public class WarehouseGatewayAdapter implements WarehouseResolver {

  @Inject WarehouseRepository warehouseRepository;

  @Override
  public boolean isActiveById(Long warehouseId) {
    DbWarehouse warehouse = warehouseRepository.findById(warehouseId);
    if (warehouse == null) {
      throw new NoSuchElementException("Warehouse not found with id: " + warehouseId);
    }
    if (warehouse.archivedAt != null) {
      throw new IllegalStateException("Warehouse is archived with id: " + warehouseId);
    }
    return true;
  }
}
