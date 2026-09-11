package com.fulfilment.application.monolith.fulfillment.domain.ports.outbound;

public interface WarehouseResolver {
  boolean isActiveById(Long warehouseId);
}
