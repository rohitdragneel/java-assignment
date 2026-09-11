package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.stores.events.StoreCreatedEvent;
import com.fulfilment.application.monolith.stores.events.StoreUpdatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;


@ApplicationScoped
public class StoreService {

  @Inject Event<StoreCreatedEvent> storeCreatedEvent;
  @Inject Event<StoreUpdatedEvent> storeUpdatedEvent;

  @Transactional
  public Store create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    store.persist();
    // Fire event inside the transaction — observer notifies legacy system AFTER_SUCCESS
    storeCreatedEvent.fire(new StoreCreatedEvent(store));
    return store;
  }

  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    // Fire event inside the transaction — observer notifies legacy system AFTER_SUCCESS
    storeUpdatedEvent.fire(new StoreUpdatedEvent(entity));
    return entity;
  }

  @Transactional
  public Store patch(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    if (updatedStore.name != null) {
      entity.name = updatedStore.name;
    }

    if (updatedStore.quantityProductsInStock != 0) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }
    // Fire event inside the transaction — observer notifies legacy system AFTER_SUCCESS
    storeUpdatedEvent.fire(new StoreUpdatedEvent(entity));
    return entity;
  }
}
