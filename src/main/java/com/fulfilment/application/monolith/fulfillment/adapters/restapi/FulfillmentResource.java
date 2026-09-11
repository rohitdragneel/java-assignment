package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.fulfillment.adapters.database.DbFulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.adapters.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.inbound.AssociateProductFulfillmentOperation;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Path("fulfillment")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject AssociateProductFulfillmentOperation associateUseCase;
  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject WarehouseRepository warehouseRepository;

  @POST
  public Response create(CreateFulfillmentRequest request) {
    if (request == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "Request body is required"))
          .build();
    }

    Long targetWarehouseId = request.warehouseId;

    if (targetWarehouseId == null && request.warehouseBusinessUnitCode != null) {
      DbFulfillmentAssignment ignored = null; // Panache find via warehouseRepository
      var dbWh = warehouseRepository.find("businessUnitCode", request.warehouseBusinessUnitCode)
          .firstResultOptional();
      if (dbWh.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error",
                "Warehouse not found with business unit code: " + request.warehouseBusinessUnitCode))
            .build();
      }
      targetWarehouseId = dbWh.get().id;
    }

    try {
      FulfillmentAssignment result =
          associateUseCase.associate(request.storeId, request.productId, targetWarehouseId);
      return Response.status(Response.Status.CREATED).entity(result).build();
    } catch (NoSuchElementException e) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(Map.of("error", e.getMessage())).build();
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", e.getMessage())).build();
    }
  }

  @GET
  public List<FulfillmentAssignment> list(
      @QueryParam("storeId") Long storeId,
      @QueryParam("productId") Long productId,
      @QueryParam("warehouseId") Long warehouseId) {
    return fulfillmentRepository.listWithFilters(storeId, productId, warehouseId);
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(@PathParam("id") Long id) {
    if (!fulfillmentRepository.deleteById(id)) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(Map.of("error", "Assignment not found with id: " + id))
          .build();
    }
    return Response.noContent().build();
  }
}
