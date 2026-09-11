package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;


@ApplicationScoped
public class StoreEventObserver {

  private static final Logger LOGGER = Logger.getLogger(StoreEventObserver.class.getName());

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  /**
   * Fired after a new store has been persisted and the transaction committed.
   */
  public void onStoreCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    LOGGER.infof("Transaction committed — notifying legacy system of new store: %s", event.store.name);
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.store);
  }

  /**
   * Fired after an existing store has been updated and the transaction committed.
   */
  public void onStoreUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    LOGGER.infof("Transaction committed — notifying legacy system of updated store: %s", event.store.name);
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store);
  }
}
