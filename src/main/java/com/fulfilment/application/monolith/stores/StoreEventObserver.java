package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;


@ApplicationScoped
public class StoreEventObserver {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.getStore());
  }

  public void onStoreUpdated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.getStore());
  }
}
