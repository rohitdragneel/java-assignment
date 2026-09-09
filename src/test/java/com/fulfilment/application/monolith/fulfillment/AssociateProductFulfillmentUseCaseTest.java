package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.domain.usecases.AssociateProductFulfillmentUseCase;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AssociateProductFulfillmentUseCaseTest {

  @Inject AssociateProductFulfillmentUseCase useCase;
  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject com.fulfilment.application.monolith.products.ProductRepository productRepository;
  @Inject com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  public void setUp() {
    fulfillmentRepository.deleteAll();
  }

  @Test
  public void testSuccessfulAssociation() {
    FulfillmentAssignment assignment = useCase.associate(1L, 1L, 1L);
    assertNotNull(assignment);
    assertNotNull(assignment.id);
    assertEquals(1L, assignment.storeId);
    assertEquals(1L, assignment.productId);
    assertEquals(1L, assignment.warehouseId);
  }

  @Test
  public void testStoreNotFound() {
    assertThrows(
        NoSuchElementException.class,
        () -> useCase.associate(9999L, 1L, 1L));
  }

  @Test
  public void testProductNotFound() {
    assertThrows(
        NoSuchElementException.class,
        () -> useCase.associate(1L, 9999L, 1L));
  }

  @Test
  public void testWarehouseNotFound() {
    assertThrows(
        NoSuchElementException.class,
        () -> useCase.associate(1L, 1L, 9999L));
  }

  @Test
  @Transactional
  public void testArchivedWarehouseFails() {
    // Create an archived warehouse
    DbWarehouse archived = new DbWarehouse();
    archived.businessUnitCode = "ARCH-001";
    archived.location = "ZWOLLE-001";
    archived.capacity = 100;
    archived.stock = 10;
    archived.createdAt = LocalDateTime.now();
    archived.archivedAt = LocalDateTime.now();
    warehouseRepository.persist(archived);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> useCase.associate(1L, 1L, archived.id));
    assertTrue(exception.getMessage().contains("archived"));
  }

  @Test
  public void testDuplicateAssociationFails() {
    useCase.associate(1L, 1L, 1L);
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> useCase.associate(1L, 1L, 1L));
    assertTrue(exception.getMessage().contains("already exists"));
  }

  @Test
  public void testConstraint1_Max2WarehousesPerProductPerStore() {
    // Warehouse 1 and Warehouse 2 fulfill Product 1 for Store 1
    useCase.associate(1L, 1L, 1L);
    useCase.associate(1L, 1L, 2L);

    // Attempting 3rd warehouse for (Store 1, Product 1) must fail
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> useCase.associate(1L, 1L, 3L));
    assertTrue(exception.getMessage().contains("already fulfilled by maximum of 2 warehouses"));
  }

  @Test
  @Transactional
  public void testConstraint2_Max3WarehousesPerStore() {
    // Create a 4th active warehouse
    DbWarehouse wh4 = new DbWarehouse();
    wh4.businessUnitCode = "MWH.004";
    wh4.location = "AMSTERDAM-001";
    wh4.capacity = 50;
    wh4.stock = 10;
    wh4.createdAt = LocalDateTime.now();
    warehouseRepository.persist(wh4);

    // Store 1 fulfilled by Warehouse 1 (Product 1), Warehouse 2 (Product 2), Warehouse 3 (Product 3)
    useCase.associate(1L, 1L, 1L);
    useCase.associate(1L, 2L, 2L);
    useCase.associate(1L, 3L, 3L);

    // Attempting a 4th distinct warehouse for Store 1 must fail
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> useCase.associate(1L, 1L, wh4.id));
    assertTrue(exception.getMessage().contains("already fulfilled by maximum of 3 warehouses"));
  }

  @Test
  @Transactional
  public void testConstraint3_Max5ProductsPerWarehouse() {
    // Create additional stores and products to test 5 product limit on Warehouse 1
    Product p4 = new Product("PROD-4");
    p4.stock = 5;
    productRepository.persist(p4);

    Product p5 = new Product("PROD-5");
    p5.stock = 5;
    productRepository.persist(p5);

    Product p6 = new Product("PROD-6");
    p6.stock = 5;
    productRepository.persist(p6);

    // Associate 5 distinct products to Warehouse 1 across stores
    useCase.associate(1L, 1L, 1L);
    useCase.associate(1L, 2L, 1L);
    useCase.associate(1L, 3L, 1L);
    useCase.associate(2L, p4.id, 1L);
    useCase.associate(2L, p5.id, 1L);

    // Attempting a 6th product type in Warehouse 1 must fail
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> useCase.associate(2L, p6.id, 1L));
    assertTrue(exception.getMessage().contains("already stores maximum of 5 types of products"));
  }
}
