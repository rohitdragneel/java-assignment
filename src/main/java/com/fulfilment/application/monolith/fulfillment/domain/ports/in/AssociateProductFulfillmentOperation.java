package com.fulfilment.application.monolith.fulfillment.domain.ports.in;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;


public interface AssociateProductFulfillmentOperation {
  FulfillmentAssignment associate(Long storeId, Long productId, Long warehouseId);
}
