package com.fulfilment.application.monolith.fulfillment.domain.ports.outbound;

public interface StoreResolver {
  boolean existsById(Long storeId);
}
