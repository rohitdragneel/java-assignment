package com.fulfilment.application.monolith.fulfillment.event;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;

public class ProductFulfillmentAssociatedEvent {

  public final FulfillmentAssignment assignment;

  public ProductFulfillmentAssociatedEvent(FulfillmentAssignment assignment) {
    this.assignment = assignment;
  }
}
