package com.fulfilment.application.monolith.fulfillment.domain.ports.outbound;

public interface ProductResolver {
  boolean existsById(Long productId);
}
