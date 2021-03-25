package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static java.util.stream.Collectors.toList;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.AddressTypeChanged;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.ContactCompact;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSPostcodesBean;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexSearchResultsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpEncrypt;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);
  private static final String NI_LAUNCH_ERR_MSG =
      "All Northern Ireland calls from CE Managers are to be escalated to the NI management team.";
  private static final String UNIT_LAUNCH_ERR_MSG =
      "A CE Manager form can only be launched against an establishment address not a UNIT.";
  private static final String CANNOT_LAUNCH_CCS_CASE_FOR_CE_MSG =
      "Telephone capture feature is not available for CCS Communal establishment's. "
          + "CCS CE's must submit their survey via CCS Paper Questionnaire";
  private static final String CCS_CASE_ERROR_MSG = "Operation not permissible for a CCS Case";
  private static final String ESTAB_TYPE_OTHER_ERROR_MSG =
      "The pre-existing Establishment Type cannot be changed to OTHER";
  private static final List<DeliveryChannel> ALL_DELIVERY_CHANNELS =
      List.of(DeliveryChannel.POST, DeliveryChannel.SMS);

  private static final String SCOTLAND_COUNTRY_CODE = "S";

  @Autowired private AppConfig appConfig;

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Autowired private ProductReference productReference;

  private MapperFacade caseDTOMapper = new CCSvcBeanMapper();

  @Autowired private EqLaunchService eqLaunchService;

  @Autowired private CaseDataRepository dataRepo;

  @Autowired private AddressService addressSvc;

  @Autowired private EventPublisher eventPublisher;

  @Autowired private CCSPostcodesBean ccsPostcodesBean;

  @Inject
  @Qualifier("addressIndexClient")
  private RestClient addressIndexClient;

  private LuhnCheckDigit luhnChecker = new LuhnCheckDigit();

  public ResponseDTO fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    if (log.isDebugEnabled()) {
      log.with(requestBodyDTO)
          .debug("Now in the fulfilmentRequestByPost method in class CaseServiceImpl.");
    }

    verifyFulfilmentCodeNotBlackListed(requestBodyDTO.getFulfilmentCode());

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());

    FulfilmentRequest fulfilmentRequestPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), Product.DeliveryChannel.POST, caseId, contact);

    sendEvent(EventType.FULFILMENT_REQUESTED, fulfilmentRequestPayload, caseId);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.with(response)
          .debug("Now returning from the fulfilmentRequestByPost method in class CaseServiceImpl.");
    }

    return response;
  }

  public ResponseDTO fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.with(requestBodyDTO)
          .debug("Now in the fulfilmentRequestBySMS method in class CaseServiceImpl.");
    }

    verifyFulfilmentCodeNotBlackListed(requestBodyDTO.getFulfilmentCode());

    UUID caseId = requestBodyDTO.getCaseId();

    Contact contact = new Contact();
    contact.setTelNo(requestBodyDTO.getTelNo());

    FulfilmentRequest fulfilmentRequestedPayload =
        createFulfilmentRequestPayload(
            requestBodyDTO.getFulfilmentCode(), Product.DeliveryChannel.SMS, caseId, contact);
    sendEvent(EventType.FULFILMENT_REQUESTED, fulfilmentRequestedPayload, caseId);

    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.with(response)
          .debug("Now returning from the fulfilmentRequestBySMS method in class CaseServiceImpl.");
    }

    return response;
  }

  @Override
  public CaseDTO createCaseForNewAddress(NewCaseRequestDTO caseRequestDTO) throws CTPException {
    CaseType caseType = caseRequestDTO.getCaseType();

    String errorMessage =
        "All queries relating to Communal Establishments in Northern Ireland "
            + "should be escalated to NISRA HQ";
    rejectIfCEInNI(caseType, caseRequestDTO.getRegion(), errorMessage);

    validateCompatibleEstabAndCaseType(caseType, caseRequestDTO.getEstabType());

    rejectIfForCrownDependency(caseRequestDTO.getPostcode());

    uk.gov.ons.ctp.integration.contactcentresvc.representation.Region actualRegion =
        determineActualRegion(caseRequestDTO);
    caseRequestDTO.setRegion(actualRegion);

    // Reject if CE with non-positive number of residents
    if (caseRequestDTO.getCaseType() == CaseType.CE) {
      if (caseRequestDTO.getCeUsualResidents() == null
          || caseRequestDTO.getCeUsualResidents() <= 0) {
        throw new CTPException(
            Fault.BAD_REQUEST, "Number of residents must be supplied for CE case");
      }
    } else {
      // Field not relevant. Clear incase it's a silly number
      caseRequestDTO.setCeUsualResidents(0);
    }
    String addressType = caseType.name();

    // Create new case
    CachedCase cachedCase = caseDTOMapper.map(caseRequestDTO, CachedCase.class);
    UUID newCaseId = UUID.randomUUID();
    cachedCase.setId(newCaseId.toString());
    cachedCase.setEstabType(caseRequestDTO.getEstabType().getCode());
    cachedCase.setAddressType(addressType);
    cachedCase.setCreatedDateTime(DateTimeUtil.nowUTC());

    dataRepo.writeCachedCase(cachedCase);

    // Publish NewAddress event
    AddressIndexAddressCompositeDTO address =
        caseDTOMapper.map(caseRequestDTO, AddressIndexAddressCompositeDTO.class);
    address.setCensusAddressType(addressType);
    address.setCensusEstabType(caseRequestDTO.getEstabType().getCode());
    address.setCountryCode(actualRegion.name());
    publishNewAddressReportedEvent(
        newCaseId, caseType, caseRequestDTO.getCeUsualResidents(), address);

    return createNewCachedCaseResponse(cachedCase, false);
  }

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.debug("Fetching case details by caseId: {}", caseId);
    }

    // Get the case details from the case service, or failing that from the cache
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseDTO caseServiceResponse = getLatestCaseById(caseId, getCaseEvents);
    rejectHouseholdIndividual(caseServiceResponse);

    if (log.isDebugEnabled()) {
      log.with("caseId", caseId).debug("Returning case details for caseId");
    }

    return caseServiceResponse;
  }

  @Override
  public List<CaseDTO> getCaseByUPRN(
      UniquePropertyReferenceNumber uprn, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.with("uprn", uprn).debug("Fetching latest case details by UPRN");
    }
    Optional<CaseDTO> latest = getLatestCaseByUprn(uprn, requestParamsDTO.getCaseEvents());

    CaseDTO response;
    if (latest.isPresent()) {
      response = latest.get();
    } else {
      // New Case
      CachedCase newcase = createNewCachedCase(uprn.getValue());
      if (log.isDebugEnabled()) {
        log.with("uprn", uprn)
            .with("caseId", newcase.getId())
            .debug("Returning new skeleton case for UPRN");
      }
      response = createNewCachedCaseResponse(newcase, false);
    }
    return Collections.singletonList(response);
  }

  @Override
  public List<CaseDTO> getCCSCaseByPostcode(String postcode) throws CTPException {
    if (log.isDebugEnabled()) {
      log.with("postcode", postcode).debug("Fetching ccs case details by postcode");
    }
    validatePostcode(postcode);

    List<CaseContainerDTO> ccsCaseContainersList = getCcsCasesFromRm(postcode);

    List<CaseDTO> ccsCases = new ArrayList<CaseDTO>();
    for (CaseContainerDTO ccsCaseDetails : ccsCaseContainersList) {
      ccsCases.add(caseDTOMapper.map(ccsCaseDetails, CaseDTO.class));
    }

    return ccsCases;
  }

  @Override
  public CaseDTO getCaseByCaseReference(final long caseRef, CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.with("caseRef", caseRef).debug("Fetching case details by case reference");
    }

    validateCaseRef(caseRef);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = getCaseFromRm(caseRef, getCaseEvents);
    CaseDTO caseServiceResponse = mapCaseContainerDTO(caseDetails);
    rejectHouseholdIndividual(caseServiceResponse);
    if (log.isDebugEnabled()) {
      log.with("caseRef", caseRef).debug("Returning case details for case reference");
    }

    return caseServiceResponse;
  }

  @Override
  public CaseDTO modifyCase(ModifyCaseRequestDTO modifyRequestDTO) throws CTPException {
    validateCompatibleEstabAndCaseType(
        modifyRequestDTO.getCaseType(), modifyRequestDTO.getEstabType());
    UUID originalCaseId = modifyRequestDTO.getCaseId();
    UUID caseId = originalCaseId;

    CaseContainerDTO caseDetails = getCaseFromRmOrCache(originalCaseId, true);

    if (modifyRequestDTO.getEstabType() == EstabType.OTHER
        && EstabType.forCode(caseDetails.getEstabType()) != EstabType.OTHER) {
      throw new CTPException(Fault.BAD_REQUEST, ESTAB_TYPE_OTHER_ERROR_MSG);
    }

    validateSurveyType(caseDetails);
    caseDetails.setCreatedDateTime(DateTimeUtil.nowUTC());
    CaseType requestedCaseType = modifyRequestDTO.getCaseType();
    CaseType existingCaseType = CaseType.valueOf(caseDetails.getCaseType());

    boolean caseTypeChanged = isCaseTypeChange(requestedCaseType, existingCaseType);

    CaseDTO response = caseDTOMapper.map(caseDetails, CaseDTO.class);
    String caseRef = caseDetails.getCaseRef();

    rejectHouseholdIndividual(response);

    if (caseTypeChanged) {
      rejectNorthernIrelandHouseholdToCE(requestedCaseType, caseDetails);
      caseId = UUID.randomUUID();
      sendAddressTypeChangedEvent(caseId, originalCaseId, modifyRequestDTO);
      caseRef = null;
    } else {
      sendAddressModifiedEvent(originalCaseId, modifyRequestDTO, caseDetails);
    }
    updateOrCreateCachedCase(caseId, caseDetails, modifyRequestDTO);
    prepareModificationResponse(response, modifyRequestDTO, caseId, caseRef);
    return response;
  }

  @Override
  public ResponseDTO reportRefusal(UUID caseId, RefusalRequestDTO requestBodyDTO)
      throws CTPException {
    String reportedDateTime = "null";
    if (requestBodyDTO.getDateTime() != null) {
      reportedDateTime = DateTimeUtil.formatDate(requestBodyDTO.getDateTime());
    }
    if (log.isDebugEnabled()) {
      log.with("caseId", caseId)
          .with("reportedDateTime", reportedDateTime)
          .debug("Processing refusal for case with reported dateTime");
    }

    // Create and publish a respondent refusal event
    RespondentRefusalDetails refusalPayload =
        createRespondentRefusalPayload(caseId, requestBodyDTO);

    sendEvent(EventType.REFUSAL_RECEIVED, refusalPayload, caseId);

    // Build response
    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.with("caseId", caseId).debug("Returning refusal response for case");
    }

    return response;
  }

  @Override
  public String getLaunchURLForCaseId(final UUID caseId, LaunchRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.with("caseId", caseId)
          .with("request", requestParamsDTO)
          .debug("Processing request to create launch URL");
    }

    CaseContainerDTO caseDetails = getLaunchCase(caseId);

    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto =
        getNewQidForCase(caseDetails, requestParamsDTO.getIndividual());

    String questionnaireId = newQuestionnaireIdDto.getQuestionnaireId();
    String formType = newQuestionnaireIdDto.getFormType();

    String eqUrl = createLaunchUrl(formType, caseDetails, requestParamsDTO, questionnaireId);
    publishSurveyLaunchedEvent(caseDetails.getId(), questionnaireId, requestParamsDTO.getAgentId());
    return eqUrl;
  }

  @Override
  public UACResponseDTO getUACForCaseId(UUID caseId, UACRequestDTO requestParamsDTO)
      throws CTPException {
    if (log.isDebugEnabled()) {
      log.with("caseId", caseId)
          .with("request", requestParamsDTO)
          .debug("Processing request to get UAC for Case");
    }

    CaseContainerDTO caseDetails = getLaunchCase(caseId);

    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto =
        getNewQidForCase(caseDetails, requestParamsDTO.getIndividual());

    return UACResponseDTO.builder()
        .id(newQuestionnaireIdDto.getQuestionnaireId())
        .uac(newQuestionnaireIdDto.getUac())
        .dateTime(DateTimeUtil.nowUTC())
        .build();
  }

  @Override
  public ResponseDTO invalidateCase(InvalidateCaseRequestDTO invalidateCaseRequestDTO)
      throws CTPException {
    UUID caseId = invalidateCaseRequestDTO.getCaseId();

    if (log.isDebugEnabled()) {
      log.with("caseId", caseId)
          .with("status", invalidateCaseRequestDTO.getStatus())
          .debug("Invalidate Case");
    }

    CaseContainerDTO caseDetails = getCaseFromRmOrCache(caseId, false);
    String errorMessage =
        "All CE addresses will be validated by a Field Officer. "
            + "It is not necessary to submit this Invalidation request.";
    CaseType caseType = CaseType.valueOf(caseDetails.getCaseType());
    rejectIfCaseIsTypeCE(caseType, errorMessage);

    CollectionCaseCompact collectionCase = new CollectionCaseCompact(caseId);

    if (log.isDebugEnabled()) {
      log.debug("Case invalidated: publishing AddressNotValid event");
    }
    AddressNotValid payload =
        AddressNotValid.builder()
            .collectionCase(collectionCase)
            .notes(invalidateCaseRequestDTO.getNotes())
            .reason(invalidateCaseRequestDTO.getStatus().name())
            .build();

    sendEvent(EventType.ADDRESS_NOT_VALID, payload, caseId);
    ResponseDTO response =
        ResponseDTO.builder().id(caseId.toString()).dateTime(DateTimeUtil.nowUTC()).build();

    if (log.isDebugEnabled()) {
      log.with(response).debug("Return from invalidate case");
    }
    return response;
  }

  private void publishSurveyLaunchedEvent(UUID caseId, String questionnaireId, Integer agentId) {
    log.with("questionnaireId", questionnaireId)
        .with("caseId", caseId)
        .with("agentId", agentId)
        .info("Generating SurveyLaunched event");

    SurveyLaunchedResponse response =
        SurveyLaunchedResponse.builder()
            .questionnaireId(questionnaireId)
            .caseId(caseId)
            .agentId(Integer.toString(agentId))
            .build();

    sendEvent(EventType.SURVEY_LAUNCHED, response, response.getCaseId());
  }

  private void publishNewAddressReportedEvent(
      UUID caseId,
      CaseType caseType,
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
    newAddress.setCeExpectedCapacity(ceExpectedCapacity);

    Address addrDetails = newAddress.getAddress();
    EstabType aimsEstabType = EstabType.forCode(addrDetails.getEstabType());
    addrDetails.setEstabType(aimsEstabType.getCode());

    Optional<AddressType> addressTypeMaybe = aimsEstabType.getAddressType();

    AddressType addressType =
        addressTypeMaybe.isPresent()
            ? addressTypeMaybe.get()
            : AddressType.valueOf(address.getCensusAddressType());
    if (addressType == AddressType.HH || addressType == AddressType.SPG) {
      addrDetails.setAddressLevel(AddressLevel.U.name());
    } else {
      addrDetails.setAddressLevel(AddressLevel.E.name());
    }

    NewAddress payload = new NewAddress();
    payload.setCollectionCase(newAddress);

    sendEvent(EventType.NEW_ADDRESS_REPORTED, payload, payload.getCollectionCase().getId());
  }

  private CaseContainerDTO filterCaseEvents(CaseContainerDTO caseDTO, Boolean getCaseEvents) {
    if (getCaseEvents) {
      // Only return whitelisted events
      Set<String> whitelistedEventCategories =
          appConfig.getCaseServiceSettings().getWhitelistedEventCategories();
      List<EventDTO> filteredEvents =
          caseDTO.getCaseEvents().stream()
              .filter(e -> whitelistedEventCategories.contains(e.getEventType()))
              .collect(toList());
      caseDTO.setCaseEvents(filteredEvents);
    } else {
      // Caller doesn't want any event data
      caseDTO.setCaseEvents(Collections.emptyList());
    }
    return caseDTO;
  }

  private Region convertRegion(CaseContainerDTO caseDetails) {
    return Region.valueOf(caseDetails.getRegion().substring(0, 1).toUpperCase());
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
    if (log.isDebugEnabled()) {
      log.with(fulfilmentCode)
          .debug("Entering createFulfilmentEvent method in class CaseServiceImpl");
    }

    CaseContainerDTO caze = getCaseFromRmOrCache(caseId, false);
    validateSurveyType(caze);
    Product product = findProduct(fulfilmentCode, deliveryChannel, convertRegion(caze));

    if (deliveryChannel == Product.DeliveryChannel.POST) {
      if (product.getIndividual()) {
        if (StringUtils.isBlank(contact.getForename())
            || StringUtils.isBlank(contact.getSurname())) {

          log.with("fulfilmentCode", fulfilmentCode)
              .warn("Individual fields are required for the requested fulfilment");
          throw new CTPException(
              Fault.BAD_REQUEST,
              "The fulfilment is for an individual so none of the following fields can be empty: "
                  + "'forename' or 'surname'");
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
    if (log.isDebugEnabled()) {
      log.with(fulfilmentCode)
          .with(deliveryChannel)
          .with(region)
          .debug("Passing fulfilmentCode, deliveryChannel, and region, into findProduct method.");
    }
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
  private CaseContainerDTO getCaseFromRmOrCache(UUID caseId, boolean getCaseEvents)
      throws CTPException {

    CaseContainerDTO caze = null;
    try {
      caze = getCaseFromRm(caseId, getCaseEvents);
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        if (log.isDebugEnabled()) {
          log.with("caseId", caseId).debug("Case Id Not Found calling Case Service");
        }
        Optional<CachedCase> cachedCase = dataRepo.readCachedCaseById(caseId);
        if (cachedCase.isPresent()) {
          log.with("caseId", caseId).info("Using stored case details");
          caze = caseDTOMapper.map(cachedCase.get(), CaseContainerDTO.class);
          caze.setSurveyType(appConfig.getSurveyName());
        } else {
          log.with("caseId", caseId).warn("Request for case Not Found");
          throw new CTPException(
              Fault.RESOURCE_NOT_FOUND, "Case Id Not Found: " + caseId.toString());
        }
      } else {
        log.with("caseId", caseId).error("Error calling Case Service", ex);
        throw ex;
      }
    }
    return caze;
  }

  private CaseContainerDTO getCaseFromRm(UUID caseId, boolean getCaseEvents) {
    CaseContainerDTO caseDetails = caseServiceClient.getCaseById(caseId, getCaseEvents);
    return filterCaseEvents(caseDetails, getCaseEvents);
  }

  private CaseContainerDTO getCaseFromRm(long caseRef, boolean getCaseEvents) {
    CaseContainerDTO caseDetails = caseServiceClient.getCaseByCaseRef(caseRef, getCaseEvents);
    return filterCaseEvents(caseDetails, getCaseEvents);
  }

  private List<CaseContainerDTO> getCasesFromRm(long uprn, boolean getCaseEvents) {
    var caseList = caseServiceClient.getCaseByUprn(uprn, getCaseEvents);
    return caseList.stream().map(c -> filterCaseEvents(c, getCaseEvents)).collect(toList());
  }

  private List<CaseContainerDTO> getCcsCasesFromRm(String postcode) {
    List<CaseContainerDTO> caseList = caseServiceClient.getCcsCaseByPostcode(postcode);
    return caseList;
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
    CollectionCaseCompact collectionCase = new CollectionCaseCompact(caseId);
    refusal.setCollectionCase(collectionCase);
    refusal.setAgentId(Integer.toString(refusalRequest.getAgentId()));
    refusal.setCallId(refusalRequest.getCallId());
    refusal.setHouseholder(refusalRequest.getIsHouseholder());

    // Populate contact
    ContactCompact contact = createRefusalContact(refusalRequest);
    refusal.setContact(contact);

    // Populate address
    AddressCompact address = new AddressCompact();
    address.setAddressLine1(refusalRequest.getAddressLine1());
    address.setAddressLine2(refusalRequest.getAddressLine2());
    address.setAddressLine3(refusalRequest.getAddressLine3());
    address.setTownName(refusalRequest.getTownName());
    address.setPostcode(refusalRequest.getPostcode());
    uk.gov.ons.ctp.integration.contactcentresvc.representation.Region region =
        refusalRequest.getRegion();
    if (region != null) {
      address.setRegion(region.name());
    }
    UniquePropertyReferenceNumber uprn = refusalRequest.getUprn();
    if (uprn != null) {
      address.setUprn(Long.toString(uprn.getValue()));
    }
    refusal.setAddress(address);

    return refusal;
  }

  private ContactCompact createRefusalContact(RefusalRequestDTO refusalRequest) {
    ContactCompact contact = null;

    if (refusalRequest.getReason() == Reason.HARD) {
      contact = new ContactCompact();
      contact.setTitle(encrypt(refusalRequest.getTitle()));
      contact.setForename(encrypt(refusalRequest.getForename()));
      contact.setSurname(encrypt(refusalRequest.getSurname()));
    }
    return contact;
  }

  private String encrypt(String clearValue) {
    if (clearValue == null) {
      return null;
    }
    List<Resource> keys = List.of(appConfig.getPublicPgpKey1(), appConfig.getPublicPgpKey2());
    String encStr = PgpEncrypt.encrypt(clearValue, keys);
    return Base64.getEncoder().encodeToString(encStr.getBytes(StandardCharsets.UTF_8));
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
      rmCases = getCasesFromRm(uprn, listCaseEvents);
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        log.with(uprn).info("Case by UPRN Not Found calling Case Service");
        return Collections.emptyList();
      } else {
        log.with("uprn", uprn).error("Error calling Case Service", ex);
        throw ex;
      }
    }

    // Only return cases that are not of caseType = HI
    List<CaseContainerDTO> casesToReturn =
        (List<CaseContainerDTO>)
            rmCases.stream()
                .filter(c -> !(c.getCaseType().equals(CaseType.HI.name())))
                .collect(toList());

    return mapCaseContainerDTOList(casesToReturn);
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

    publishNewAddressReportedEvent(newCaseId, cachedCase.getCaseType(), 0, address);

    dataRepo.writeCachedCase(cachedCase);
    return cachedCase;
  }

  private CaseDTO createNewCachedCaseResponse(CachedCase newCase, boolean caseEvents) {
    CaseDTO response = caseDTOMapper.map(newCase, CaseDTO.class);
    response.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));

    EstabType estabType = EstabType.forCode(newCase.getEstabType());
    response.setEstabType(estabType);
    response.setSecureEstablishment(estabType.isSecure());

    if (!caseEvents) {
      response.setCaseEvents(Collections.emptyList());
    }
    return response;
  }

  private void rejectIfCEInNI(
      CaseType caseType,
      uk.gov.ons.ctp.integration.contactcentresvc.representation.Region region,
      String errorMessage)
      throws CTPException {
    if (region == uk.gov.ons.ctp.integration.contactcentresvc.representation.Region.N) {
      rejectIfCaseIsTypeCE(caseType, errorMessage);
    }
  }

  private void rejectIfCaseIsTypeCE(CaseType caseType, String errorMessage) throws CTPException {
    if (caseType == CaseType.CE) {
      log.with("caseType", caseType.name()).warn(errorMessage);
      throw new CTPException(Fault.BAD_REQUEST, errorMessage);
    }
  }

  private void sendEvent(EventType eventType, EventPayload payload, Object caseId) {
    String transactionId =
        eventPublisher.sendEvent(
            eventType, Source.CONTACT_CENTRE_API, appConfig.getChannel(), payload);

    if (log.isDebugEnabled()) {
      log.with("caseId", caseId)
          .with("transactionId", transactionId)
          .debug("{} event published", eventType);
    }
  }

  private void validatePostcode(String postcode) throws CTPException {
    if (!ccsPostcodesBean.isInCCSPostcodes(postcode)) {
      log.with(postcode).info("Check failed for postcode");
      throw new CTPException(
          Fault.BAD_REQUEST, "The requested postcode is not within the CCS sample");
    }
  }

  private void rejectHouseholdIndividual(CaseDTO caseDetails) {
    if (caseDetails.getCaseType().equals(CaseType.HI.name())) {
      log.with(caseDetails.getId())
          .info("Case is not suitable as it is a household individual case");
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case is not suitable");
    }
  }

  private CaseDTO mapCaseContainerDTO(CaseContainerDTO caseDetails) {
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);
    return adaptCaseDTO(caseServiceResponse);
  }

  private List<CaseDTO> mapCaseContainerDTOList(List<CaseContainerDTO> casesToReturn) {
    List<CaseDTO> caseServiceListResponse = caseDTOMapper.mapAsList(casesToReturn, CaseDTO.class);
    for (CaseDTO caseServiceResponse : caseServiceListResponse) {
      adaptCaseDTO(caseServiceResponse);
    }
    return caseServiceListResponse;
  }

  private CaseDTO adaptCaseDTO(CaseDTO caseServiceResponse) {
    caseServiceResponse.setAllowedDeliveryChannels(ALL_DELIVERY_CHANNELS);
    if (null == caseServiceResponse.getEstabDescription()) {
      String caseType = caseServiceResponse.getCaseType();
      caseServiceResponse.setEstabType(
          CaseType.HH.name().equals(caseType) ? EstabType.HOUSEHOLD : EstabType.OTHER);
    } else {
      caseServiceResponse.setEstabType(
          EstabType.forCode(caseServiceResponse.getEstabDescription()));
    }
    return caseServiceResponse;
  }

  private CaseDTO getLatestCaseById(UUID caseId, Boolean getCaseEvents) throws CTPException {
    TimeOrderedCases timeOrderedCases = new TimeOrderedCases();

    try {
      CaseContainerDTO caseFromRM = getCaseFromRm(caseId, getCaseEvents);
      if (caseFromRM != null) {
        timeOrderedCases.addCase(mapCaseContainerDTO(caseFromRM));
      }
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        if (log.isDebugEnabled()) {
          log.with("caseId", caseId).debug("Case Id Not Found by Case Service");
        }
      } else {
        log.with("caseId", caseId).error("Error calling Case Service", ex);
        throw ex;
      }
    }

    Optional<CaseDTO> cachedCase =
        dataRepo
            .readCachedCaseById(caseId)
            .map(cc -> createNewCachedCaseResponse(cc, getCaseEvents));

    if (cachedCase.isPresent()) {
      timeOrderedCases.addCase(cachedCase.get());
    }
    Optional<CaseDTO> latest = timeOrderedCases.latest();

    CaseDTO latestCaseDto = null;
    if (latest.isPresent()) {
      latestCaseDto = latest.get();
    } else {
      log.with("caseId", caseId).warn("Request for case Not Found");
      throw new CTPException(Fault.RESOURCE_NOT_FOUND, "Case Id Not Found: " + caseId.toString());
    }

    return latestCaseDto;
  }

  private Optional<CaseDTO> getLatestCaseByUprn(
      UniquePropertyReferenceNumber uprn, boolean addCaseEvents) throws CTPException {
    TimeOrderedCases timeOrderedCases = new TimeOrderedCases();

    List<CaseDTO> rmCases = callCaseSvcByUPRN(uprn.getValue(), addCaseEvents);
    if (log.isDebugEnabled()) {
      log.with("uprn", uprn)
          .with("cases", rmCases.size())
          .debug("Found {} case details in RM for UPRN", rmCases.size());
    }
    timeOrderedCases.add(rmCases);

    List<CaseDTO> cachedCases =
        dataRepo.readCachedCasesByUprn(uprn).stream()
            .map(cc -> createNewCachedCaseResponse(cc, addCaseEvents))
            .collect(toList());
    if (log.isDebugEnabled()) {
      log.with("uprn", uprn)
          .with("cases", cachedCases.size())
          .debug("Found {} case details in Cache for UPRN", cachedCases.size());
    }
    timeOrderedCases.add(cachedCases);

    return timeOrderedCases.latest();
  }

  private void validateCaseRef(long caseRef) throws CTPException {
    if (!luhnChecker.isValid(Long.toString(caseRef))) {
      log.with(caseRef).info("Luhn check failed for case Reference");
      throw new CTPException(Fault.BAD_REQUEST, "Invalid Case Reference");
    }
  }

  private void validateCompatibleEstabAndCaseType(CaseType caseType, EstabType estabType)
      throws CTPException {
    Optional<AddressType> addrType = estabType.getAddressType();
    if (addrType.isPresent() && (caseType != CaseType.valueOf(addrType.get().name()))) {
      log.with("caseType", caseType)
          .with("estabType", estabType)
          .info("Mismatching caseType and estabType");
      String msg =
          "Derived address type of '"
              + addrType.get()
              + "', from establishment type '"
              + estabType
              + "', "
              + "is not compatible with caseType of '"
              + caseType
              + "'";
      throw new CTPException(Fault.BAD_REQUEST, msg);
    }
  }

  private boolean isCaseTypeChange(CaseType requestedCaseType, CaseType existingCaseType) {
    return requestedCaseType != existingCaseType;
  }

  private void rejectNorthernIrelandHouseholdToCE(
      CaseType requestedCaseType, CaseContainerDTO caseDetails) throws CTPException {
    Region region = convertRegion(caseDetails);
    if (region == Region.N && requestedCaseType == CaseType.CE) {
      AddressType addrType = AddressType.valueOf(caseDetails.getCaseType());
      if (addrType == AddressType.HH) {
        String msg =
            "All queries relating to Communal Establishments in Northern Ireland "
                + "should be escalated to NISRA HQ";
        log.with("caseType", requestedCaseType).with("caseDetails", caseDetails).info(msg);
        throw new CTPException(Fault.BAD_REQUEST, msg);
      }
    }
  }

  private void rejectIfForCrownDependency(String postcode) throws CTPException {
    String postcodeArea = postcode.substring(0, 2).toUpperCase();

    switch (postcodeArea) {
      case "GY":
      case "JE":
        log.with(postcode).info("Rejecting request as postcode is for a channel island address");
        throw new CTPException(
            Fault.BAD_REQUEST, "Channel Island addresses are not valid for Census");
      case "IM":
        log.with(postcode).info("Rejecting request as postcode is for an Isle of Man address");
        throw new CTPException(Fault.BAD_REQUEST, "Isle of Man addresses are not valid for Census");
      default: // to keep checkstyle happy
    }
  }

  private uk.gov.ons.ctp.integration.contactcentresvc.representation.Region determineActualRegion(
      NewCaseRequestDTO caseRequestDTO) throws CTPException {
    uk.gov.ons.ctp.integration.contactcentresvc.representation.Region sercoRegion =
        caseRequestDTO.getRegion();
    uk.gov.ons.ctp.integration.contactcentresvc.representation.Region ccRegion = null;

    String postcode = caseRequestDTO.getPostcode();
    String postcodeArea = postcode.substring(0, 2).toUpperCase();

    if (postcodeArea.equals("BT")) {
      ccRegion = uk.gov.ons.ctp.integration.contactcentresvc.representation.Region.N;

    } else {
      // Get ready to call AI to find the region for the specified postcode
      MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
      queryParams.add("offset", "0");
      queryParams.add("limit", "1");
      queryParams.add("includeauxiliarysearch", "true");
      addEpoch(queryParams);

      // Ask Address Index to do postcode search
      AddressIndexSearchResultsDTO addressIndexResponse = null;
      try {
        String path = appConfig.getAddressIndexSettings().getPostcodeLookupPath();
        addressIndexResponse =
            addressIndexClient.getResource(
                path, AddressIndexSearchResultsDTO.class, null, queryParams, postcode);
      } catch (ResponseStatusException e) {
        // Something went wrong calling AI.
        // Never mind, we'll still be able to use the Serco supplied region
        log.with("postcode", postcode).warn("Failed to call AI to resolve region");
      }

      if (addressIndexResponse != null) {
        ArrayList<AddressIndexAddressDTO> addresses =
            addressIndexResponse.getResponse().getAddresses();
        if (!addresses.isEmpty()) {
          // Found an address. Fail if Scottish otherwise use its region
          String countryCode = addresses.get(0).getCensus().getCountryCode();
          if (countryCode != null && countryCode.equals("S")) {
            log.with("postcode", postcode).info("Rejecting as it's a Scottish address");
            throw new CTPException(
                Fault.BAD_REQUEST, "Scottish addresses are not valid for Census");
          }

          if (countryCode != null) {
            // Use the region as specified by AI
            ccRegion =
                uk.gov.ons.ctp.integration.contactcentresvc.representation.Region.valueOf(
                    countryCode);
          }
        }
      }
    }

    // Decide if we are using the Serco or the CC calculated region
    uk.gov.ons.ctp.integration.contactcentresvc.representation.Region regionForNewCase = null;
    if (ccRegion == null) {
      // CC failed to find the region
      log.with("sercoRegion", sercoRegion)
          .info(
              "Unable to determine region for new case."
                  + " Falling back to using Serco provided region");
      regionForNewCase = sercoRegion;
    } else if (sercoRegion == ccRegion) {
      // CC agrees with Serco supplied region
      log.with("sercoRegion", sercoRegion)
          .with("ccRegion", ccRegion)
          .info("Using Serco provided region for new case");
      regionForNewCase = sercoRegion;
    } else {
      // CC and Serco differ, so override Serco region
      log.with("sercoRegion", sercoRegion)
          .with("ccRegion", ccRegion)
          .info("Overriding Serco region with cc region for new case");
      regionForNewCase = ccRegion;
    }

    return regionForNewCase;
  }

  private MultiValueMap<String, String> addEpoch(MultiValueMap<String, String> queryParams) {
    String epoch = appConfig.getAddressIndexSettings().getEpoch();
    if (!StringUtils.isBlank(epoch)) {
      queryParams.add("epoch", epoch);
    }
    return queryParams;
  }

  private void updateOrCreateCachedCase(
      UUID caseId, CaseContainerDTO caseDetails, ModifyCaseRequestDTO modifyRequestDTO)
      throws CTPException {
    CachedCase cachedCase = caseDTOMapper.map(caseDetails, CachedCase.class);
    cachedCase.setId(caseId.toString());
    CaseType caseType = modifyRequestDTO.getCaseType();
    cachedCase.setCaseType(caseType);
    cachedCase.setEstabType(modifyRequestDTO.getEstabType().getCode());
    cachedCase.setAddressType(caseType.name());
    cachedCase.setAddressLine1(modifyRequestDTO.getAddressLine1());
    cachedCase.setAddressLine2(modifyRequestDTO.getAddressLine2());
    cachedCase.setAddressLine3(modifyRequestDTO.getAddressLine3());
    cachedCase.setCeOrgName(modifyRequestDTO.getCeOrgName());
    dataRepo.writeCachedCase(cachedCase);
  }

  private void sendAddressModifiedEvent(
      UUID caseId, ModifyCaseRequestDTO modifyRequestDTO, CaseContainerDTO caseDetails) {
    CollectionCaseCompact collectionCase =
        CollectionCaseCompact.builder()
            .id(caseId)
            .caseType(modifyRequestDTO.getCaseType().name())
            .ceExpectedCapacity(modifyRequestDTO.getCeUsualResidents())
            .build();
    AddressCompact originalAddress = caseDTOMapper.map(caseDetails, AddressCompact.class);
    AddressCompact newAddress = caseDTOMapper.map(caseDetails, AddressCompact.class);

    newAddress.setAddressLine1(modifyRequestDTO.getAddressLine1());
    newAddress.setAddressLine2(modifyRequestDTO.getAddressLine2());
    newAddress.setAddressLine3(modifyRequestDTO.getAddressLine3());
    newAddress.setEstabType(modifyRequestDTO.getEstabType().getCode());
    newAddress.setOrganisationName(modifyRequestDTO.getCeOrgName());

    AddressModification payload =
        AddressModification.builder()
            .collectionCase(collectionCase)
            .originalAddress(originalAddress)
            .newAddress(newAddress)
            .build();
    sendEvent(EventType.ADDRESS_MODIFIED, payload, caseId);
  }

  private void sendAddressTypeChangedEvent(
      UUID newCaseId, UUID originalCaseId, ModifyCaseRequestDTO modifyRequestDTO) {
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId(originalCaseId.toString());
    collectionCase.setCeExpectedCapacity(modifyRequestDTO.getCeUsualResidents());
    collectionCase.setContact(null);

    Address address = new Address();
    address.setAddressLine1(modifyRequestDTO.getAddressLine1());
    address.setAddressLine2(modifyRequestDTO.getAddressLine2());
    address.setAddressLine3(modifyRequestDTO.getAddressLine3());
    address.setEstabType(modifyRequestDTO.getEstabType().getCode());
    address.setOrganisationName(modifyRequestDTO.getCeOrgName());
    address.setAddressType(modifyRequestDTO.getCaseType().name());

    collectionCase.setAddress(address);

    AddressTypeChanged payload =
        AddressTypeChanged.builder().newCaseId(newCaseId).collectionCase(collectionCase).build();
    sendEvent(EventType.ADDRESS_TYPE_CHANGED, payload, newCaseId);
  }

  private void prepareModificationResponse(
      CaseDTO response, ModifyCaseRequestDTO modifyRequestDTO, UUID caseId, String caseRef) {
    CaseType caseType = modifyRequestDTO.getCaseType();
    response.setId(caseId);
    response.setCaseRef(caseRef);
    response.setCaseType(caseType.name());
    response.setAddressType(caseType.name());
    EstabType estabType = modifyRequestDTO.getEstabType();
    response.setEstabType(estabType);
    response.setEstabDescription(estabType.getCode());
    response.setAddressLine1(modifyRequestDTO.getAddressLine1());
    response.setAddressLine2(modifyRequestDTO.getAddressLine2());
    response.setAddressLine3(modifyRequestDTO.getAddressLine3());
    response.setCeOrgName(modifyRequestDTO.getCeOrgName());
    response.setAllowedDeliveryChannels(ALL_DELIVERY_CHANNELS);
    response.setCaseEvents(Collections.emptyList());
  }

  private void validateSurveyType(CaseContainerDTO caseDetails) throws CTPException {
    if (!appConfig.getSurveyName().equalsIgnoreCase(caseDetails.getSurveyType())) {
      throw new CTPException(Fault.BAD_REQUEST, CCS_CASE_ERROR_MSG);
    }
  }

  /**
   * Request a new questionnaire Id for a Case
   *
   * @param caseDetails of case for which to get questionnaire Id
   * @param individual whether request for individual questionnaire
   * @return
   */
  private SingleUseQuestionnaireIdDTO getNewQidForCase(
      CaseContainerDTO caseDetails, boolean individual) throws CTPException {

    CaseType caseType = CaseType.valueOf(caseDetails.getCaseType());
    if (!(caseType == CaseType.CE || caseType == CaseType.HH || caseType == CaseType.SPG)) {
      throw new CTPException(Fault.BAD_REQUEST, "Case type must be SPG, CE or HH");
    }
    if (caseType == CaseType.CE) {
      if ("CCS".equalsIgnoreCase(caseDetails.getSurveyType())) {
        throw new CTPException(Fault.BAD_REQUEST, CANNOT_LAUNCH_CCS_CASE_FOR_CE_MSG);
      } else if (!individual && "U".equals(caseDetails.getAddressLevel())) {
        throw new CTPException(Fault.BAD_REQUEST, UNIT_LAUNCH_ERR_MSG);
      }
    }

    UUID parentCaseId = caseDetails.getId();
    UUID individualCaseId = null;
    if (caseType == CaseType.HH && individual) {
      caseDetails = createIndividualCase(caseDetails);
      individualCaseId = caseDetails.getId();
    }

    // Get RM to allocate a new questionnaire ID
    log.info("Before new QID");
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto;
    try {
      newQuestionnaireIdDto =
          caseServiceClient.getSingleUseQuestionnaireId(parentCaseId, individual, individualCaseId);
    } catch (ResponseStatusException ex) {
      if (ex.getCause() != null) {
        HttpStatusCodeException cause = (HttpStatusCodeException) ex.getCause();
        log.with("caseid", parentCaseId)
            .with("status", cause.getStatusCode())
            .with("message", cause.getMessage())
            .warn("New QID, UAC Case service request failure.");
        if (cause.getStatusCode() == HttpStatus.BAD_REQUEST) {
          throw new CTPException(
              Fault.BAD_REQUEST, "Invalid request for case %s", parentCaseId.toString());
        }
      }
      throw ex;
    }

    String questionnaireId = newQuestionnaireIdDto.getQuestionnaireId();
    String formType = newQuestionnaireIdDto.getFormType();
    log.with("newQuestionnaireID", questionnaireId)
        .with("formType", formType)
        .info("Have generated new questionnaireId");

    if (caseType == CaseType.CE) {
      rejectInvalidLaunchCombinationsForCE(caseDetails, individual, formType);
    }

    return newQuestionnaireIdDto;
  }

  /**
   * Get the Case for which the client has requested a launch URL/UAC
   *
   * @param caseId of case to get
   * @return CaseContainerDTO for case requested
   * @throws CTPException if case not available to call (not in case service), but new skeleton case
   *     details available
   */
  private CaseContainerDTO getLaunchCase(UUID caseId) throws CTPException {
    try {
      CaseContainerDTO caseDetails = getCaseFromRm(caseId, false);
      return caseDetails;
    } catch (ResponseStatusException ex) {
      if (ex.getStatus() == HttpStatus.NOT_FOUND) {
        Optional<CachedCase> cachedCase = dataRepo.readCachedCaseById(caseId);
        if (cachedCase.isPresent()) {
          log.with("caseid", caseId)
              .with("status", ex.getStatus())
              .with("message", ex.getMessage())
              .warn("New skeleton case created but not yet available.");
          throw new CTPException(
              Fault.ACCEPTED_UNABLE_TO_PROCESS,
              "Unable to provide launch URL/UAC at present, please try again later.");
        }
      }
      log.with("caseId", caseId)
          .error("Unable to provide launch URL/UAC, failed to call case service", ex);
      throw ex;
    }
  }

  private void rejectInvalidLaunchCombinationsForCE(
      CaseContainerDTO caseDetails, boolean individual, String formType) throws CTPException {
    if (!individual && FormType.C.name().equals(formType)) {
      Region region = convertRegion(caseDetails);
      String addressLevel = caseDetails.getAddressLevel();
      if ("E".equals(addressLevel)) {
        if (Region.N == region) {
          throw new CTPException(Fault.BAD_REQUEST, NI_LAUNCH_ERR_MSG);
        }
      }
    }
  }

  // Create a new case for a HH individual
  private CaseContainerDTO createIndividualCase(CaseContainerDTO caseDetails) {
    UUID individualCaseId = UUID.randomUUID();
    caseDetails.setId(individualCaseId);
    caseDetails.setCaseType(CaseType.HI.name());
    log.with("individualCaseId", individualCaseId).info("Creating new HI case");
    return caseDetails;
  }

  private String createLaunchUrl(
      String formType,
      CaseContainerDTO caseDetails,
      LaunchRequestDTO requestParamsDTO,
      String questionnaireId)
      throws CTPException {
    String encryptedPayload = "";
    try {
      EqLaunchData eqLuanchCoreDate =
          EqLaunchData.builder()
              .language(Language.ENGLISH)
              .source(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API)
              .channel(uk.gov.ons.ctp.common.domain.Channel.CC)
              .questionnaireId(questionnaireId)
              .formType(formType)
              .salt(appConfig.getEq().getResponseIdSalt())
              .caseContainer(caseDetails)
              .userId(Integer.toString(requestParamsDTO.getAgentId()))
              .accountServiceUrl(null)
              .accountServiceLogoutUrl(null)
              .build();
      encryptedPayload = eqLaunchService.getEqLaunchJwe(eqLuanchCoreDate);

    } catch (CTPException e) {
      log.with("caseId", caseDetails.getId())
          .with("questionnaireId", questionnaireId)
          .error("Failed to create JWE payload for eq launch", e);
      throw e;
    }
    String eqUrl =
        appConfig.getEq().getProtocol()
            + "://"
            + appConfig.getEq().getHost()
            + appConfig.getEq().getPath()
            + encryptedPayload;
    if (log.isDebugEnabled()) {
      log.with("launchURL", eqUrl).debug("Have created launch URL");
    }
    return eqUrl;
  }

  private void verifyFulfilmentCodeNotBlackListed(String fulfilmentCode) throws CTPException {
    Set<String> blacklistedProducts = appConfig.getFulfilments().getBlacklistedCodes();

    if (blacklistedProducts.contains(fulfilmentCode)) {
      log.with(fulfilmentCode).info("Fulfilment code is no longer available");
      throw new CTPException(
          Fault.BAD_REQUEST, "Requested fulfilment code is no longer available: " + fulfilmentCode);
    }
  }
}
