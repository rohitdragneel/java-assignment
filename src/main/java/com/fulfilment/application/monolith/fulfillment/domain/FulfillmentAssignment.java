package com.fulfilment.application.monolith.fulfillment.domain;

import java.time.LocalDateTime;


public class FulfillmentAssignment {

  public Long id;

  public Long storeId;

  public Long productId;

  public Long warehouseId;

  public LocalDateTime createdAt;

  public FulfillmentAssignment() {}

  public FulfillmentAssignment(Long storeId, Long productId, Long warehouseId) {
    this.storeId = storeId;
    this.productId = productId;
    this.warehouseId = warehouseId;
    this.createdAt = LocalDateTime.now();
  }
}
