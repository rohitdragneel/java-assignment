package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class AssociateProductFulfillmentUseCase {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject com.fulfilment.application.monolith.products.ProductRepository productRepository;
  @Inject com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository warehouseRepository;

  @Transactional
  public FulfillmentAssignment associate(Long storeId, Long productId, Long warehouseId) {
    if (storeId == null || productId == null || warehouseId == null) {
      throw new IllegalArgumentException("storeId, productId, and warehouseId must not be null");
    }

    // 1. Verify Store exists
    Store store = Store.findById(storeId);
    if (store == null) {
      throw new NoSuchElementException("Store not found with id: " + storeId);
    }

    // 2. Verify Product exists
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new NoSuchElementException("Product not found with id: " + productId);
    }

    // 3. Verify Warehouse exists and is active
    DbWarehouse warehouse = warehouseRepository.findById(warehouseId);
    if (warehouse == null) {
      throw new NoSuchElementException("Warehouse not found with id: " + warehouseId);
    }
    if (warehouse.archivedAt != null) {
      throw new IllegalStateException("Warehouse is archived with id: " + warehouseId);
    }

    // 4. Verify association does not already exist
    if (fulfillmentRepository
        .findByStoreProductWarehouse(storeId, productId, warehouseId)
        .isPresent()) {
      throw new IllegalStateException(
          String.format(
              "Fulfillment assignment already exists for store %d, product %d, warehouse %d",
              storeId, productId, warehouseId));
    }

    // 5. Constraint 1: Each Product can be fulfilled by a maximum of 2 different Warehouses per Store
    List<Long> currentWarehousesForProduct =
        fulfillmentRepository.findDistinctWarehousesByStoreAndProduct(storeId, productId);
    if (!currentWarehousesForProduct.contains(warehouseId)
        && currentWarehousesForProduct.size() >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new IllegalStateException(
          String.format(
              "Product %d already fulfilled by maximum of %d warehouses for Store %d",
              productId, MAX_WAREHOUSES_PER_PRODUCT_PER_STORE, storeId));
    }

    // 6. Constraint 2: Each Store can be fulfilled by a maximum of 3 different Warehouses
    List<Long> currentWarehousesForStore =
        fulfillmentRepository.findDistinctWarehousesByStore(storeId);
    if (!currentWarehousesForStore.contains(warehouseId)
        && currentWarehousesForStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new IllegalStateException(
          String.format(
              "Store %d already fulfilled by maximum of %d warehouses",
              storeId, MAX_WAREHOUSES_PER_STORE));
    }

    // 7. Constraint 3: Each Warehouse can store maximally 5 types of Products
    List<Long> currentProductsInWarehouse =
        fulfillmentRepository.findDistinctProductsByWarehouse(warehouseId);
    if (!currentProductsInWarehouse.contains(productId)
        && currentProductsInWarehouse.size() >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new IllegalStateException(
          String.format(
              "Warehouse %d already stores maximum of %d types of products",
              warehouseId, MAX_PRODUCT_TYPES_PER_WAREHOUSE));
    }

    // 8. Persist assignment
    FulfillmentAssignment assignment = new FulfillmentAssignment(storeId, productId, warehouseId);
    fulfillmentRepository.persist(assignment);
    return assignment;
  }
}
