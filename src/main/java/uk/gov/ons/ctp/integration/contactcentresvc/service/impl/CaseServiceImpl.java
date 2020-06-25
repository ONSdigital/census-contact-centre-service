package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static uk.gov.ons.ctp.integration.contactcentresvc.utility.Constants.UNKNOWN_UUID;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);
  private static final Collection<String> VALID_REGIONS =
      Stream.of(uk.gov.ons.ctp.integration.contactcentresvc.representation.Region.values())
          .map(Enum::name)
          .collect(Collectors.toList());
  private static final String NI_LAUNCH_ERR_MSG =
      "All Northern Ireland calls from CE Managers are to be escalated to the NI management team.";
  private static final String UNIT_LAUNCH_ERR_MSG =
      "A CE Manager form can only be launched against an establishment address not a UNIT.";

  private static final String SCOTLAND_COUNTRY_CODE = "S";

  @Autowired private AppConfig appConfig;

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Autowired private ProductReference productReference;

  private MapperFacade caseDTOMapper = new CCSvcBeanMapper();

  @Autowired private EqLaunchService eqLaunchService;

  @Autowired private CaseDataRepository dataRepo;

  @Autowired private AddressService addressSvc;

  @Autowired private EventPublisher eventPublisher;

  private LuhnCheckDigit luhnChecker = new LuhnCheckDigit();

  public ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    log.with(requestBodyDTO)
        .debug("Now in the fulfilmentRequestByPost method in class CaseServiceImpl.");

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());

    FulfilmentRequest fulfilmentRequestPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), Product.DeliveryChannel.POST, caseId, contact);

    eventPublisher.sendEvent(
        EventType.FULFILMENT_REQUESTED,
        Source.CONTACT_CENTRE_API,
        appConfig.getChannel(),
        fulfilmentRequestPayload);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    log.with(response)
        .debug("Now returning from the fulfilmentRequestByPost method in class CaseServiceImpl.");

    return response;
  }

  public ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with(requestBodyDTO)
        .debug("Now in the fulfilmentRequestBySMS method in class CaseServiceImpl.");

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTelNo(requestBodyDTO.getTelNo());

    FulfilmentRequest fulfilmentRequestedPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), Product.DeliveryChannel.SMS, caseId, contact);
    eventPublisher.sendEvent(
        EventType.FULFILMENT_REQUESTED,
        Source.CONTACT_CENTRE_API,
        appConfig.getChannel(),
        fulfilmentRequestedPayload);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    log.with(response)
        .debug("Now returning from the fulfilmentRequestBySMS method in class CaseServiceImpl.");

    return response;
  }

  @Override
  public CaseDTO createCaseForNewAddress(NewCaseRequestDTO caseRequestDTO) throws CTPException {
    CaseType caseType = caseRequestDTO.getCaseType();

    // Validate that case type and address match
    String censusAddressType;
    if (caseRequestDTO.getEstabType() == EstabType.OTHER) {
      // Can't get an address type from the estab so it'll need to be the same as the case type
      censusAddressType = caseType.name();
    } else {
      AddressType addressType = caseRequestDTO.getEstabType().getAddressType().get();
      if (!addressType.name().equals(caseType.name())) {
        throw new CTPException(
            Fault.BAD_REQUEST,
            "Derived address type of '"
                + addressType.name()
                + "', from establishment type '"
                + caseRequestDTO.getEstabType().name()
                + "', "
                + "is not compatible with caseType of '"
                + caseRequestDTO.getCaseType().name()
                + "'");
      }
      censusAddressType = addressType.name();
    }

    // Reject if CE with non-positive number of residents
    if (caseRequestDTO.getCaseType() == CaseType.CE) {
      if (caseRequestDTO.getCeUsualResidents() <= 0) {
        throw new CTPException(
            Fault.BAD_REQUEST, "Number of residents must be supplied for CE case");
      }
    } else {
      // Field not relevant. Clear incase it's a silly number
      caseRequestDTO.setCeUsualResidents(0);
    }

    // Create new case
    CachedCase cachedCase = caseDTOMapper.map(caseRequestDTO, CachedCase.class);
    UUID newCaseId = UUID.randomUUID();
    cachedCase.setId(newCaseId.toString());
    cachedCase.setEstabType(caseRequestDTO.getEstabType().getCode());
    cachedCase.setAddressType(censusAddressType);
    cachedCase.setCreatedDateTime(DateTimeUtil.nowUTC());

    dataRepo.writeCachedCase(cachedCase);

    // Publish NewAddress event
    AddressIndexAddressCompositeDTO address =
        caseDTOMapper.map(caseRequestDTO, AddressIndexAddressCompositeDTO.class);
    address.setCensusAddressType(censusAddressType);
    address.setCensusEstabType(caseRequestDTO.getEstabType().getCode());
    address.setCountryCode(caseRequestDTO.getRegion().name());
    publishNewAddressReportedEvent(
        newCaseId,
        caseType,
        caseRequestDTO.getCeOrgName(),
        caseRequestDTO.getCeUsualResidents(),
        address);

    return createNewCachedCaseResponse(cachedCase);
  }

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.debug("Fetching case details by caseId: {}", caseId);

    // Get the case details from the case service, or failing that from the cache
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = retrieveCaseById(caseId, getCaseEvents);

    // Do not return HI cases
    if (caseDetails.getCaseType().equals(CaseType.HI.name())) {
      log.with(caseId).info("Case is not suitable as it is a household individual case");
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case is not suitable");
    }

    // Convert from Case service to Contact Centre DTOs NB. A request for an SPG case will not get
    // this far.
    CaseDTO caseServiceResponse = mapCaseContainerDTO(caseDetails);

    filterCaseEvents(caseServiceResponse, getCaseEvents);

    log.with("caseId", caseId).debug("Returning case details for caseId");

    return caseServiceResponse;
  }

  private CaseDTO mapCaseContainerDTO(CaseContainerDTO caseDetails) {
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);
    caseServiceResponse.setAllowedDeliveryChannels(
        calculateAllowedDeliveryChannels(caseServiceResponse));
    caseServiceResponse.setEstabType(EstabType.forCode(caseServiceResponse.getEstabDescription()));

    return caseServiceResponse;
  }

  private List<CaseDTO> mapCaseContainerDTOList(List<CaseContainerDTO> casesToReturn) {
    List<CaseDTO> caseServiceListResponse = caseDTOMapper.mapAsList(casesToReturn, CaseDTO.class);

    for (CaseDTO caseServiceResponse : caseServiceListResponse) {
      caseServiceResponse.setAllowedDeliveryChannels(
          calculateAllowedDeliveryChannels(caseServiceResponse));
      caseServiceResponse.setEstabType(
          EstabType.forCode(caseServiceResponse.getEstabDescription()));
    }

    return caseServiceListResponse;
  }

  private List<DeliveryChannel> calculateAllowedDeliveryChannels(CaseDTO caseServiceResponse) {

    List<DeliveryChannel> dcList = null;

    if (caseServiceResponse.isHandDelivery()
        && caseServiceResponse.getCaseType().equals(CaseType.SPG.name())) {
      log.with(caseServiceResponse.getId())
          .debug(
              "Calculating allowed delivery channel list as [SMS] because handDelivery=true "
                  + "and caseType=SPG");
      dcList = Arrays.asList(DeliveryChannel.SMS);
    } else {
      log.with(caseServiceResponse.getId())
          .debug("Calculating allowed delivery channel list as [POST, SMS]");
      dcList = Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    }

    return dcList;
  }

  @Override
  public List<CaseDTO> getCaseByUPRN(
      UniquePropertyReferenceNumber uprn, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("uprn", uprn).debug("Fetching case details by UPRN");

    List<CaseDTO> rmCases = callCaseSvcByUPRN(uprn.getValue(), requestParamsDTO.getCaseEvents());
    if (!rmCases.isEmpty()) {
      log.with("uprn", uprn).with("cases", rmCases.size()).debug("Returning case details for UPRN");
      return rmCases;
    }

    // Return stored case details if present
    Optional<CachedCase> cachedCase = dataRepo.readCachedCaseByUPRN(uprn);
    if (cachedCase.isPresent()) {
      log.with("uprn", uprn).debug("Returning stored case details for UPRN");
      CaseDTO response = createNewCachedCaseResponse(cachedCase.get());
      return Collections.singletonList(response);
    }

    // New Case
    CachedCase newcase = createNewCachedCase(uprn.getValue());
    log.with("uprn", uprn)
        .with("caseId", newcase.getId())
        .debug("Returning new skeleton case for UPRN");
    CaseDTO response = createNewCachedCaseResponse(newcase);
    return Collections.singletonList(response);
  }

  private void validateCaseRef(long caseRef) throws CTPException {
    if (!luhnChecker.isValid(Long.toString(caseRef))) {
      log.with(caseRef).info("Luhn check failed for case Reference");
      throw new CTPException(Fault.BAD_REQUEST, "Invalid Case Reference");
    }
  }

  @Override
  public CaseDTO getCaseByCaseReference(final long caseRef, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("caseRef", caseRef).debug("Fetching case details by case reference");

    validateCaseRef(caseRef);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = caseServiceClient.getCaseByCaseRef(caseRef, getCaseEvents);

    // Do not return HI cases
    if (caseDetails.getCaseType().equals(CaseType.HI.name())) {
      log.with(caseDetails.getId())
          .info("Case is not suitable as it is a household individual case");
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case is not suitable");
    }

    CaseDTO caseServiceResponse = mapCaseContainerDTO(caseDetails);

    filterCaseEvents(caseServiceResponse, getCaseEvents);

    log.with("caseRef", caseRef).debug("Returning case details for case reference");

    return caseServiceResponse;
  }

  @Override
  public ResponseDTO reportRefusal(UUID caseId, RefusalRequestDTO requestBodyDTO)
      throws CTPException {
    String reportedDateTime = "null";
    if (requestBodyDTO.getDateTime() != null) {
      reportedDateTime = DateTimeUtil.formatDate(requestBodyDTO.getDateTime());
    }
    log.with("caseId", caseId)
        .with("reportedDateTime", reportedDateTime)
        .debug("Processing refusal for case with reported dateTime");

    // Create and publish a respondent refusal event
    UUID refusalCaseId = caseId == null ? new UUID(0, 0) : caseId;
    RespondentRefusalDetails refusalPayload =
        createRespondentRefusalPayload(refusalCaseId, requestBodyDTO);

    eventPublisher.sendEvent(
        EventType.REFUSAL_RECEIVED,
        Source.CONTACT_CENTRE_API,
        appConfig.getChannel(),
        refusalPayload);

    // Build response
    ResponseDTO response =
        ResponseDTO.builder()
            .id(caseId == null ? UNKNOWN_UUID : caseId.toString())
            .dateTime(DateTimeUtil.nowUTC())
            .build();

    log.with("caseId", caseId).debug("Returning refusal response for case");

    return response;
  }

  @Override
  public String getLaunchURLForCaseId(final UUID caseId, LaunchRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("caseId", caseId)
        .with("request", requestParamsDTO)
        .debug("Processing request to create launch URL");

    CaseContainerDTO caseDetails = getLaunchCase(caseId);

    CaseType caseType = CaseType.valueOf(caseDetails.getCaseType());
    if (!(caseType == CaseType.CE || caseType == CaseType.HH || caseType == CaseType.SPG)) {
      throw new CTPException(Fault.BAD_REQUEST, "Case type must be SPG, CE or HH");
    }

    UUID individualCaseId = createIndividualCaseId(caseType, caseDetails, requestParamsDTO);

    // Get RM to allocate a new questionnaire ID
    log.info("Before new QID");
    boolean individual = requestParamsDTO.getIndividual();
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto =
        caseServiceClient.getSingleUseQuestionnaireId(caseId, individual, individualCaseId);
    String questionnaireId = newQuestionnaireIdDto.getQuestionnaireId();
    String formType = newQuestionnaireIdDto.getFormType();
    log.with("newQuestionnaireID", questionnaireId)
        .with("formType", formType)
        .info("Have generated new questionnaireId");

    if (caseType == CaseType.CE && !individual && "CE".contentEquals(formType)) {
      rejectInvalidLaunchCombinations(caseDetails.getRegion(), caseDetails.getAddressLevel());
    }

    String eqUrl = createLaunchUrl(formType, caseDetails, requestParamsDTO, questionnaireId);
    publishSurveyLaunchedEvent(caseDetails.getId(), questionnaireId, requestParamsDTO.getAgentId());
    return eqUrl;
  }

  /**
   * Get the Case for which the client has requested a launch URL
   *
   * @param caseId of case to get
   * @return CaseContainerDTO for case requested
   * @throws CTPException if case not available to call (not in case service), but new skeleton case
   *     details available
   */
  private CaseContainerDTO getLaunchCase(UUID caseId) throws CTPException {
    try {
      CaseContainerDTO caseDetails = caseServiceClient.getCaseById(caseId, false);
      return caseDetails;
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        Optional<CachedCase> cachedCase = dataRepo.readCachedCaseById(caseId);
        if (cachedCase.isPresent()) {
          log.with("caseid", caseId)
              .with("status", ex.getStatus())
              .with("message", ex.getMessage())
              .warn("New skeleton case created but launch URL not available.");
          throw new CTPException(
              Fault.ACCEPTED_UNABLE_TO_PROCESS,
              "Unable to provide launch URL at present, please try again later.");
        }
      }
      log.with("caseid", caseId)
          .with("status", ex.getStatus())
          .with("message", ex.getMessage())
          .error("Unable to provide launch URL, failed to call case service");
      throw ex;
    }
  }

  private void rejectInvalidLaunchCombinations(String region, String addressLevel)
      throws CTPException {
    if ("E".equals(addressLevel)) {
      if ("N".equals(region)) {
        throw new CTPException(Fault.BAD_REQUEST, NI_LAUNCH_ERR_MSG);
      }
    } else if ("U".equals(addressLevel)) {
      if (VALID_REGIONS.contains(region)) {
        throw new CTPException(Fault.BAD_REQUEST, UNIT_LAUNCH_ERR_MSG);
      }
    }
  }

  // Create a new case if for a HH individual
  private UUID createIndividualCaseId(
      CaseType caseType, CaseContainerDTO caseDetails, LaunchRequestDTO requestParamsDTO) {
    boolean individual = requestParamsDTO.getIndividual();
    UUID individualCaseId = null;
    if (caseType == CaseType.HH && individual) {
      individualCaseId = UUID.randomUUID();
      caseDetails.setId(individualCaseId);
      caseDetails.setCaseType(CaseType.HI.name());
      log.with("individualCaseId", individualCaseId).info("Creating new HI case");
    }
    return individualCaseId;
  }

  private String createLaunchUrl(
      String formType,
      CaseContainerDTO caseDetails,
      LaunchRequestDTO requestParamsDTO,
      String questionnaireId)
      throws CTPException {
    String encryptedPayload = "";
    try {
      encryptedPayload =
          eqLaunchService.getEqLaunchJwe(
              Language.ENGLISH,
              uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API,
              uk.gov.ons.ctp.common.domain.Channel.CC,
              caseDetails,
              requestParamsDTO.getAgentId(),
              questionnaireId,
              formType,
              null,
              null,
              appConfig.getKeystore());
    } catch (CTPException e) {
      log.with(e).error("Failed to create JWE payload for eq launch");
      throw e;
    }
    String eqUrl =
        "https://" + appConfig.getEq().getHost() + "/en/start/launch-eq?token=" + encryptedPayload;
    log.with("launchURL", eqUrl).debug("Have created launch URL");
    return eqUrl;
  }

  // will throw exception if case does not exist.
  private void verifyCaseExists(UUID caseId) {
    caseServiceClient.getCaseById(caseId, false);
  }

  @Override
  public ResponseDTO invalidateCase(InvalidateCaseRequestDTO invalidateCaseRequestDTO)
      throws CTPException {
    UUID caseId = invalidateCaseRequestDTO.getCaseId();

    log.with("caseId", caseId)
        .with("status", invalidateCaseRequestDTO.getStatus())
        .debug("Invalidate Case");

    verifyCaseExists(caseId);

    CollectionCaseCompact collectionCase = new CollectionCaseCompact(caseId);

    log.debug("Case invalidated: publishing AddressNotValid event");
    AddressNotValid payload =
        AddressNotValid.builder()
            .collectionCase(collectionCase)
            .notes(invalidateCaseRequestDTO.getNotes())
            .reason(invalidateCaseRequestDTO.getStatus().name())
            .build();

    eventPublisher.sendEvent(
        EventType.ADDRESS_NOT_VALID, Source.CONTACT_CENTRE_API, appConfig.getChannel(), payload);
    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    log.with(response).debug("Return from invalidate case");
    return response;
  }

  private void publishSurveyLaunchedEvent(UUID caseId, String questionnaireId, String agentId) {
    log.with("questionnaireId", questionnaireId)
        .with("caseId", caseId)
        .with("agentId", agentId)
        .info("Generating SurveyLaunched event");

    SurveyLaunchedResponse response =
        SurveyLaunchedResponse.builder()
            .questionnaireId(questionnaireId)
            .caseId(caseId)
            .agentId(agentId)
            .build();

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.CONTACT_CENTRE_API, appConfig.getChannel(), response);

    log.with("caseId", response.getCaseId())
        .with("transactionId", transactionId)
        .debug("SurveyLaunch event published");
  }

  private void publishNewAddressReportedEvent(
      UUID caseId,
      CaseType caseType,
      String organisationName,
      Integer ceExpectedCapacity,
      AddressIndexAddressCompositeDTO address)
      throws CTPException {
    log.with("caseId", caseId.toString()).info("Generating NewAddressReported event");

    CollectionCaseNewAddress newAddress =
        caseDTOMapper.map(address, CollectionCaseNewAddress.class);
    newAddress.setId(caseId.toString());
    newAddress.setCaseType(caseType.name());
    newAddress.setSurvey(appConfig.getSurveyName());
    newAddress.setCollectionExerciseId(appConfig.getCollectionExerciseId());
    newAddress.setOrganisationName(organisationName);
    newAddress.setCeExpectedCapacity(ceExpectedCapacity);

    EstabType aimsEstabType = EstabType.forCode(newAddress.getAddress().getEstabType());
    Optional<AddressType> addressTypeMaybe = aimsEstabType.getAddressType();

    AddressType addressType =
        addressTypeMaybe.isPresent()
            ? addressTypeMaybe.get()
            : AddressType.valueOf(address.getCensusAddressType());
    if (addressType == AddressType.HH || addressType == AddressType.SPG) {
      newAddress.getAddress().setAddressLevel(AddressLevel.U.name());
    } else {
      newAddress.getAddress().setAddressLevel(AddressLevel.E.name());
    }

    NewAddress payload = new NewAddress();
    payload.setCollectionCase(newAddress);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.NEW_ADDRESS_REPORTED,
            Source.CONTACT_CENTRE_API,
            appConfig.getChannel(),
            payload);

    log.with("caseId", payload.getCollectionCase().getId())
        .with("transactionId", transactionId)
        .debug("NewAddressReported event published");
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

  /**
   * create a contact centre fulfilment request event
   *
   * @param fulfilmentCode the code for the product requested
   * @param deliveryChannel how the fulfilment should be delivered
   * @param caseId the id of the household,CE or SPG case the fulfilment is for
   * @return the request event to be delivered to the events exchange
   * @throws CTPException the requested product is invalid for the parameters given
   */
  private FulfilmentRequest createFulfilmentRequestPayload(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel, UUID caseId, Contact contact)
      throws CTPException {
    log.with(fulfilmentCode)
        .debug("Entering createFulfilmentEvent method in class CaseServiceImpl");

    CaseContainerDTO caze = retrieveCaseById(caseId, false);

    Region region = Region.valueOf(caze.getRegion().substring(0, 1));
    Product product = findProduct(fulfilmentCode, deliveryChannel, region);

    if (deliveryChannel == Product.DeliveryChannel.POST) {
      if (caze.isHandDelivery()) {
        log.info("Postal fulfilments cannot be delivered to this respondent");
        throw new CTPException(
            Fault.BAD_REQUEST, "Postal fulfilments cannot be delivered to this respondent");
      }
      if (product.getIndividual()) {
        if (StringUtils.isBlank(contact.getTitle())
            || StringUtils.isBlank(contact.getForename())
            || StringUtils.isBlank(contact.getSurname())) {

          log.warn("Individual fields are required for the requested fulfilment");
          throw new CTPException(
              Fault.BAD_REQUEST,
              "The fulfilment is for an individual so none of the following fields can be empty: "
                  + "'title', 'forename' and 'surname'");
        }
      }
    }

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    // create a new indiv id only if the parent case is an HH and the product requested is for an
    // indiv
    // SPG and CE indiv product requests do not need an indiv id creating
    if (CaseType.HH.name().equals(caze.getCaseType()) && product.getIndividual()) {
      fulfilmentRequest.setIndividualCaseId(UUID.randomUUID().toString());
    }

    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);
    fulfilmentRequest.setAddress(caseDTOMapper.map(caze, Address.class));

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
      log.with("searchCriteria", searchCriteria).warn("Compatible product cannot be found");
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    return products.get(0);
  }

  /**
   * Return a case by Id, if Not Found calling Case Service query repository for new skeleton cached
   * case
   *
   * @param caseId for which to request case
   * @param getCaseEvents should be set to true if the caller wants case events returned.
   * @return the requested case
   * @throws CTPException if case Not Found
   */
  private CaseContainerDTO retrieveCaseById(UUID caseId, boolean getCaseEvents)
      throws CTPException {

    CaseContainerDTO caze = null;
    try {
      caze = caseServiceClient.getCaseById(caseId, getCaseEvents);
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        log.with("caseId", caseId).debug("Fulfilment case Id Not Found calling Case Service");
        Optional<CachedCase> cachedCase = dataRepo.readCachedCaseById(caseId);
        if (cachedCase.isPresent()) {
          log.with("caseId", caseId).debug("Fulfilment using stored case details");
          caze = caseDTOMapper.map(cachedCase.get(), CaseContainerDTO.class);
        } else {
          log.with("caseId", caseId).warn("Fulfilment request Not Found");
          throw new CTPException(
              Fault.RESOURCE_NOT_FOUND, "Case Id Not Found: " + caseId.toString());
        }
      } else {
        log.with("caseId", caseId)
            .with("status", ex.getStatus())
            .error("Error calling Case Service");
        throw ex;
      }
    }
    return caze;
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
    refusal.setType(mapToType(refusalRequest.getReason()));
    refusal.setReport(refusalRequest.getNotes());
    CollectionCaseCompact collectionCase = new CollectionCaseCompact(caseId);
    refusal.setCollectionCase(collectionCase);
    refusal.setAgentId(refusalRequest.getAgentId());
    refusal.setCallId(refusalRequest.getCallId());

    // Populate contact
    Contact contact = new Contact();
    contact.setTitle(refusalRequest.getTitle());
    contact.setForename(refusalRequest.getForename());
    contact.setSurname(refusalRequest.getSurname());
    contact.setTelNo(refusalRequest.getTelNo());
    refusal.setContact(contact);

    // Populate address
    AddressCompact address = new AddressCompact();
    address.setAddressLine1(refusalRequest.getAddressLine1());
    address.setAddressLine2(refusalRequest.getAddressLine2());
    address.setAddressLine3(refusalRequest.getAddressLine3());
    address.setTownName(refusalRequest.getTownName());
    address.setPostcode(refusalRequest.getPostcode());
    address.setRegion(refusalRequest.getRegion().name());
    address.setUprn(Long.toString(refusalRequest.getUprn().getValue()));
    refusal.setAddress(address);

    return refusal;
  }

  private String mapToType(Reason reason) throws CTPException {
    switch (reason) {
      case HARD:
        return "HARD_REFUSAL";
      case EXTRAORDINARY:
        return "EXTRAORDINARY_REFUSAL";
      default:
        throw new CTPException(Fault.SYSTEM_ERROR, "Unexpected refusal reason: %s", reason);
    }
  }

  /**
   * Make Case Service request to return cases by UPRN
   *
   * @param uprn of requested cases
   * @param listCaseEvents boolean of whether require case events
   * @return List of cases for UPRN
   * @throws CTPException
   */
  private List<CaseDTO> callCaseSvcByUPRN(Long uprn, Boolean listCaseEvents) throws CTPException {

    List<CaseContainerDTO> rmCases = new ArrayList<>();
    try {
      rmCases = caseServiceClient.getCaseByUprn(uprn, listCaseEvents);
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        log.with(uprn).info("Case by UPRN Not Found calling Case Service");
        return Collections.emptyList();
      } else {
        log.with(uprn).with("status", ex.getStatus()).error("Error calling Case Service");
        throw ex;
      }
    }

    // Only return cases that are not of caseType = HI
    List<CaseContainerDTO> casesToReturn =
        (List<CaseContainerDTO>)
            rmCases
                .stream()
                .filter(c -> !(c.getCaseType().equals(CaseType.HI.name())))
                .collect(Collectors.toList());

    // Convert from Case service to Contact Centre DTOs
    List<CaseDTO> caseServiceResponse = mapCaseContainerDTOList(casesToReturn);

    // Clean up the events before returning them
    caseServiceResponse.stream().forEach(c -> filterCaseEvents(c, listCaseEvents));
    return caseServiceResponse;
  }

  /**
   * Create new skeleton case, publish new address reported event and store new case in repository
   * cache.
   *
   * @param uprn for address
   * @return CachedCase details of created skeleton case
   * @throws CTPException
   */
  private CachedCase createNewCachedCase(Long uprn) throws CTPException {

    // Query AIMS for UPRN
    AddressIndexAddressCompositeDTO address = addressSvc.uprnQuery(uprn);

    if (SCOTLAND_COUNTRY_CODE.equals(address.getCountryCode())) {
      log.with("uprn", uprn)
          .with("countryCode", address.getCountryCode())
          .warn("Scottish address retrieved");
      throw new CTPException(Fault.VALIDATION_FAILED, "Scottish address found for UPRN: " + uprn);
    }

    // Validate address type
    try {
      AddressType.valueOf(address.getCensusAddressType());
    } catch (IllegalArgumentException e) {
      log.with("uprn", uprn)
          .with("AddressType", address.getCensusAddressType())
          .warn("AIMs AddressType not valid");
      throw new CTPException(
          Fault.RESOURCE_NOT_FOUND,
          e,
          "AddressType of '%s' not valid for Census",
          address.getCensusAddressType());
    }

    CachedCase cachedCase = caseDTOMapper.map(address, CachedCase.class);
    cachedCase.setCaseType(CaseType.valueOf(address.getCensusAddressType()));

    UUID newCaseId = UUID.randomUUID();
    cachedCase.setId(newCaseId.toString());
    cachedCase.setCreatedDateTime(DateTimeUtil.nowUTC());

    publishNewAddressReportedEvent(newCaseId, cachedCase.getCaseType(), null, 0, address);

    dataRepo.writeCachedCase(cachedCase);
    return cachedCase;
  }

  private CaseDTO createNewCachedCaseResponse(CachedCase newCase) throws CTPException {

    CaseDTO response = caseDTOMapper.map(newCase, CaseDTO.class);
    response.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));

    EstabType estabType = EstabType.forCode(newCase.getEstabType());
    response.setEstabType(estabType);
    response.setSecureEstablishment(estabType.isSecure());

    return response;
  }
}
