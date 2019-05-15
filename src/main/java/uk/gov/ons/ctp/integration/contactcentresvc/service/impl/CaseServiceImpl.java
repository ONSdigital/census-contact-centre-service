package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@Service
@Validated()
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

  // when the rest client has been written
  // @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Autowired ProductReference productReference;

  @Override
  public ResponseDTO fulfilmentRequestByPost(UUID caseId, PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    // Get the case using the CaseServiceClientServiceImpl when available
    // Case caze = caseServiceClientService.getCase(requestBodyDTO.getCaseId());

    Product example = new Product();
    example.setFulfilmentCode(requestBodyDTO.getProductCode());
    example.setRequestChannels(Arrays.asList(RequestChannel.CC));
    example.setDeliveryChannel(DeliveryChannel.POST);
    // once we have the case...
    // example.setRegions(Arrays.asList(Region.valueOf(caze.getRegion().substring(0,1))));
    List<Product> products = productReference.searchProducts(example);

    log.info("Hello there 1");
    if (products.size() == 0) {
      // log.warn here
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }
    log.info("Hello there 2");
    if (products.size() > 1) {
      // log.warn here
      throw new CTPException(Fault.SYSTEM_ERROR, "More then one matching product was found");
    }

    // here you need to construct an Event to publish...

    // and publish it

    ResponseDTO response =
        ResponseDTO.builder()
            .id(caseId.toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return response;
  }
}
