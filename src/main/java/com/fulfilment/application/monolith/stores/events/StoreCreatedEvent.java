package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.Store;

public class StoreCreatedEvent {

  public final Store store;

  public StoreCreatedEvent(Store store) {
    this.store = store;
  }
}
