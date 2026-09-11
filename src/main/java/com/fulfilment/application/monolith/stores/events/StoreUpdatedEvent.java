package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.Store;


public class StoreUpdatedEvent {

  public final Store store;

  public StoreUpdatedEvent(Store store) {
    this.store = store;
  }
}
