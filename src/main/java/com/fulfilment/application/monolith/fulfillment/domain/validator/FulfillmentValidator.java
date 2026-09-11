package com.fulfilment.application.monolith.fulfillment.domain.validator;

import java.util.List;


public class FulfillmentValidator {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;


  public void validateConstraints(
      Long storeId,
      Long productId,
      Long warehouseId,
      List<Long> warehousesForProductInStore,
      List<Long> warehousesForStore,
      List<Long> productsInWarehouse) {

    // Constraint 1: Each Product can be fulfilled by at most 2 Warehouses per Store
    if (!warehousesForProductInStore.contains(warehouseId)
        && warehousesForProductInStore.size() >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new IllegalStateException(
          String.format(
              "Product %d already fulfilled by maximum of %d warehouses for Store %d",
              productId, MAX_WAREHOUSES_PER_PRODUCT_PER_STORE, storeId));
    }

    // Constraint 2: Each Store can be fulfilled by at most 3 Warehouses
    if (!warehousesForStore.contains(warehouseId)
        && warehousesForStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new IllegalStateException(
          String.format(
              "Store %d already fulfilled by maximum of %d warehouses",
              storeId, MAX_WAREHOUSES_PER_STORE));
    }

    // Constraint 3: Each Warehouse can store at most 5 product types
    if (!productsInWarehouse.contains(productId)
        && productsInWarehouse.size() >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new IllegalStateException(
          String.format(
              "Warehouse %d already stores maximum of %d types of products",
              warehouseId, MAX_PRODUCT_TYPES_PER_WAREHOUSE));
    }
  }
}
