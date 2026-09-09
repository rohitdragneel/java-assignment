package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<FulfillmentAssignment> {

  /** Find all distinct warehouse IDs fulfilling a specific product for a specific store. */
  public List<Long> findDistinctWarehousesByStoreAndProduct(Long storeId, Long productId) {
    return find(
            "select distinct f.warehouseId from FulfillmentAssignment f where f.storeId = ?1 and f.productId = ?2",
            storeId,
            productId)
        .project(Long.class)
        .list();
  }

  /** Find all distinct warehouse IDs fulfilling any product for a specific store. */
  public List<Long> findDistinctWarehousesByStore(Long storeId) {
    return find(
            "select distinct f.warehouseId from FulfillmentAssignment f where f.storeId = ?1",
            storeId)
        .project(Long.class)
        .list();
  }

  /** Find all distinct product IDs stored/fulfilled by a specific warehouse across all stores. */
  public List<Long> findDistinctProductsByWarehouse(Long warehouseId) {
    return find(
            "select distinct f.productId from FulfillmentAssignment f where f.warehouseId = ?1",
            warehouseId)
        .project(Long.class)
        .list();
  }

  /** Find specific assignment. */
  public Optional<FulfillmentAssignment> findByStoreProductWarehouse(
      Long storeId, Long productId, Long warehouseId) {
    return find(
            "storeId = ?1 and productId = ?2 and warehouseId = ?3", storeId, productId, warehouseId)
        .firstResultOptional();
  }

  /** Filter assignments by optional storeId, productId, warehouseId. */
  public List<FulfillmentAssignment> listWithFilters(
      Long storeId, Long productId, Long warehouseId) {
    StringBuilder query = new StringBuilder("1=1");
    Map<String, Object> params = new HashMap<>();

    if (storeId != null) {
      query.append(" and storeId = :storeId");
      params.put("storeId", storeId);
    }
    if (productId != null) {
      query.append(" and productId = :productId");
      params.put("productId", productId);
    }
    if (warehouseId != null) {
      query.append(" and warehouseId = :warehouseId");
      params.put("warehouseId", warehouseId);
    }

    return list(query.toString(), params);
  }
}
