package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_QUESTIONNAIRE_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.EventPayload;
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
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.config.EqConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseStatus;
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
import uk.gov.ons.ctp.integration.eqlaunch.service.impl.EqLaunchServiceImpl;

/**
 * This class tests the CaseServiceImpl layer. It mocks out the layer below (caseServiceClientImpl),
 * which would deal with actually sending a HTTP request to the case service.
 */
public class CaseServiceImplTest {
  @Mock AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;

  @Mock CaseServiceClientServiceImpl caseServiceClient;

  @Mock EqLaunchService eqLaunchService = new EqLaunchServiceImpl();

  @Mock EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Mock CaseDataRepository dataRepo;

  @Mock AddressService addressSvc;

  @InjectMocks CaseService target = new CaseServiceImpl();

  @Captor private ArgumentCaptor<UUID> individualCaseIdCaptor;
  @Captor private ArgumentCaptor<CaseContainerDTO> caseCaptor;
  @Captor private ArgumentCaptor<SurveyLaunchedResponse> surveyLaunchedResponseCaptor;

  private static final EventType REFUSAL_EVENT_TYPE_FIELD_VALUE = EventType.REFUSAL_RECEIVED;
  private static final Source REFUSAL_SOURCE_FIELD_VALUE = Source.CONTACT_CENTRE_API;
  private static final Channel REFUSAL_CHANNEL_FIELD_VALUE = Channel.CC;

  private static final EventType FULFILMENT_EVENT_TYPE_FIELD_VALUE = EventType.FULFILMENT_REQUESTED;
  private static final Source FULFILMENT_SOURCE_FIELD_VALUE = Source.CONTACT_CENTRE_API;
  private static final Channel FULFILMENT_CHANNEL_FIELD_VALUE = Channel.CC;

  private static final boolean HAND_DELIVERY_TRUE = true;
  private static final boolean HAND_DELIVERY_FALSE = false;

  private static final boolean CASE_EVENTS_TRUE = true;
  private static final boolean CASE_EVENTS_FALSE = false;

  private static final long VALID_CASE_REF = 882_345_440L;

  private static final boolean USE_CACHED_CASE = true;
  private static final boolean NO_CACHED_CASE = false;

  private static final String A_CALL_ID = "8989-NOW";
  private static final String A_UPRN = "1234";
  private static final String AN_ESTAB_UPRN = "334111111111";
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    // For case retrieval, mock out a whitelist of allowable case events
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_CREATED", "CASE_UPDATED");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    Mockito.when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
    Mockito.when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    Mockito.when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);
  }

  @Test
  public void testNewCaseForNewAddress() throws Exception {
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);

    doTestNewCaseForNewAddress(caseRequestDTO, "SPG", true);
  }

  @Test
  public void testNewCaseForNewAddress_forEstabTypeOfOther() throws Exception {
    // Load request, which has estabType of Other
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);

    // Address type will be that for EstabType.Other
    doTestNewCaseForNewAddress(caseRequestDTO, "HH", false);
  }

  @Test
  public void testNewCaseForNewAddress_mismatchedCaseAndAddressType() throws Exception {
    // Load request, which has caseType of HH and estabType with a CE addressType
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, false);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(
          e.toString(),
          e.getMessage()
              .matches(".* address type .*CE.* from .*MILITARY_SLA.* not compatible .*HH.*"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_ceWithNonPositiveNumberOfResidents() throws Exception {
    // Load valid request and then update so that it's invalid
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);
    // Simulate error by making request a CE with a non-positive number of residents
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setCeUsualResidents(0);

    try {
      doTestNewCaseForNewAddress(caseRequestDTO, null, false);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.toString(), e.getMessage().matches(".*Number of residents .* for CE .*"));
    }
  }

  @Test
  public void testNewCaseForNewAddress_cePositiveNumberOfResidents() throws Exception {
    // Test that the check for a CE with non zero number residents is correct
    NewCaseRequestDTO caseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(2);
    // Simulate condition by making request a CE with a positive number of residents
    caseRequestDTO.setCaseType(CaseType.CE);
    caseRequestDTO.setCeUsualResidents(11);

    doTestNewCaseForNewAddress(caseRequestDTO, "CE", true);
  }

  private void doTestNewCaseForNewAddress(
      NewCaseRequestDTO caseRequestDTO,
      String expectedAddressType,
      boolean expectedIsSecureEstablishment)
      throws CTPException {
    // Run code under test
    CaseDTO response = target.createCaseForNewAddress(caseRequestDTO);

    // Grab created case
    ArgumentCaptor<CachedCase> caseCaptor = ArgumentCaptor.forClass(CachedCase.class);
    Mockito.verify(dataRepo, times(1)).writeCachedCase(caseCaptor.capture());
    CachedCase storedCase = caseCaptor.getValue();

    // Check contents of new case
    CachedCase expectedCase = mapperFacade.map(caseRequestDTO, CachedCase.class);
    expectedCase.setId(storedCase.getId());
    expectedCase.setCreatedDateTime(storedCase.getCreatedDateTime());
    String caseTypeName = caseRequestDTO.getCaseType().name();
    expectedCase.setAddressType(expectedAddressType);
    expectedCase.setEstabType(caseRequestDTO.getEstabType().getCode());
    assertEquals(expectedCase, storedCase);

    // Verify the NewAddressEvent
    CollectionCaseNewAddress expectedAddress =
        mapperFacade.map(caseRequestDTO, CollectionCaseNewAddress.class);
    expectedAddress.setAddress(mapperFacade.map(caseRequestDTO, Address.class));
    expectedAddress.setId(storedCase.getId());
    verifyNewAddressEventSent(
        expectedCase.getAddressType(),
        caseRequestDTO.getEstabType().getCode(),
        caseRequestDTO.getCeOrgName(),
        caseRequestDTO.getCeUsualResidents(),
        expectedAddress);

    // Verify response
    verifyCaseDTOContent(expectedCase, caseTypeName, expectedIsSecureEstablishment, response);
  }

  @Test
  public void testFulfilmentRequestByPost_individualFailsWithNullContactDetails() throws Exception {
    // All of the following fail validation because one of the contact detail fields is always null
    // or empty
    doVerifyFulfilmentRequestByPostFailsValidation(
        Product.CaseType.HH, null, "John", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "", "John", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", null, "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", "", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", "John", null, true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", "John", "", true);

    doVerifyFulfilmentRequestByPostFailsValidation(
        Product.CaseType.CE, null, "John", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "", "John", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", null, "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", "", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", "John", null, true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", "John", "", true);
  }

  @Test
  public void testFulfilmentRequestByPost_nonIndividualAllowsNullContactDetails() throws Exception {
    // Test that non-individual cases allow null/empty contact details
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, null, null, null, false, false);
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "", "", "", false, false);

    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, null, null, null, false, false);
    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, "", "", "", false, false);
  }

  @Test
  public void testFulfilmentRequestByPostSuccess_withCaseTypeHH() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "Mr", "Mickey", "Mouse", false, false);
  }

  @Test
  public void testFulfilmentRequestByPostSuccess_withIndividualTrue() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "Mr", "Mickey", "Mouse", true, false);
  }

  @Test
  public void testFulfilmentRequestByPost_caseSvcNotFoundResponse_cachedCase() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "Mr", "Mickey", "Mouse", true, true);
  }

  @Test
  public void testFulfilmentRequestByPostFailure_productNotFound() throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseData = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseData);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, "Mr", "Mickey", "Mouse");

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            Product.Region.E,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.POST);

    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>());

    try {
      // execution - call the unit under test
      target.fulfilmentRequestByPost(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertEquals("Compatible product cannot be found", e.getMessage());
      assertEquals("BAD_REQUEST", e.getFault().name());
    }
  }

  @Test
  public void testFulfilmentRequestByPostFailure_handDeliveryOnly() throws Exception {

    Mockito.clearInvocations(eventPublisher);

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType("SPG");
    caseFromCaseService.setHandDelivery(true);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, "Mrs", "Sally", "Smurf");

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            Product.Region.E,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.POST);

    Product productFoundFixture =
        getProductFoundFixture(
            Arrays.asList(Product.CaseType.SPG), Product.DeliveryChannel.POST, false);
    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    try {
      // execution - call the unit under test
      target.fulfilmentRequestByPost(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertEquals("Postal fulfilments cannot be delivered to this respondent", e.getMessage());
      assertEquals("BAD_REQUEST", e.getFault().name());
    }
  }

  @Test(expected = CTPException.class)
  public void testFulfilmentRequestByPost_caseSvcNotFoundResponse_noCachedCase() throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(eq(UUID_0), any());
    Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.empty());

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, "Mrs", "Sally", "Smurf");
    target.fulfilmentRequestByPost(requestBodyDTOFixture);
  }

  @Test(expected = ResponseStatusException.class)
  public void testFulfilmentRequestByPost_caseSvcRestClientException() throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(eq(UUID_0), any());

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, "Mrs", "Sally", "Smurf");

    target.fulfilmentRequestByPost(requestBodyDTOFixture);
  }

  @Test
  public void testFulfilmentRequestBySMSSuccess_withCaseTypeHH() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, false, false);
  }

  @Test
  public void testFulfilmentRequestBySMSSuccess_withIndividualTrue() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, true, false);
  }

  @Test
  public void testFulfilmentRequestBySMS_caseSvcNotFoundResponse_cachedCase() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, true, true);
  }

  @Test
  public void testFulfilmentRequestBySMSFailure_productNotFound() throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseData = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseData);

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            Product.Region.E,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.SMS);

    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>());

    try {
      // execution - call the unit under test
      target.fulfilmentRequestBySMS(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertEquals("Compatible product cannot be found", e.getMessage());
      assertEquals("BAD_REQUEST", e.getFault().name());
    }
  }

  @Test(expected = CTPException.class)
  public void testFulfilmentRequestBySMS_caseSvcNotFoundResponse_noCachedCase() throws Exception {
    CaseContainerDTO caseData = casesFromCaseService().get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(eq(UUID_0), any());
    Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.empty());

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);

    target.fulfilmentRequestBySMS(requestBodyDTOFixture);
  }

  @Test(expected = ResponseStatusException.class)
  public void testFulfilmentRequestBySMS_caseSvcRestClientException() throws Exception {
    CaseContainerDTO caseData = casesFromCaseService().get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(eq(UUID_0), any());

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);

    target.fulfilmentRequestBySMS(requestBodyDTOFixture);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_TRUE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseHHhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.CE,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_TRUE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.CE,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels = Arrays.asList(DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryFalse() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryFalse_fromCache() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_FALSE,
        USE_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void shouldGetSecureEstablishmentByCaseId() throws CTPException {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_1), any())).thenReturn(caseFromCaseService);

    CaseDTO results = target.getCaseById(UUID_1, new CaseQueryRequestDTO(false));
    assertTrue(results.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.getEstabUprn());
  }

  @Test
  public void testGetCaseByCaseId_householdIndividualCase() throws CTPException {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseById(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseById(UUID_0, new CaseQueryRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is not suitable", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void testGetCaseByUprn_withCaseDetails() throws CTPException {
    doTestGetCaseByUprn(true);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetails() throws CTPException {
    doTestGetCaseByUprn(false);
  }

  @Test
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet_noCachedCase()
      throws Exception {

    List<CaseContainerDTO> caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    caseFromCaseService.get(0).setCaseType("HI");
    caseFromCaseService.get(1).setCaseType("HI");
    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any()))
        .thenReturn(caseFromCaseService);
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(true));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_HH() throws Exception {

    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_SPG() throws Exception {

    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    addressFromAI.setCensusEstabType("marina");
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_CE() throws Exception {

    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    addressFromAI.setCensusEstabType("CARE HOME");
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_NA() throws Exception {

    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    addressFromAI.setCensusEstabType("NA");
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_addressServiceNotFound()
      throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND))
        .when(addressSvc)
        .uprnQuery(UPRN.getValue());
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1))
        .readCachedCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(dataRepo, never()).writeCachedCase(any());
    Mockito.verify(addressSvc, times(1)).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_addressSvcRestClientException()
      throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(addressSvc)
        .uprnQuery(eq(UPRN.getValue()));

    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_scottishAddress()
      throws Exception {

    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    addressFromAI.setCountryCode("S");
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_cachedCase() throws Exception {

    CachedCase cachedCase = caseFromRepository();
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.of(cachedCase));
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(1, results.size());
    verifyCachedCase(cachedCase, results.get(0));
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetCaseByUprn_caseSvcRestClientException() throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase_RetriesExhausted()
      throws Exception {

    AddressIndexAddressCompositeDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCachedCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);
    Mockito.doThrow(new CTPException(Fault.SYSTEM_ERROR, new Exception(), "Retries exhausted"))
        .when(dataRepo)
        .writeCachedCase(any());
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test
  public void testGetCaseByUprn_mixedCaseTypes() throws CTPException {

    // Build results to be returned from search
    List<CaseContainerDTO> caseFromCaseService = casesFromCaseService();
    caseFromCaseService.get(0).setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    assertEquals(1, results.size());

    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(
            caseFromCaseService.get(1),
            caseEvents,
            Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS));
    verifyCase(results.get(0), expectedCaseResult, caseEvents);
  }

  @Test
  public void testGetCaseByUprn_caseSPGhandDeliveryTrue() throws CTPException {
    doTestGetCasesByUprn("SPG", true, Arrays.asList(DeliveryChannel.SMS));
  }

  @Test
  public void testGetCaseByUprn_caseHHhandDeliveryTrue() throws CTPException {
    doTestGetCasesByUprn("HH", true, Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS));
  }

  @Test
  public void testGetCaseByUprn_caseSPGhandDeliveryFalse() throws CTPException {
    doTestGetCasesByUprn("SPG", false, Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS));
  }

  @Test
  public void shouldGetSecureEstablishmentByUprn() throws CTPException {

    Mockito.when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any()))
        .thenReturn(casesFromCaseService());

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(2, results.size());
    assertTrue(results.get(1).isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.get(1).getEstabUprn());
  }

  private void doTestGetCasesByUprn(
      String caseType, boolean handDelivery, List<DeliveryChannel> expectedDeliveryChannels)
      throws CTPException {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(334999999999L);

    List<CaseContainerDTO> caseFromCaseService = casesFromCaseService();
    caseFromCaseService.get(0).setCaseType(caseType);
    caseFromCaseService.get(0).setHandDelivery(handDelivery);
    Mockito.when(caseServiceClient.getCaseByUprn(eq(uprn.getValue()), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    List<CaseDTO> results = target.getCaseByUPRN(uprn, new CaseQueryRequestDTO(caseEvents));
    assertEquals(2, results.size());

    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(caseFromCaseService.get(0), caseEvents, expectedDeliveryChannels);
    verifyCase(results.get(0), expectedCaseResult, caseEvents);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withCaseDetails() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.HH, HAND_DELIVERY_FALSE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withNoCaseDetails() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.HH, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_caseHHhandDeliveryTrue() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.HH, HAND_DELIVERY_TRUE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withCaseDetails() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.CE, HAND_DELIVERY_FALSE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withNoCaseDetails() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.CE, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_caseSPGhandDeliveryTrue() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels = Arrays.asList(DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.SPG, HAND_DELIVERY_TRUE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_caseSPGhandDeliveryFalse() throws Exception {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.SPG, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_householdIndividualCase() throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseByCaseRef(eq(VALID_CASE_REF), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseByCaseReference(VALID_CASE_REF, new CaseQueryRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is not suitable", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void shouldGetSecureEstablishmentByCaseReference() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    CaseDTO results = target.getCaseByCaseReference(VALID_CASE_REF, new CaseQueryRequestDTO(false));
    assertTrue(results.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.getEstabUprn());
  }

  private void rejectNonLuhn(long caseRef) {
    CaseQueryRequestDTO dto = new CaseQueryRequestDTO(false);
    CTPException e =
        assertThrows(CTPException.class, () -> target.getCaseByCaseReference(caseRef, dto));
    assertEquals("Invalid Case Reference", e.getMessage());
  }

  @Test
  public void shouldRejectGetByNonLuhnCaseReference() {
    rejectNonLuhn(123);
    rejectNonLuhn(1231);
    rejectNonLuhn(100000000);
    rejectNonLuhn(999999999);
  }

  private void acceptLuhn(long caseRef) throws Exception {
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any()))
        .thenReturn(casesFromCaseService().get(0));
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    target.getCaseByCaseReference(caseRef, requestParams);
  }

  @Test
  public void shouldAcceptLuhnCompliantGetByCaseReference() throws Exception {
    acceptLuhn(1230);
    acceptLuhn(VALID_CASE_REF);
    acceptLuhn(100000009);
    acceptLuhn(999999998);
  }

  @Test
  public void testRespondentRefusal_withExtraordinaryReason() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.EXTRAORDINARY);
  }

  @Test
  public void testRespondentRefusal_withHardReason() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.HARD);
  }

  @Test
  public void testRespondentRefusal_withUUID() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.EXTRAORDINARY);
  }

  @Test
  public void testRespondentRefusal_withoutDateTime() throws Exception {
    Date dateTime = null;
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(
        caseId, expectedEventCaseId, expectedResponseCaseId, dateTime, Reason.EXTRAORDINARY);
  }

  @Test
  public void testRespondentRefusal_forUnknownUUID() throws Exception {
    UUID unknownCaseId = null;
    UUID expectedEventCaseId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    String expectedResponseCaseId = "unknown";
    doRespondentRefusalTest(
        unknownCaseId,
        expectedEventCaseId,
        expectedResponseCaseId,
        new Date(),
        Reason.EXTRAORDINARY);
  }

  @Test
  public void testLaunchCECase() throws Exception {
    doLaunchTest("CE", false);
  }

  @Test
  public void testLaunchCECaseForIndividual() throws Exception {
    doLaunchTest("CE", true);
  }

  @Test
  public void testLaunchHHCase() throws Exception {
    doLaunchTest("HH", false);
  }

  @Test
  public void testLaunchSPGCase() throws Exception {
    doLaunchTest("SPG", false);
  }

  @Test
  public void testLaunchSPGCaseForIndividual() throws Exception {
    doLaunchTest("SPG", true);
  }

  @Test
  public void testLaunchHHCaseForIndividual() throws Exception {
    doLaunchTest("HH", true);
  }

  @Test
  public void testLaunchHICase() throws Exception {
    try {
      doLaunchTest("HI", false);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage(), e.getMessage().contains("must be SPG, CE or HH"));
    }
  }

  @Test(expected = CTPException.class)
  public void testLaunch_caseServiceNotFoundException_cachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.of(new CachedCase()));
    LaunchRequestDTO launchRequestDTO = CaseServiceFixture.createLaunchRequestDTO(false);
    target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
  }

  @Test(expected = ResponseStatusException.class)
  public void testLaunch_caseServiceNotFoundException_noCachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.empty());
    LaunchRequestDTO launchRequestDTO = CaseServiceFixture.createLaunchRequestDTO(true);
    target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
  }

  @Test(expected = ResponseStatusException.class)
  public void testLaunch_caseServiceResponseStatusException() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    LaunchRequestDTO launchRequestDTO = CaseServiceFixture.createLaunchRequestDTO(true);
    target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(CaseContainerDTO dto, String expectedMsg) {
    try {
      doLaunchTest(false, dto, "CE");
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains(expectedMsg));
    }
  }

  @SneakyThrows
  private void assertThatCeManagerFormFromUnitRegionIsRejected(CaseContainerDTO dto) {
    assertThatInvalidLaunchComboIsRejected(
        dto, "A CE Manager form can only be launched against an establishment address not a UNIT.");
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionEast() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "E");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionWest() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "W");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionNorth() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "N");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectNorthernIslandCallsFromCeManagers() throws Exception {
    CaseContainerDTO dto = mockGetCaseById("CE", "E", "N");
    assertThatInvalidLaunchComboIsRejected(
        dto,
        "All Northern Ireland calls from CE Managers are to be escalated to the NI management team.");
  }

  @SneakyThrows
  private void checkInvalidateCaseForStatus(CaseStatus status) {
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    dto.setStatus(status);
    ResponseDTO response = target.invalidateCase(dto);
    assertEquals(dto.getCaseId().toString(), response.getId());
    assertNotNull(response.getDateTime());

    ArgumentCaptor<AddressNotValid> payloadCaptor = ArgumentCaptor.forClass(AddressNotValid.class);

    verify(eventPublisher)
        .sendEvent(
            eq(EventType.ADDRESS_NOT_VALID),
            eq(Source.CONTACT_CENTRE_API),
            eq(Channel.CC),
            payloadCaptor.capture());

    AddressNotValid payload = payloadCaptor.getValue();
    assertEquals(dto.getCaseId(), payload.getCollectionCase().getId());
    assertEquals(dto.getNotes(), payload.getNotes());
    assertEquals(dto.getStatus().name(), payload.getReason());
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDerelict() {
    checkInvalidateCaseForStatus(CaseStatus.DERELICT);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDemolished() {
    checkInvalidateCaseForStatus(CaseStatus.DEMOLISHED);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusNonResidential() {
    checkInvalidateCaseForStatus(CaseStatus.NON_RESIDENTIAL);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusUnderConstruction() {
    checkInvalidateCaseForStatus(CaseStatus.UNDER_CONSTRUCTION);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusSplitAddress() {
    checkInvalidateCaseForStatus(CaseStatus.SPLIT_ADDRESS);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusMerged() {
    checkInvalidateCaseForStatus(CaseStatus.MERGED);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDuplicate() {
    checkInvalidateCaseForStatus(CaseStatus.DUPLICATE);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDoesNotExist() {
    checkInvalidateCaseForStatus(CaseStatus.DOES_NOT_EXIST);
  }

  @Test(expected = ResponseStatusException.class)
  public void shouldRejectCaseNotFound() throws Exception {
    when(caseServiceClient.getCaseById(any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    target.invalidateCase(dto);
  }

  private void mockEqLaunchJwe() throws Exception {
    // Mock out building of launch payload
    Mockito.when(
            eqLaunchService.getEqLaunchJwe(
                eq(Language.ENGLISH),
                eq(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API),
                eq(uk.gov.ons.ctp.common.domain.Channel.CC),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                isNull())) // keystore
        .thenReturn("simulated-encrypted-payload");
  }

  private void verifyEqLaunchJwe(
      String questionnaireId, boolean individual, String caseType, String formType)
      throws Exception {
    Mockito.verify(eqLaunchService)
        .getEqLaunchJwe(
            eq(Language.ENGLISH),
            eq(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API),
            eq(uk.gov.ons.ctp.common.domain.Channel.CC),
            caseCaptor.capture(),
            eq(AN_AGENT_ID), // agent
            eq(questionnaireId),
            eq(formType),
            isNull(), // accountServiceUrl
            isNull(),
            any()); // keystore

    CaseContainerDTO capturedCase = caseCaptor.getValue();
    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, capturedCase.getId());
    } else {
      assertEquals(UUID_0, capturedCase.getId());
    }
  }

  private void verifySurveyLaunchedEventPublished(
      String caseType, boolean individual, UUID caseId, String questionnaireId) {
    Mockito.verify(eventPublisher)
        .sendEvent(
            eq(EventType.SURVEY_LAUNCHED),
            eq(Source.CONTACT_CENTRE_API),
            eq(Channel.CC),
            (EventPayload) surveyLaunchedResponseCaptor.capture());

    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, surveyLaunchedResponseCaptor.getValue().getCaseId());
    } else {
      assertEquals(caseId, surveyLaunchedResponseCaptor.getValue().getCaseId());
    }
    assertEquals(questionnaireId, surveyLaunchedResponseCaptor.getValue().getQuestionnaireId());
    assertEquals(AN_AGENT_ID, surveyLaunchedResponseCaptor.getValue().getAgentId());
  }

  private void verifyCorrectIndividualCaseId(String caseType, boolean individual) {
    // Verify call to RM to get qid is using the correct individual case id
    Mockito.verify(caseServiceClient)
        .getSingleUseQuestionnaireId(any(), eq(individual), individualCaseIdCaptor.capture());
    if (caseType.equals("HH") && individual) {
      assertNotEquals(UUID_0, individualCaseIdCaptor.getValue()); // expecting newly allocated uuid
    } else {
      assertNull(individualCaseIdCaptor.getValue());
    }
  }

  private CaseContainerDTO mockGetCaseById(String caseType, String addressLevel, String region) {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType);
    caseFromCaseService.setAddressLevel(addressLevel);
    caseFromCaseService.setRegion(region);
    when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);
    return caseFromCaseService;
  }

  private void doLaunchTest(String caseType, boolean individual) throws Exception {
    CaseContainerDTO caseFromCaseService = mockGetCaseById(caseType, "U", A_REGION.name());
    doLaunchTest(individual, caseFromCaseService, "H");
  }

  private void doLaunchTest(
      boolean individual, CaseContainerDTO caseFromCaseService, String formType) throws Exception {
    String caseType = caseFromCaseService.getCaseType();

    // Fake RM response for creating questionnaire ID
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto = new SingleUseQuestionnaireIdDTO();
    newQuestionnaireIdDto.setQuestionnaireId(A_QUESTIONNAIRE_ID);
    newQuestionnaireIdDto.setFormType(formType);
    Mockito.when(caseServiceClient.getSingleUseQuestionnaireId(eq(UUID_0), eq(individual), any()))
        .thenReturn(newQuestionnaireIdDto);

    // Mock appConfig data
    EqConfig eqConfig = new EqConfig();
    eqConfig.setHost("localhost");
    Mockito.when(appConfig.getEq()).thenReturn(eqConfig);

    mockEqLaunchJwe();

    LaunchRequestDTO launchRequestDTO = CaseServiceFixture.createLaunchRequestDTO(individual);

    // Invoke method under test, and check returned url
    String launchUrl = target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
    assertEquals("https://localhost/session?token=simulated-encrypted-payload", launchUrl);

    verifyCorrectIndividualCaseId(caseType, individual);
    verifyEqLaunchJwe(A_QUESTIONNAIRE_ID, individual, caseType, formType);
    verifySurveyLaunchedEventPublished(caseType, individual, UUID_0, A_QUESTIONNAIRE_ID);
  }

  private void doRespondentRefusalTest(
      UUID caseId,
      UUID expectedEventCaseId,
      String expectedResponseCaseId,
      Date dateTime,
      Reason reason)
      throws Exception {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(A_UPRN);
    RefusalRequestDTO refusalPayload =
        RefusalRequestDTO.builder()
            .caseId(caseId == null ? null : caseId.toString())
            .agentId("123")
            .notes("Description of refusal")
            .title("Mr")
            .forename("Steve")
            .surname("Jones")
            .telNo("+447890000000")
            .addressLine1("1 High Street")
            .addressLine2("Delph")
            .addressLine3("Oldham")
            .townName("Manchester")
            .postcode("OL3 5DJ")
            .uprn(uprn)
            .region(A_REGION)
            .reason(reason)
            .dateTime(dateTime)
            .callId(A_CALL_ID)
            .build();

    // report the refusal
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO refusalResponse = target.reportRefusal(caseId, refusalPayload);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response to the refusal
    assertEquals(expectedResponseCaseId, refusalResponse.getId());
    verifyTimeInExpectedRange(
        timeBeforeInvocation, timeAfterInvocation, refusalResponse.getDateTime());

    // Grab the published event
    ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<RespondentRefusalDetails> refusalEventCaptor =
        ArgumentCaptor.forClass(RespondentRefusalDetails.class);
    verify(eventPublisher)
        .sendEvent(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            refusalEventCaptor.capture());

    assertEquals(REFUSAL_EVENT_TYPE_FIELD_VALUE, eventTypeCaptor.getValue());
    assertEquals(REFUSAL_SOURCE_FIELD_VALUE, sourceCaptor.getValue());
    assertEquals(REFUSAL_CHANNEL_FIELD_VALUE, channelCaptor.getValue());

    // Validate payload of published event
    RespondentRefusalDetails refusal = refusalEventCaptor.getValue();
    assertEquals("Description of refusal", refusal.getReport());
    assertEquals("123", refusal.getAgentId());
    assertEquals(A_CALL_ID, refusal.getCallId());
    assertEquals(expectedEventCaseId, refusal.getCollectionCase().getId());

    verifyRefusalAddress(refusal, uprn);
    assertEquals(reason.name() + "_REFUSAL", refusal.getType());
    Contact expectedContact = new Contact("Mr", "Steve", "Jones", "+447890000000");
    assertEquals(expectedContact, refusal.getContact());
  }

  private void verifyRefusalAddress(
      RespondentRefusalDetails refusal, UniquePropertyReferenceNumber expectedUprn) {
    // Validate address
    AddressCompact address = refusal.getAddress();
    assertEquals("1 High Street", address.getAddressLine1());
    assertEquals("Delph", address.getAddressLine2());
    assertEquals("Oldham", address.getAddressLine3());
    assertEquals("Manchester", address.getTownName());
    assertEquals("OL3 5DJ", address.getPostcode());
    assertEquals(A_REGION.name(), address.getRegion());
    assertEquals(A_UPRN, address.getUprn());
  }

  private void verifyTimeInExpectedRange(long minAllowed, long maxAllowed, Date dateTime) {
    long actualInMillis = dateTime.getTime();
    assertTrue(actualInMillis + " not after " + minAllowed, actualInMillis >= minAllowed);
    assertTrue(actualInMillis + " not before " + maxAllowed, actualInMillis <= maxAllowed);
  }

  @SneakyThrows
  private void doTestGetCaseByCaseId(
      CaseType caseType,
      boolean handDelivery,
      boolean caseEvents,
      boolean cached,
      List<DeliveryChannel> expectedAllowedDeliveryChannels) {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    caseFromCaseService.setHandDelivery(handDelivery);
    CaseDTO expectedCaseResult;

    if (cached) {
      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
          .when(caseServiceClient)
          .getCaseById(eq(UUID_0), any());

      CachedCase caseFromRepository = caseFromRepository();
      caseFromRepository.setCreatedDateTime(caseFromCaseService.getCreatedDateTime());
      Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0)))
          .thenReturn(Optional.of(caseFromRepository));

      CaseContainerDTO expectedCase = mapperFacade.map(caseFromRepository, CaseContainerDTO.class);
      expectedCaseResult =
          createExpectedCaseDTO(expectedCase, caseEvents, expectedAllowedDeliveryChannels);

    } else {
      Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any()))
          .thenReturn(caseFromCaseService);
      expectedCaseResult =
          createExpectedCaseDTO(caseFromCaseService, caseEvents, expectedAllowedDeliveryChannels);
    }

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);

    verifyCase(results, expectedCaseResult, caseEvents);
    assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), results.getCreatedDateTime().getTime());
  }

  private void doTestGetCaseByUprn(boolean caseEvents) throws CTPException {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(334999999999L);

    // Build results to be returned from search
    List<CaseContainerDTO> caseFromCaseService = casesFromCaseService();
    caseFromCaseService.get(0).setCaseType(CaseType.HH.name());
    caseFromCaseService.get(1).setCaseType(CaseType.CE.name());
    Mockito.when(caseServiceClient.getCaseByUprn(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    List<CaseDTO> results = target.getCaseByUPRN(uprn, requestParams);

    // Verify response
    CaseDTO expectedCaseResult0 =
        createExpectedCaseDTO(
            caseFromCaseService.get(0),
            caseEvents,
            Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS));
    verifyCase(results.get(0), expectedCaseResult0, caseEvents);

    CaseDTO expectedCaseResult1 =
        createExpectedCaseDTO(
            caseFromCaseService.get(1),
            caseEvents,
            Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS));
    verifyCase(results.get(1), expectedCaseResult1, caseEvents);
  }

  private void doTestGetCaseByCaseRef(
      CaseType caseType,
      boolean handDelivery,
      boolean caseEvents,
      List<DeliveryChannel> expectedAllowedDeliveryChannels)
      throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    caseFromCaseService.setHandDelivery(handDelivery);
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseByCaseReference(VALID_CASE_REF, requestParams);
    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(caseFromCaseService, caseEvents, expectedAllowedDeliveryChannels);
    verifyCase(results, expectedCaseResult, caseEvents);
  }

  private UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  private CaseDTO createExpectedCaseDTO(
      CaseContainerDTO caseFromCaseService,
      boolean caseEvents,
      List<DeliveryChannel> expectedAllowedDeliveryChannels) {

    CaseDTO expectedCaseResult =
        CaseDTO.builder()
            .id(caseFromCaseService.getId())
            .caseRef(caseFromCaseService.getCaseRef())
            .caseType(caseFromCaseService.getCaseType())
            .estabType(EstabType.forCode(caseFromCaseService.getEstabType()))
            .estabDescription(caseFromCaseService.getEstabType())
            .allowedDeliveryChannels(expectedAllowedDeliveryChannels)
            .createdDateTime(caseFromCaseService.getCreatedDateTime())
            .lastUpdated(caseFromCaseService.getLastUpdated())
            .addressLine1(caseFromCaseService.getAddressLine1())
            .addressLine2(caseFromCaseService.getAddressLine2())
            .addressLine3(caseFromCaseService.getAddressLine3())
            .addressType(caseFromCaseService.getAddressType())
            .townName(caseFromCaseService.getTownName())
            .region(caseFromCaseService.getRegion().substring(0, 1))
            .postcode(caseFromCaseService.getPostcode())
            .ceOrgName(caseFromCaseService.getOrganisationName())
            .uprn(createUprn(caseFromCaseService.getUprn()))
            .estabUprn(createUprn(caseFromCaseService.getEstabUprn()))
            .handDelivery(caseFromCaseService.isHandDelivery())
            .secureEstablishment(caseFromCaseService.isSecureEstablishment())
            .build();
    if (caseEvents) {
      List<CaseEventDTO> expectedCaseEvents =
          caseFromCaseService
              .getCaseEvents()
              .stream()
              .filter(e -> !e.getDescription().contains("Should be filtered out"))
              .map(
                  e -> {
                    CaseEventDTO expectedEvent =
                        CaseEventDTO.builder()
                            .description(e.getDescription())
                            .category(e.getEventType())
                            .createdDateTime(e.getCreatedDateTime())
                            .build();
                    return expectedEvent;
                  })
              .collect(Collectors.toList());
      expectedCaseResult.setCaseEvents(expectedCaseEvents);
    }
    return expectedCaseResult;
  }

  @SneakyThrows
  private void verifyCase(CaseDTO results, CaseDTO expectedCaseResult, boolean caseEventsExpected) {
    assertEquals(expectedCaseResult.getId(), results.getId());
    assertEquals(expectedCaseResult.getCaseRef(), results.getCaseRef());
    assertEquals(expectedCaseResult.getCaseType(), results.getCaseType());
    assertEquals(expectedCaseResult.getCeOrgName(), results.getCeOrgName());
    assertEquals(
        expectedCaseResult.getAllowedDeliveryChannels(), results.getAllowedDeliveryChannels());
    assertEquals(expectedCaseResult.isHandDelivery(), results.isHandDelivery());

    if (caseEventsExpected) {
      // Note that the test data contains 3 events, but the 'X11' event is filtered out as it is not
      // on the whitelist
      assertEquals(2, results.getCaseEvents().size());
      CaseEventDTO event = results.getCaseEvents().get(0);
      assertEquals("Initial creation of case", event.getDescription());
      assertEquals("CASE_CREATED", event.getCategory());
      assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), event.getCreatedDateTime().getTime());
      event = results.getCaseEvents().get(1);
      assertEquals("Create Household Visit", event.getDescription());
      assertEquals("CASE_UPDATED", event.getCategory());
      assertEquals(asMillis("2019-05-16T12:12:12.343Z"), event.getCreatedDateTime().getTime());
    } else {
      assertNull(results.getCaseEvents());
    }

    assertEquals(expectedCaseResult, results);
    Mockito.verify(dataRepo, never()).readCachedCaseByUPRN(any());
    Mockito.verify(dataRepo, never()).writeCachedCase(any());
    Mockito.verify(addressSvc, never()).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  private void verifyNewCase(AddressIndexAddressCompositeDTO address, CaseDTO result)
      throws Exception {

    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1))
        .readCachedCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(addressSvc, times(1)).uprnQuery(anyLong());

    // Verify content of case written to Firestore
    CachedCase cachedCase = mapperFacade.map(address, CachedCase.class);
    cachedCase.setId(result.getId().toString());
    Mockito.verify(dataRepo, times(1)).writeCachedCase(any(CachedCase.class));

    // Verify response
    verifyCaseDTOContent(cachedCase, CaseType.HH.name(), false, result);

    // Verify the NewAddressEvent
    CollectionCaseNewAddress newAddress = mapperFacade.map(address, CollectionCaseNewAddress.class);
    newAddress.setId(cachedCase.getId());
    verifyNewAddressEventSent(
        address.getCensusAddressType(), address.getCensusEstabType(), null, 0, newAddress);
  }

  private void verifyCaseDTOContent(
      CachedCase cachedCase,
      String expectedCaseType,
      boolean isSecureEstablishment,
      CaseDTO actualCaseDto) {
    CaseDTO expectedNewCaseResult = mapperFacade.map(cachedCase, CaseDTO.class);
    expectedNewCaseResult.setCreatedDateTime(actualCaseDto.getCreatedDateTime());
    expectedNewCaseResult.setCaseType(expectedCaseType);
    expectedNewCaseResult.setEstabType(EstabType.forCode(cachedCase.getEstabType()));
    expectedNewCaseResult.setSecureEstablishment(isSecureEstablishment);
    expectedNewCaseResult.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));
    assertEquals(expectedNewCaseResult, actualCaseDto);
  }

  private void verifyNewAddressEventSent(
      String expectedAddressType,
      String expectedEstabTypeCode,
      String orgName,
      Integer expectedCapacity,
      CollectionCaseNewAddress newAddress) {
    newAddress.setCaseType(expectedAddressType);
    newAddress.setSurvey(SURVEY_NAME);
    newAddress.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    newAddress.setOrganisationName(orgName);
    newAddress.setCeExpectedCapacity(expectedCapacity);
    Optional<AddressType> addressType = EstabType.forCode(expectedEstabTypeCode).getAddressType();
    if (addressType.isPresent() && addressType.get() == AddressType.CE) {
      newAddress.getAddress().setAddressLevel("E");
    } else {
      newAddress.getAddress().setAddressLevel("U");
    }
    newAddress.getAddress().setAddressType(expectedAddressType);
    newAddress.getAddress().setEstabType(expectedEstabTypeCode);
    NewAddress payload = new NewAddress();
    payload.setCollectionCase(newAddress);
    Mockito.verify(eventPublisher, times(1))
        .sendEvent(
            EventType.NEW_ADDRESS_REPORTED,
            Source.CONTACT_CENTRE_API,
            appConfig.getChannel(),
            payload);
  }

  private void verifyCachedCase(CachedCase cachedCase, CaseDTO result) throws Exception {
    CaseDTO expectedResult = mapperFacade.map(cachedCase, CaseDTO.class);
    expectedResult.setCaseType(CaseType.HH.name());
    expectedResult.setEstabType(EstabType.forCode(cachedCase.getEstabType()));
    expectedResult.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));

    assertEquals(expectedResult, result);

    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1))
        .readCachedCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(dataRepo, never()).writeCachedCase(any());
    Mockito.verify(addressSvc, never()).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  private long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);

    return dateParser.parse(datetime).getTime();
  }

  private PostalFulfilmentRequestDTO getPostalFulfilmentRequestDTO(
      UUID caseId, String title, String forename, String surname) {
    PostalFulfilmentRequestDTO requestBodyDTOFixture = new PostalFulfilmentRequestDTO();
    requestBodyDTOFixture.setCaseId(caseId);
    requestBodyDTOFixture.setTitle(title);
    requestBodyDTOFixture.setForename(forename);
    requestBodyDTOFixture.setSurname(surname);
    requestBodyDTOFixture.setFulfilmentCode("ABC123");
    requestBodyDTOFixture.setDateTime(DateTimeUtil.nowUTC());
    return requestBodyDTOFixture;
  }

  private SMSFulfilmentRequestDTO getSMSFulfilmentRequestDTO(CaseContainerDTO caseFromCaseService) {
    SMSFulfilmentRequestDTO requestBodyDTOFixture = new SMSFulfilmentRequestDTO();
    requestBodyDTOFixture.setCaseId(caseFromCaseService.getId());
    requestBodyDTOFixture.setTelNo("+447890000000");
    requestBodyDTOFixture.setFulfilmentCode("ABC123");
    requestBodyDTOFixture.setDateTime(DateTimeUtil.nowUTC());
    return requestBodyDTOFixture;
  }

  private Product getProductFoundFixture(
      List<Product.CaseType> caseTypes,
      Product.DeliveryChannel deliveryChannel,
      boolean individual) {
    return Product.builder()
        .caseTypes(caseTypes)
        .description("foobar")
        .fulfilmentCode("ABC123")
        .deliveryChannel(deliveryChannel)
        .regions(new ArrayList<Product.Region>(List.of(Product.Region.E)))
        .requestChannels(
            new ArrayList<Product.RequestChannel>(
                List.of(Product.RequestChannel.CC, Product.RequestChannel.FIELD)))
        .individual(individual)
        .build();
  }

  private Product getExpectedSearchCriteria(
      Product.Region region, String fulfilmentCode, Product.DeliveryChannel deliveryChannel) {
    return Product.builder()
        .fulfilmentCode(fulfilmentCode)
        .requestChannels(Arrays.asList(Product.RequestChannel.CC))
        .deliveryChannel(deliveryChannel)
        .regions(Arrays.asList(region))
        .build();
  }

  private void doVerifyFulfilmentRequestByPostFailsValidation(
      Product.CaseType caseType, String title, String forename, String surname, boolean individual)
      throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, title, forename, surname);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            Product.Region.valueOf(caseFromCaseService.getRegion().substring(0, 1)),
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.POST);

    // The mocked productReference will return this product
    Product productFoundFixture =
        getProductFoundFixture(Arrays.asList(caseType), Product.DeliveryChannel.POST, individual);
    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    // execution - call the unit under test
    try {
      target.fulfilmentRequestByPost(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertTrue(e.getMessage().contains("none of the following fields can be empty"));
    }
  }

  private void doFulfilmentRequestByPostSuccess(
      Product.CaseType caseType,
      String title,
      String forename,
      String surname,
      boolean individual,
      boolean cached)
      throws Exception {
    Mockito.clearInvocations(eventPublisher);

    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    CachedCase cachedCase = caseFromRepository();

    if (cached) {
      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
          .when(caseServiceClient)
          .getCaseById(eq(UUID_0), any());
      Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.of(cachedCase));
    } else {
      Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any()))
          .thenReturn(caseFromCaseService);
    }

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, title, forename, surname);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            Product.Region.E,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.POST);

    // The mocked productReference will return this product
    Product productFoundFixture =
        getProductFoundFixture(Arrays.asList(caseType), Product.DeliveryChannel.POST, individual);
    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    // execution - call the unit under test
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO responseDTOFixture = target.fulfilmentRequestByPost(requestBodyDTOFixture);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), responseDTOFixture.getId());
    verifyTimeInExpectedRange(
        timeBeforeInvocation, timeAfterInvocation, responseDTOFixture.getDateTime());

    // Grab the published event
    ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<FulfilmentRequest> fulfilmentRequestCaptor =
        ArgumentCaptor.forClass(FulfilmentRequest.class);
    verify(eventPublisher)
        .sendEvent(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            fulfilmentRequestCaptor.capture());

    assertEquals(FULFILMENT_EVENT_TYPE_FIELD_VALUE, eventTypeCaptor.getValue());
    assertEquals(FULFILMENT_SOURCE_FIELD_VALUE, sourceCaptor.getValue());
    assertEquals(FULFILMENT_CHANNEL_FIELD_VALUE, channelCaptor.getValue());
    FulfilmentRequest actualFulfilmentRequest = fulfilmentRequestCaptor.getValue();
    assertEquals(
        requestBodyDTOFixture.getFulfilmentCode(), actualFulfilmentRequest.getFulfilmentCode());
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), actualFulfilmentRequest.getCaseId());

    if (caseType == Product.CaseType.HH && individual) {
      assertNotNull(actualFulfilmentRequest.getIndividualCaseId());
    } else {
      assertEquals(null, actualFulfilmentRequest.getIndividualCaseId());
    }

    Contact actualContact = actualFulfilmentRequest.getContact();
    assertEquals(requestBodyDTOFixture.getTitle(), actualContact.getTitle());
    assertEquals(requestBodyDTOFixture.getForename(), actualContact.getForename());
    assertEquals(requestBodyDTOFixture.getSurname(), actualContact.getSurname());
    assertEquals(null, actualContact.getTelNo());

    Address actualAddress = actualFulfilmentRequest.getAddress();
    assertEquals(
        cached ? cachedCase.getAddressLine1() : caseFromCaseService.getAddressLine1(),
        actualAddress.getAddressLine1());
    assertEquals(
        cached ? cachedCase.getAddressLine2() : caseFromCaseService.getAddressLine2(),
        actualAddress.getAddressLine2());
    assertEquals(
        cached ? cachedCase.getAddressLine3() : caseFromCaseService.getAddressLine3(),
        actualAddress.getAddressLine3());
    assertEquals(
        cached ? cachedCase.getTownName() : caseFromCaseService.getTownName(),
        actualAddress.getTownName());
    assertEquals(
        cached ? cachedCase.getPostcode() : caseFromCaseService.getPostcode(),
        actualAddress.getPostcode());
    assertEquals(
        cached ? cachedCase.getRegion() : caseFromCaseService.getRegion(),
        actualAddress.getRegion());
    assertEquals(cached ? null : caseFromCaseService.getLatitude(), actualAddress.getLatitude());
    assertEquals(cached ? null : caseFromCaseService.getLongitude(), actualAddress.getLongitude());
    assertEquals(
        cached ? cachedCase.getUprn() : caseFromCaseService.getUprn(), actualAddress.getUprn());
    assertEquals(
        cached ? cachedCase.getAddressType() : caseFromCaseService.getAddressType(),
        actualAddress.getAddressType());
    assertEquals(
        cached ? cachedCase.getEstabType() : caseFromCaseService.getEstabType(),
        actualAddress.getEstabType());
  }

  private void doFulfilmentRequestBySMSSuccess(
      Product.CaseType caseType, boolean individual, boolean cached) throws Exception {

    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    CachedCase cachedCase = caseFromRepository();

    if (cached) {
      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
          .when(caseServiceClient)
          .getCaseById(eq(UUID_0), any());
      Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.of(cachedCase));
    } else {
      Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any()))
          .thenReturn(caseFromCaseService);
    }

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseFromCaseService);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            Product.Region.E,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.SMS);

    // The mocked productReference will return this product
    Product productFoundFixture =
        getProductFoundFixture(Arrays.asList(caseType), Product.DeliveryChannel.SMS, individual);
    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    // execution - call the unit under test
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO response = target.fulfilmentRequestBySMS(requestBodyDTOFixture);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), response.getId());
    verifyTimeInExpectedRange(timeBeforeInvocation, timeAfterInvocation, response.getDateTime());

    // Grab the published event
    ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<FulfilmentRequest> fulfilmentRequestCaptor =
        ArgumentCaptor.forClass(FulfilmentRequest.class);
    verify(eventPublisher)
        .sendEvent(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            fulfilmentRequestCaptor.capture());

    assertEquals(FULFILMENT_EVENT_TYPE_FIELD_VALUE, eventTypeCaptor.getValue());
    assertEquals(FULFILMENT_SOURCE_FIELD_VALUE, sourceCaptor.getValue());
    assertEquals(FULFILMENT_CHANNEL_FIELD_VALUE, channelCaptor.getValue());

    FulfilmentRequest actualFulfilmentRequest = fulfilmentRequestCaptor.getValue();
    assertEquals(
        requestBodyDTOFixture.getFulfilmentCode(), actualFulfilmentRequest.getFulfilmentCode());
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), actualFulfilmentRequest.getCaseId());

    if (caseType == Product.CaseType.HH && individual) {
      assertNotNull(actualFulfilmentRequest.getIndividualCaseId());
    } else {
      assertEquals(null, actualFulfilmentRequest.getIndividualCaseId());
    }

    Contact actualContact = actualFulfilmentRequest.getContact();
    assertEquals(null, actualContact.getTitle());
    assertEquals(null, actualContact.getForename());
    assertEquals(null, actualContact.getSurname());
    assertEquals(requestBodyDTOFixture.getTelNo(), actualContact.getTelNo());
  }

  @SneakyThrows
  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
  }

  @SneakyThrows
  private CachedCase caseFromRepository() {
    return FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);
  }
}
