package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.FulfillmentStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class FulfillmentRepository implements FulfillmentStore, PanacheRepository<DbFulfillmentAssignment> {

  @Override
  public FulfillmentAssignment save(FulfillmentAssignment model) {
    DbFulfillmentAssignment entity = toEntity(model);
    persist(entity);
    return toModel(entity);
  }

  @Override
  public Optional<FulfillmentAssignment> findByStoreProductWarehouse(
      Long storeId, Long productId, Long warehouseId) {
    return find("storeId = ?1 and productId = ?2 and warehouseId = ?3", storeId, productId, warehouseId)
        .firstResultOptional()
        .map(this::toModel);
  }

  @Override
  public List<Long> findDistinctWarehousesByStoreAndProduct(Long storeId, Long productId) {
    return find(
            "select distinct f.warehouseId from DbFulfillmentAssignment f "
                + "where f.storeId = ?1 and f.productId = ?2",
            storeId, productId)
        .project(Long.class)
        .list();
  }

  @Override
  public List<Long> findDistinctWarehousesByStore(Long storeId) {
    return find(
            "select distinct f.warehouseId from DbFulfillmentAssignment f where f.storeId = ?1",
            storeId)
        .project(Long.class)
        .list();
  }

  @Override
  public List<Long> findDistinctProductsByWarehouse(Long warehouseId) {
    return find(
            "select distinct f.productId from DbFulfillmentAssignment f where f.warehouseId = ?1",
            warehouseId)
        .project(Long.class)
        .list();
  }

  @Override
  public List<FulfillmentAssignment> listWithFilters(Long storeId, Long productId, Long warehouseId) {
    StringBuilder query = new StringBuilder("1=1");
    Map<String, Object> params = new HashMap<>();
    if (storeId != null)     { query.append(" and storeId = :storeId");     params.put("storeId", storeId); }
    if (productId != null)   { query.append(" and productId = :productId"); params.put("productId", productId); }
    if (warehouseId != null) { query.append(" and warehouseId = :warehouseId"); params.put("warehouseId", warehouseId); }
    return list(query.toString(), params).stream().map(this::toModel).toList();
  }

  @Override
  public boolean deleteById(Long id) {
    return delete("id", id) > 0;
  }

  /** Convenience method for test setup — deletes all assignments. */
  public void deleteAllAssignments() {
    deleteAll();
  }

  // ---------- Mapping ----------

  private DbFulfillmentAssignment toEntity(FulfillmentAssignment m) {
    DbFulfillmentAssignment e = new DbFulfillmentAssignment();
    e.storeId = m.storeId;
    e.productId = m.productId;
    e.warehouseId = m.warehouseId;
    e.createdAt = m.createdAt;
    return e;
  }

  FulfillmentAssignment toModel(DbFulfillmentAssignment e) {
    FulfillmentAssignment m = new FulfillmentAssignment();
    m.id = e.id;
    m.storeId = e.storeId;
    m.productId = e.productId;
    m.warehouseId = e.warehouseId;
    m.createdAt = e.createdAt;
    return m;
  }
}
