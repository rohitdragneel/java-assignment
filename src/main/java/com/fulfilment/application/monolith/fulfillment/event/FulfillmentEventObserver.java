package com.fulfilment.application.monolith.fulfillment.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FulfillmentEventObserver {

  private static final Logger LOGGER = Logger.getLogger(FulfillmentEventObserver.class.getName());

  public void onProductFulfillmentAssociated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS)
          ProductFulfillmentAssociatedEvent event) {
    LOGGER.infof(
        "Fulfillment assignment committed — store=%d, product=%d, warehouse=%d",
        event.assignment.storeId,
        event.assignment.productId,
        event.assignment.warehouseId);
  }
}
