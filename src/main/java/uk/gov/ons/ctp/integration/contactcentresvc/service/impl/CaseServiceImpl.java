package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.event.ContactCentreEventPublisher;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@Service
@Validated()
public class CaseServiceImpl implements CaseService {

  @Autowired private ContactCentreEventPublisher publisher;

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);
  private static final String FULFILMENT_REQUESTED_TYPE = "FULFILMENT_REQUESTED";
  private static final String CONTACT_CENTRE_SOURCE = "CONTACT_CENTRE_API";
  private static final String CONTACT_CENTRE_CHANNEL = "CC";

  private MapperFacade caseDTOMapper = new CCSvcBeanMapper();

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Autowired ProductReference productReference;

  @Override
  public ResponseDTO fulfilmentRequestByPost(
      UUID notNeeded, PostalFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    log.with(requestBodyDTO)
        .info("Now in the fulfilmentRequestByPost method in class CaseServiceImpl.");

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());

    FulfilmentRequestedEvent fulfilmentRequestedEvent =
        createFulfilmentEvent(
            requestBodyDTO.getFulfilmentCode(), DeliveryChannel.POST, caseId, contact);

    publisher.sendEvent(fulfilmentRequestedEvent);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    return response;
  }

  public FulfilmentRequestedEvent searchProductsAndConstructEvent(
      String fulfilmentCode,
      Product.DeliveryChannel deliveryChannel,
      CaseContainerDTO caseContainerDTO,
      UUID caseId)
      throws CTPException {
    log.with(fulfilmentCode)
        .info("Now in the searchProductsAndConstructEvent method in class CaseServiceImpl.");

    Product example = new Product();
    example.setFulfilmentCode(fulfilmentCode);
    example.setRequestChannels(Arrays.asList(Product.RequestChannel.CC));
    example.setDeliveryChannel(deliveryChannel);
    Product.Region region = Product.Region.valueOf(caseContainerDTO.getRegion().substring(0, 1));
    example.setRegions(Arrays.asList(region));
    List<Product> products = productReference.searchProducts(example);

    if (products.size() == 0) {
      log.with(products).warn("Compatible product cannot be found");
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    if (products.size() > 1) {
      log.with(products).warn("More then one matching product was found");
      throw new CTPException(Fault.SYSTEM_ERROR, "More then one matching product was found");
    }

    Product productFound = products.get(0);

    FulfilmentRequestedEvent fulfilmentRequestedEvent = new FulfilmentRequestedEvent();

    FulfilmentPayload fulfilmentPayload = fulfilmentRequestedEvent.getPayload();

    FulfilmentRequest fulfilmentRequest = fulfilmentPayload.getFulfilmentRequest();

    Product.CaseType caseType = productFound.getCaseType();

    if (caseType.toString().equals("HI")) {
      UUID newUUID;

      newUUID = UUID.randomUUID();

      fulfilmentRequest.setIndividualCaseId(newUUID.toString());
    }

    fulfilmentRequest.setFulfilmentCode(productFound.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());

    Header header = new Header();
    header.setType(FULFILMENT_REQUESTED_TYPE);
    header.setSource(CONTACT_CENTRE_SOURCE);
    header.setChannel(CONTACT_CENTRE_CHANNEL);
    header.setDateTime(DateTimeUtil.nowUTC());
    header.setTransactionId(UUID.randomUUID().toString());
    fulfilmentRequestedEvent.setEvent(header);

    return fulfilmentRequestedEvent;
  }

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by caseId: {}", caseId);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = caseServiceClient.getCaseById(caseId, getCaseEvents);

    // Only return Household cases
    if (!caseIsHouseholdOrCommunal(caseDetails.getCaseType())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Case is a not a household or communal case");
    }

    // Convert from Case service to Contact Centre DTOs
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);

    // Make sure that we don't return any events if the caller doesn't want them
    if (!getCaseEvents) {
      caseServiceResponse.setCaseEvents(null);
    }

    log.debug("Returning case details for caseId: {}", caseId);

    return caseServiceResponse;
  }

  @Override
  public List<CaseDTO> getCaseByUPRN(
      UniquePropertyReferenceNumber uprn, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by UPRN: {}", uprn);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    List<CaseContainerDTO> caseDetails =
        caseServiceClient.getCaseByUprn(uprn.getValue(), getCaseEvents);

    // Only return Household cases
    List<CaseContainerDTO> householdCases =
        caseDetails
            .parallelStream()
            .filter(c -> caseIsHouseholdOrCommunal(c.getCaseType()))
            .collect(Collectors.toList());

    // Convert from Case service to Contact Centre DTOs
    List<CaseDTO> caseServiceResponse = caseDTOMapper.mapAsList(householdCases, CaseDTO.class);

    // Make sure that we don't return any events if the caller doesn't want them
    if (!getCaseEvents) {
      caseServiceResponse.parallelStream().forEach(c -> c.setCaseEvents(null));
    }

    log.debug(
        "Returning case details for UPRN: {}. Result set size: {}",
        uprn,
        caseServiceResponse.size());

    return caseServiceResponse;
  }

  @Override
  public CaseDTO getCaseByCaseReference(final long caseRef, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by case reference: {}", caseRef);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = caseServiceClient.getCaseByCaseRef(caseRef, getCaseEvents);

    // Only return Household cases
    if (!caseIsHouseholdOrCommunal(caseDetails.getCaseType())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Case is a not a household or communal case");
    }

    // Convert from Case service to Contact Centre DTOs
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);

    // Make sure that we don't return any events if the caller doesn't want them
    if (!getCaseEvents) {
      caseServiceResponse.setCaseEvents(null);
    }

    log.debug("Returning case details for case reference: {}", caseRef);

    return caseServiceResponse;
  }

  private boolean caseIsHouseholdOrCommunal(String caseTypeString) {
    return caseTypeString.equals(CaseType.H.name()) || caseTypeString.equals(CaseType.C.name());
  }

  /**
   * create a contact centre fulfilment request event
   *
   * @param fulfilmentCode the code for the product requested
   * @param deliveryChannel how the fulfilment should be delivered
   * @param caseId the id of the household case the fulfilment is for
   * @return the request event to be delivered to the events exchange
   * @throws CTPException the requested product is invalid for the parameters given
   */
  private FulfilmentRequestedEvent createFulfilmentEvent(
      String fulfilmentCode, DeliveryChannel deliveryChannel, UUID caseId, Contact contact)
      throws CTPException {
    log.with(fulfilmentCode)
        .info("Entering createFulfilmentEvent method in class CaseServiceImpl.");

    Region region =
        Region.valueOf(caseServiceClient.getCaseById(caseId, false).getRegion().substring(0, 1));
    Product product = findProduct(fulfilmentCode, deliveryChannel, region);

    FulfilmentRequestedEvent fulfilmentRequestedEvent = new FulfilmentRequestedEvent();

    // Set up the event header
    Header header =
        Header.builder()
            .type(FULFILMENT_REQUESTED_TYPE)
            .source(CONTACT_CENTRE_SOURCE)
            .channel(RequestChannel.CC.name())
            .dateTime(DateTimeUtil.nowUTC())
            .transactionId(UUID.randomUUID().toString())
            .build();
    fulfilmentRequestedEvent.setEvent(header);

    // Set up the event payload request
    FulfilmentRequest fulfilmentRequest =
        fulfilmentRequestedEvent.getPayload().getFulfilmentRequest();
    if (product.getCaseType().toString().equals(Product.CaseType.HI.name())) {
      fulfilmentRequest.setIndividualCaseId(UUID.randomUUID().toString());
    }

    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);

    return fulfilmentRequestedEvent;
  }

  /**
   * find the product using the parameters provided
   *
   * @param fulfilmentCode the code for the product requested
   * @param deliveryChannel how should the fulfilment be delivered
   * @param region identifies the region of the household case the fulfilment is for - used to
   *     confirm the requested products eligibility
   * @return the matching product
   * @throws CTPException the product could not found or is ineligible for the given parameters
   */
  private Product findProduct(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel, Product.Region region)
      throws CTPException {
    log.info("Entering findProduct method in class CaseServiceImpl.");
    Product example =
        Product.builder()
            .fulfilmentCode(fulfilmentCode)
            .requestChannels(Arrays.asList(Product.RequestChannel.CC))
            .deliveryChannel(deliveryChannel)
            .regions(Arrays.asList(region))
            .build();
    List<Product> products = productReference.searchProducts(example);
    if (products.size() == 0) {
      log.with(products).warn("Compatible product cannot be found");
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    if (products.size() > 1) {
      log.with(products).warn("More then one matching product was found");
      throw new CTPException(Fault.SYSTEM_ERROR, "More than one matching product was found");
    }
    return products.get(0);
  }
}
