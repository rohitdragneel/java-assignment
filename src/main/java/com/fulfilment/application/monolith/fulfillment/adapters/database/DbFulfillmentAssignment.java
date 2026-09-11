package com.fulfilment.application.monolith.fulfillment.adapters.database;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;


@Entity
@Table(
    name = "fulfillment_assignment",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_store_product_warehouse",
          columnNames = {"storeId", "productId", "warehouseId"})
    })
@Cacheable
public class DbFulfillmentAssignment {

  @Id @GeneratedValue public Long id;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public Long warehouseId;

  public LocalDateTime createdAt;

  public DbFulfillmentAssignment() {}
}
