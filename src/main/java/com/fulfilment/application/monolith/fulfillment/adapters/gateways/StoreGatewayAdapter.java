package com.fulfilment.application.monolith.fulfillment.adapters.gateways;

import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.StoreResolver;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class StoreGatewayAdapter implements StoreResolver {

  @Override
  public boolean existsById(Long storeId) {
    return Store.findById(storeId) != null;
  }
}
