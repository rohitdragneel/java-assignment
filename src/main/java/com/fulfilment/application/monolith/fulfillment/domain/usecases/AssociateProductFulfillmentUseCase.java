package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.inbound.AssociateProductFulfillmentOperation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.ProductResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.StoreResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.WarehouseResolver;
import com.fulfilment.application.monolith.fulfillment.domain.validator.FulfillmentValidator;
import com.fulfilment.application.monolith.fulfillment.event.ProductFulfillmentAssociatedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;


@ApplicationScoped
public class AssociateProductFulfillmentUseCase implements AssociateProductFulfillmentOperation {

  @Inject FulfillmentStore fulfillmentStore;
  @Inject StoreResolver storeResolver;
  @Inject ProductResolver productResolver;
  @Inject WarehouseResolver warehouseResolver;
  @Inject Event<ProductFulfillmentAssociatedEvent> fulfillmentEvent;

  private final FulfillmentValidator validator = new FulfillmentValidator();

  @Override
  @Transactional
  public FulfillmentAssignment associate(Long storeId, Long productId, Long warehouseId) {
    if (storeId == null || productId == null || warehouseId == null) {
      throw new IllegalArgumentException("storeId, productId, and warehouseId must not be null");
    }

    // 1. Verify entities exist
    if (!storeResolver.existsById(storeId)) {
      throw new NoSuchElementException("Store not found with id: " + storeId);
    }
    if (!productResolver.existsById(productId)) {
      throw new NoSuchElementException("Product not found with id: " + productId);
    }
    warehouseResolver.isActiveById(warehouseId); // throws NoSuchElement / IllegalState if invalid

    // 2. Prevent duplicate assignment
    if (fulfillmentStore.findByStoreProductWarehouse(storeId, productId, warehouseId).isPresent()) {
      throw new IllegalStateException(String.format(
          "Fulfillment assignment already exists for store %d, product %d, warehouse %d",
          storeId, productId, warehouseId));
    }

    // 3. Validate the three fulfillment constraints
    List<Long> warehousesForProduct =
        fulfillmentStore.findDistinctWarehousesByStoreAndProduct(storeId, productId);
    List<Long> warehousesForStore =
        fulfillmentStore.findDistinctWarehousesByStore(storeId);
    List<Long> productsInWarehouse =
        fulfillmentStore.findDistinctProductsByWarehouse(warehouseId);

    validator.validateConstraints(
        storeId, productId, warehouseId,
        warehousesForProduct, warehousesForStore, productsInWarehouse);

    // 4. Persist
    FulfillmentAssignment assignment = new FulfillmentAssignment(storeId, productId, warehouseId);
    FulfillmentAssignment saved = fulfillmentStore.save(assignment);

    // 5. Fire event (observer notifies downstream AFTER transaction commits)
    fulfillmentEvent.fire(new ProductFulfillmentAssociatedEvent(saved));

    return saved;
  }
}
