package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.domain.usecases.AssociateProductFulfillmentUseCase;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
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
  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  public void setUp() {
    fulfillmentRepository.deleteAllAssignments();
  }

  @Test
  public void testSuccessfulAssociation() {
    FulfillmentAssignment result = useCase.associate(1L, 1L, 1L);
    assertNotNull(result);
    assertNotNull(result.id);
    assertEquals(1L, result.storeId);
    assertEquals(1L, result.productId);
    assertEquals(1L, result.warehouseId);
  }

  @Test
  public void testStoreNotFound() {
    assertThrows(NoSuchElementException.class, () -> useCase.associate(9999L, 1L, 1L));
  }

  @Test
  public void testProductNotFound() {
    assertThrows(NoSuchElementException.class, () -> useCase.associate(1L, 9999L, 1L));
  }

  @Test
  public void testWarehouseNotFound() {
    assertThrows(NoSuchElementException.class, () -> useCase.associate(1L, 1L, 9999L));
  }

  @Test
  @Transactional
  public void testArchivedWarehouseFails() {
    DbWarehouse archived = new DbWarehouse();
    archived.businessUnitCode = "ARCH-001";
    archived.location = "ZWOLLE-001";
    archived.capacity = 100;
    archived.stock = 10;
    archived.createdAt = LocalDateTime.now();
    archived.archivedAt = LocalDateTime.now();
    warehouseRepository.persist(archived);

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> useCase.associate(1L, 1L, archived.id));
    assertTrue(ex.getMessage().contains("archived"));
  }

  @Test
  public void testDuplicateAssociationFails() {
    useCase.associate(1L, 1L, 1L);
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> useCase.associate(1L, 1L, 1L));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  public void testConstraint1_Max2WarehousesPerProductPerStore() {
    useCase.associate(1L, 1L, 1L);
    useCase.associate(1L, 1L, 2L);

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> useCase.associate(1L, 1L, 3L));
    assertTrue(ex.getMessage().contains("already fulfilled by maximum of 2 warehouses"));
  }

  @Test
  @Transactional
  public void testConstraint2_Max3WarehousesPerStore() {
    DbWarehouse wh4 = new DbWarehouse();
    wh4.businessUnitCode = "MWH.004";
    wh4.location = "AMSTERDAM-001";
    wh4.capacity = 50;
    wh4.stock = 10;
    wh4.createdAt = LocalDateTime.now();
    warehouseRepository.persist(wh4);

    useCase.associate(1L, 1L, 1L);
    useCase.associate(1L, 2L, 2L);
    useCase.associate(1L, 3L, 3L);

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> useCase.associate(1L, 1L, wh4.id));
    assertTrue(ex.getMessage().contains("already fulfilled by maximum of 3 warehouses"));
  }

  @Test
  @Transactional
  public void testConstraint3_Max5ProductsPerWarehouse() {
    Product p4 = new Product("PROD-4"); p4.stock = 5; productRepository.persist(p4);
    Product p5 = new Product("PROD-5"); p5.stock = 5; productRepository.persist(p5);
    Product p6 = new Product("PROD-6"); p6.stock = 5; productRepository.persist(p6);

    useCase.associate(1L, 1L, 1L);
    useCase.associate(1L, 2L, 1L);
    useCase.associate(1L, 3L, 1L);
    useCase.associate(2L, p4.id, 1L);
    useCase.associate(2L, p5.id, 1L);

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> useCase.associate(2L, p6.id, 1L));
    assertTrue(ex.getMessage().contains("already stores maximum of 5 types of products"));
  }

  @Test
  public void testNullArgumentsFail() {
    assertThrows(IllegalArgumentException.class, () -> useCase.associate(null, 1L, 1L));
    assertThrows(IllegalArgumentException.class, () -> useCase.associate(1L, null, 1L));
    assertThrows(IllegalArgumentException.class, () -> useCase.associate(1L, 1L, null));
  }
}
