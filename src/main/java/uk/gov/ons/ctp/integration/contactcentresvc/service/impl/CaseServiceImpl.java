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
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.Header;
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
    log.with(caseId)
        .with(requestBodyDTO)
        .info("Now in the fulfilmentRequestByPost method in class CaseServiceImpl.");

    // Get the case using the CaseServiceClientServiceImpl when available
    // Case caze = caseServiceClientService.getCase(requestBodyDTO.getCaseId());

    String fulfilmentCode = requestBodyDTO.getProductCode();
    Product.DeliveryChannel deliveryChannel = DeliveryChannel.POST;

    FulfilmentRequestedEvent fulfilmentRequestedEvent =
        searchProductsAndConstructEvent(fulfilmentCode, deliveryChannel);

    // publish the event

    ResponseDTO response =
        ResponseDTO.builder()
            .id(caseId.toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return response;
  }

  public FulfilmentRequestedEvent searchProductsAndConstructEvent(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel) throws CTPException {
    log.with(fulfilmentCode)
        .info("Now in the earchProductsAndConstructEvent method in class CaseServiceImpl.");

    Product example = new Product();
    example.setFulfilmentCode(fulfilmentCode);
    example.setRequestChannels(Arrays.asList(RequestChannel.CC));
    example.setDeliveryChannel(deliveryChannel);

    // once we have the case...
    // example.setRegions(Arrays.asList(Region.valueOf(caze.getRegion().substring(0,1))));
    List<Product> products = productReference.searchProducts(example);

    if (products.size() == 0) {
      // log.warn here
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    if (products.size() > 1) {
      // log.warn here
      throw new CTPException(Fault.SYSTEM_ERROR, "More then one matching product was found");
    }

    Product productFound = products.get(0);

    // if productFound caseType = HI then we need to set the individualCaseId of the
    // fulfilmentRequestedEvent to be a new UUID

    // here you need to construct an Event to publish...
    FulfilmentRequestedEvent fulfilmentRequestedEvent = new FulfilmentRequestedEvent();

    FulfilmentPayload fulfilmentPayload = fulfilmentRequestedEvent.getPayload();

    FulfilmentRequest fulfilmentRequest = fulfilmentPayload.getFulfilmentRequest();

    fulfilmentRequest.setFulfilmentCode("XXXXXX-XXXXXX");
    fulfilmentRequest.setCaseId("bbd55984-0dbf-4499-bfa7-0aa4228700e9");

    Address address = fulfilmentRequest.getAddress();
    address.setAddressLine1("1 main street");
    address.setAddressLine2("upper upperingham");
    address.setAddressLine3("");
    address.setTownName("upton");
    address.setPostcode("UP103UP");
    address.setRegion("E");
    address.setLatitude("50.863849");
    address.setLongitude("-1.229710");
    address.setUprn("XXXXXXXXXXXXX");
    address.setArid("XXXXX");
    address.setAddressType("CE");
    address.setEstabType("XXX");

    Contact contact = fulfilmentRequest.getContact();
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setEmail("me@example.com");
    contact.setTelNo("+447890000000");

    Header header = new Header();
    header.setType("FULFILMENT_REQUESTED");
    header.setSource("CONTACT_CENTRE_API");
    header.setChannel("CC");
    //    header.setDateTime(DateTimeUtil.getCurrentDateTimeInJsonFormat());
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    fulfilmentRequestedEvent.setEvent(header);

    return fulfilmentRequestedEvent;
  }
}
