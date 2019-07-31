package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.integration.contactcentresvc.utility.Constants.UNKNOWN_UUID;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@Service
@Validated()
@Configuration
public class CaseServiceImpl implements CaseService {

  @Autowired private EventPublisher publisher;

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);
  private static final String RESPONDENT_REFUSAL_TYPE = "HARD_REFUSAL";

  @Autowired private AppConfig appConfig;

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Autowired private ProductReference productReference;

  private MapperFacade caseDTOMapper = new CCSvcBeanMapper();

  public ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    log.with(requestBodyDTO)
        .info("Now in the fulfilmentRequestByPost method in class CaseServiceImpl.");

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());

    FulfilmentRequest fulfilmentRequestPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), DeliveryChannel.POST, caseId, contact);

    publisher.sendEvent(
        EventType.FULFILMENT_REQUESTED,
        Source.CONTACT_CENTRE_API,
        Channel.CC,
        fulfilmentRequestPayload);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    log.with(response)
        .info("Now returning from the fulfilmentRequestByPost method in class CaseServiceImpl.");

    return response;
  }

  public ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with(requestBodyDTO)
        .info("Now in the fulfilmentRequestBySMS method in class CaseServiceImpl.");

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTelNo(requestBodyDTO.getTelNo());

    FulfilmentRequest fulfilmentRequestedPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), DeliveryChannel.SMS, caseId, contact);
    publisher.sendEvent(
        EventType.FULFILMENT_REQUESTED,
        Source.CONTACT_CENTRE_API,
        Channel.CC,
        fulfilmentRequestedPayload);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    log.with(response)
        .info("Now returning from the fulfilmentRequestBySMS method in class CaseServiceImpl.");

    return response;
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

    filterCaseEvents(caseServiceResponse, getCaseEvents);

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

    // Clean up the events before returning them
    caseServiceResponse.stream().forEach(c -> filterCaseEvents(c, getCaseEvents));

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

    filterCaseEvents(caseServiceResponse, getCaseEvents);

    log.debug("Returning case details for case reference: {}", caseRef);

    return caseServiceResponse;
  }

  @Override
  public ResponseDTO reportRefusal(UUID caseId, RefusalRequestDTO requestBodyDTO)
      throws CTPException {
    String reportedDateTime = "null";
    if (requestBodyDTO.getDateTime() != null) {
      reportedDateTime = DateTimeUtil.formatDate(requestBodyDTO.getDateTime());
    }
    log.debug(
        "Processing refusal for case {} with reported dateTime of {}", caseId, reportedDateTime);

    // Create and publish a respondent refusal event
    UUID refusalCaseId = caseId == null ? new UUID(0, 0) : caseId;
    RespondentRefusalDetails refusalPayload =
        createRespondentRefusalPayload(refusalCaseId, requestBodyDTO);

    publisher.sendEvent(
        EventType.REFUSAL_RECEIVED, Source.CONTACT_CENTRE_API, Channel.CC, refusalPayload);

    // Build response
    ResponseDTO response =
        ResponseDTO.builder()
            .id(caseId == null ? UNKNOWN_UUID : caseId.toString())
            .dateTime(DateTimeUtil.nowUTC())
            .build();

    log.debug("Returning refusal response for case {}", caseId);

    return response;
  }

  private void filterCaseEvents(CaseDTO caseDTO, Boolean getCaseEvents) {
    if (getCaseEvents) {
      // Only return whitelisted events
      Set<String> whitelistedEventCategories =
          appConfig.getCaseServiceSettings().getWhitelistedEventCategories();
      List<CaseEventDTO> filteredEvents =
          caseDTO
              .getCaseEvents()
              .stream()
              .filter(e -> whitelistedEventCategories.contains(e.getCategory()))
              .collect(Collectors.toList());
      caseDTO.setCaseEvents(filteredEvents);
    } else {
      // Caller doesn't want any event data
      caseDTO.setCaseEvents(null);
    }
  }

  private boolean caseIsHouseholdOrCommunal(String caseTypeString) {
    return caseTypeString.equals(CaseType.HH.name()) || caseTypeString.equals(CaseType.CE.name());
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
  private FulfilmentRequest createFulfilmentRequestPayload(
      String fulfilmentCode, DeliveryChannel deliveryChannel, UUID caseId, Contact contact)
      throws CTPException {
    log.with(fulfilmentCode)
        .debug("Entering createFulfilmentEvent method in class CaseServiceImpl.");

    Region region =
        Region.valueOf(caseServiceClient.getCaseById(caseId, false).getRegion().substring(0, 1));
    Product product = findProduct(fulfilmentCode, deliveryChannel, region);

    if (deliveryChannel.equals(DeliveryChannel.POST)) {
      if (!caseIsHouseholdOrCommunal(product.getCaseType().name())) {
        if (StringUtils.isBlank(contact.getTitle())
            || StringUtils.isBlank(contact.getForename())
            || StringUtils.isBlank(contact.getSurname())) {
          throw new CTPException(
              Fault.BAD_REQUEST,
              "The fulfilment is for an individual so none of the following fields can be empty: "
                  + "'title', 'forename' and 'surname'");
        }
      }
    }

    // Set up the event payload request
    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    if (product.getCaseType().equals(Product.CaseType.HI)) {
      fulfilmentRequest.setIndividualCaseId(UUID.randomUUID().toString());
    }

    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);

    return fulfilmentRequest;
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
    log.with(fulfilmentCode)
        .with(deliveryChannel)
        .with(region)
        .debug("Passing fulfilmentCode, deliveryChannel, and region, into findProduct method.");
    Product searchCriteria =
        Product.builder()
            .fulfilmentCode(fulfilmentCode)
            .requestChannels(Arrays.asList(Product.RequestChannel.CC))
            .deliveryChannel(deliveryChannel)
            .regions(Arrays.asList(region))
            .build();
    List<Product> products = productReference.searchProducts(searchCriteria);
    if (products.size() == 0) {
      log.with(products).warn("Compatible product cannot be found");
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    return products.get(0);
  }

  /**
   * Create a case refusal event.
   *
   * @param caseId is the UUID for the case, or null if the endpoint was invoked with a caseId of
   *     'unknown'.
   * @param refusalRequest holds the details about the refusal.
   * @return the request event to be delivered to the events exchange.
   * @throws CTPException if there is a failure.
   */
  private RespondentRefusalDetails createRespondentRefusalPayload(
      UUID caseId, RefusalRequestDTO refusalRequest) throws CTPException {

    // Create message payload
    RespondentRefusalDetails refusal = new RespondentRefusalDetails();
    refusal.setType(RESPONDENT_REFUSAL_TYPE);
    refusal.setReport(refusalRequest.getNotes());
    CollectionCaseCompact collectionCase = new CollectionCaseCompact(caseId);
    refusal.setCollectionCase(collectionCase);

    // Populate contact
    Contact contact = new Contact();
    // --- Start of code commented out for 2019 rehearsal. CR-416. ---
    // contact.setTitle(refusalRequest.getTitle());
    // contact.setForename(refusalRequest.getForename());
    // contact.setSurname(refusalRequest.getSurname());
    // contact.setTelNo(refusalRequest.getTelNo());
    // --- End of code commented out for 2019 rehearsal. CR-416. ---
    refusal.setContact(contact);

    // Populate address
    AddressCompact address = new AddressCompact();
    address.setAddressLine1(refusalRequest.getAddressLine1());
    address.setAddressLine2(refusalRequest.getAddressLine2());
    address.setAddressLine3(refusalRequest.getAddressLine3());
    address.setTownName(refusalRequest.getTownName());
    address.setPostcode(refusalRequest.getPostcode());
    address.setRegion(refusalRequest.getRegion());
    refusal.setAddress(address);

    return refusal;
  }
}
