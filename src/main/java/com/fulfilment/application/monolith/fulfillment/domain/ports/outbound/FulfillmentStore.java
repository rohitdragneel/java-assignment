package com.fulfilment.application.monolith.fulfillment.domain.ports.outbound;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import java.util.List;
import java.util.Optional;

public interface FulfillmentStore {

  FulfillmentAssignment save(FulfillmentAssignment assignment);

  Optional<FulfillmentAssignment> findByStoreProductWarehouse(Long storeId, Long productId, Long warehouseId);

  List<Long> findDistinctWarehousesByStoreAndProduct(Long storeId, Long productId);

  List<Long> findDistinctWarehousesByStore(Long storeId);

  List<Long> findDistinctProductsByWarehouse(Long warehouseId);

  List<FulfillmentAssignment> listWithFilters(Long storeId, Long productId, Long warehouseId);

  boolean deleteById(Long id);
}
