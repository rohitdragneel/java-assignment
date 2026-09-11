package com.fulfilment.application.monolith.fulfillment.adapters.gateways;

import com.fulfilment.application.monolith.fulfillment.domain.ports.outbound.ProductResolver;
import com.fulfilment.application.monolith.products.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class ProductGatewayAdapter implements ProductResolver {

  @Inject ProductRepository productRepository;

  @Override
  public boolean existsById(Long productId) {
    return productRepository.findById(productId) != null;
  }
}
