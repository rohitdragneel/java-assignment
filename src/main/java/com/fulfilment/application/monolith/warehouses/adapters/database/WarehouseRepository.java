package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return find("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = LocalDateTime.now();
    entity.archivedAt = null;
    persist(entity);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
            .firstResult();
    if (entity != null) {
      entity.location = warehouse.location;
      entity.capacity = warehouse.capacity;
      entity.stock = warehouse.stock;
      entity.archivedAt = warehouse.archivedAt;
    }
  }

  @Override
  public void remove(Warehouse warehouse) {
    delete("businessUnitCode", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    if (entity == null) {
      try {
        long id = Long.parseLong(buCode);
        entity = find("id = ?1 and archivedAt is null", id).firstResult();
      } catch (NumberFormatException ignored) {}
    }
    if (entity == null) {
      return null;
    }
    return entity.toWarehouse();
  }

  @Override
  public List<Warehouse> findAllActiveByLocation(String location) {
    return find("location = ?1 and archivedAt is null", location).stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
}
