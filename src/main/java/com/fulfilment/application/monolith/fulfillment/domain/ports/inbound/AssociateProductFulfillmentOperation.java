package com.fulfilment.application.monolith.fulfillment.domain.ports.inbound;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;


public interface AssociateProductFulfillmentOperation {
  FulfillmentAssignment associate(Long storeId, Long productId, Long warehouseId);
}
