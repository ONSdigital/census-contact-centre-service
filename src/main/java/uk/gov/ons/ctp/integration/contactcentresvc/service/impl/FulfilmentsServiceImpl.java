package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ProductGroup;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.service.FulfilmentsService;

@Service
@Validated()
public class FulfilmentsServiceImpl implements FulfilmentsService {
  @Autowired ProductReference productReference;

  @Autowired private MapperFacade mapperFacade;

  @Autowired private AppConfig appConfig;

  @Override
  public List<FulfilmentDTO> getFulfilments(
      CaseType caseType,
      Region region,
      DeliveryChannel deliveryChannel,
      Boolean individual,
      ProductGroup productGroup)
      throws CTPException {
    Product example = new Product();
    example.setCaseTypes(
        caseType == null ? null : Arrays.asList(Product.CaseType.valueOf(caseType.name())));
    example.setRegions(
        region == null ? null : Arrays.asList(Product.Region.valueOf(region.name())));
    example.setRequestChannels(Arrays.asList(RequestChannel.CC));
    example.setDeliveryChannel(
        deliveryChannel == null ? null : Product.DeliveryChannel.valueOf(deliveryChannel.name()));
    example.setIndividual(individual);
    example.setProductGroup(
        productGroup == null ? null : Product.ProductGroup.valueOf(productGroup.name()));
    List<FulfilmentDTO> fulfilments =
        mapperFacade.mapAsList(productReference.searchProducts(example), FulfilmentDTO.class);

    // Remove blacklisted products
    Set<String> blacklistedProducts = appConfig.getFulfilments().getBlacklistedCodes();
    List<FulfilmentDTO> filteredFulfilments =
        fulfilments.stream()
            .filter(p -> !blacklistedProducts.contains(p.getFulfilmentCode()))
            .collect(toList());

    return filteredFulfilments;
  }
}
