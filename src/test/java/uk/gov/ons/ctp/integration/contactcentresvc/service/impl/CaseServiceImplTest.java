package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
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
import uk.gov.ons.ctp.common.model.Language;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressSplitDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.config.EqConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;
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
  private static final String A_UPRN = "1234";
  private static final String AN_ESTAB_UPRN = "334111111111";
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  private Reason reason = Reason.EXTRAORDINARY;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    // For case retrieval, mock out a whitelist of allowable case events
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_CREATED", "CASE_UPDATED");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    Mockito.when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
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
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, null, null, null, false);
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "", "", "", false);

    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, null, null, null, false);
    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, "", "", "", false);
  }

  @Test
  public void testFulfilmentRequestByPostSuccess_withCaseTypeHH() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "Mr", "Mickey", "Mouse", false);
  }

  @Test
  public void testFulfilmentRequestByPostSuccess_withIndividualTrue() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "Mr", "Mickey", "Mouse", true);
  }

  @Test
  public void testFulfilmentRequestByPostFailure_productNotFound() throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseData = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseData);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(caseData, "Mr", "Mickey", "Mouse");

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseData, requestBodyDTOFixture.getFulfilmentCode(), Product.DeliveryChannel.POST);

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
        getPostalFulfilmentRequestDTO(caseFromCaseService, "Mrs", "Sally", "Smurf");

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
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

  @Test
  public void testFulfilmentRequestBySMSSuccess_withCaseTypeHH() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, false);
  }

  @Test
  public void testFulfilmentRequestBySMSSuccess_withIndividualTrue() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH, true);
  }

  @Test
  public void testFulfilmentRequestBySMSFailure_productNotFound() throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseData = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseData);

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseData, requestBodyDTOFixture.getFulfilmentCode(), Product.DeliveryChannel.SMS);

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

  @Test
  public void testGetHouseholdCaseByCaseId_withCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH, HAND_DELIVERY_FALSE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseHHhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH, HAND_DELIVERY_TRUE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.CE, HAND_DELIVERY_TRUE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.CE, HAND_DELIVERY_TRUE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels = Arrays.asList(DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG, HAND_DELIVERY_TRUE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryFalse() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void shouldGetSecureEstablishmentByCaseId() {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_1), any())).thenReturn(caseFromCaseService);

    CaseDTO results = target.getCaseById(UUID_1, new CaseQueryRequestDTO(false));
    assertTrue(results.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.getEstabUprn());
  }

  @Test
  public void testGetCaseByCaseId_householdIndividualCase() {
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
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet_NoCachedCase()
      throws Exception {

    List<CaseContainerDTO> caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    caseFromCaseService.get(0).setCaseType("HI");
    caseFromCaseService.get(1).setCaseType("HI");
    AddressIndexAddressSplitDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressSplitDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any()))
        .thenReturn(caseFromCaseService);
    Mockito.when(dataRepo.readCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(Optional.of(addressFromAI));

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(true));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase() throws Exception {

    AddressIndexAddressSplitDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressSplitDTO[].class).get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(Optional.of(addressFromAI));

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(1, results.size());
    verifyNewCase(addressFromAI, results.get(0));
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase_NoAddressFound()
      throws Exception {

    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(Optional.empty());
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1)).readCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(dataRepo, never()).storeCaseByUPRN(any());
    Mockito.verify(addressSvc, times(1)).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase_ScottishAddress()
      throws Exception {

    AddressIndexAddressSplitDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressSplitDTO[].class).get(0);
    addressFromAI.setCountryCode("S");
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(Optional.of(addressFromAI));
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1)).readCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(dataRepo, never()).storeCaseByUPRN(any());
    Mockito.verify(addressSvc, times(1)).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_CachedCase() throws Exception {

    CachedCase cachedCase = FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCaseByUPRN(UPRN)).thenReturn(Optional.of(cachedCase));
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
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase_DataStoreContentionException()
      throws Exception {

    AddressIndexAddressSplitDTO addressFromAI =
        FixtureHelper.loadClassFixtures(AddressIndexAddressSplitDTO[].class).get(0);
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
    Mockito.when(dataRepo.readCaseByUPRN(UPRN)).thenReturn(Optional.empty());
    Mockito.when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(Optional.of(addressFromAI));
    Mockito.doThrow(
            new DataStoreContentionException(
                "Test repository contention exception", new Exception()))
        .when(dataRepo)
        .storeCaseByUPRN(any());
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
  public void testGetCaseByCaseRef_caseHHhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.HH, HAND_DELIVERY_TRUE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.CE, HAND_DELIVERY_FALSE, CASE_EVENTS_TRUE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.CE, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_caseSPGhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels = Arrays.asList(DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.SPG, HAND_DELIVERY_TRUE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_caseSPGhandDeliveryFalse() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseRef(
        CaseType.SPG, HAND_DELIVERY_FALSE, CASE_EVENTS_FALSE, expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseRef_householdIndividualCase() {
    long testCaseRef = 88234544;

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseByCaseRef(eq(testCaseRef), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseByCaseReference(testCaseRef, new CaseQueryRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is not suitable", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void shouldGetSecureEstablishmentByCaseReference() {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    CaseDTO results =
        target.getCaseByCaseReference(103300000000001L, new CaseQueryRequestDTO(false));
    assertTrue(results.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.getEstabUprn());
  }

  @Test
  public void testRespondentRefusal_withExtraordinaryReason() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    this.reason = Reason.EXTRAORDINARY;
    doRespondentRefusalTest(caseId, expectedEventCaseId, expectedResponseCaseId, dateTime);
  }

  @Test
  public void testRespondentRefusal_withHardReason() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    this.reason = Reason.HARD;
    doRespondentRefusalTest(caseId, expectedEventCaseId, expectedResponseCaseId, dateTime);
  }

  @Test
  public void testRespondentRefusal_withUUID() throws Exception {
    Date dateTime = new Date();
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(caseId, expectedEventCaseId, expectedResponseCaseId, dateTime);
  }

  @Test
  public void testRespondentRefusal_withoutDateTime() throws Exception {
    Date dateTime = null;
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    doRespondentRefusalTest(caseId, expectedEventCaseId, expectedResponseCaseId, dateTime);
  }

  @Test
  public void testRespondentRefusal_forUnknownUUID() throws Exception {
    UUID unknownCaseId = null;
    UUID expectedEventCaseId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    String expectedResponseCaseId = "unknown";
    doRespondentRefusalTest(unknownCaseId, expectedEventCaseId, expectedResponseCaseId, new Date());
  }

  @Test
  public void testLaunchCECase() throws Exception {
    doLaunchTest(UUID_0, "CE", false);
  }

  @Test
  public void testLaunchCECaseForIndividual() throws Exception {
    doLaunchTest(UUID_0, "CE", true);
  }

  @Test
  public void testLaunchHHCase() throws Exception {
    doLaunchTest(UUID_0, "HH", false);
  }

  @Test
  public void testLaunchSPGCase() throws Exception {
    doLaunchTest(UUID_0, "SPG", false);
  }

  @Test
  public void testLaunchSPGCaseForIndividual() throws Exception {
    doLaunchTest(UUID_0, "SPG", true);
  }

  @Test
  public void testLaunchHHCaseForIndividual() throws Exception {
    doLaunchTest(UUID_0, "HH", true);
  }

  @Test
  public void testLaunchHICase() throws Exception {
    try {
      doLaunchTest(UUID_0, "HI", false);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage(), e.getMessage().contains("must be SPG, CE or HH"));
    }
  }

  @SneakyThrows
  private void checkModifyCaseForStatus(CaseStatus status) {
    ModifyCaseRequestDTO dto = CaseServiceFixture.createModifyCaseRequestDTO();
    dto.setStatus(status);
    ResponseDTO response = target.modifyCase(dto);
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
  public void shouldModifyCaseWhenStatusDerelict() {
    checkModifyCaseForStatus(CaseStatus.DERELICT);
  }

  @Test
  public void shouldModifyCaseWhenStatusDemolished() {
    checkModifyCaseForStatus(CaseStatus.DEMOLISHED);
  }

  @Test
  public void shouldModifyCaseWhenStatusNonResidential() {
    checkModifyCaseForStatus(CaseStatus.NON_RESIDENTIAL);
  }

  @Test
  public void shouldModifyCaseWhenStatusUnderConstruction() {
    checkModifyCaseForStatus(CaseStatus.UNDER_CONSTRUCTION);
  }

  @Test
  public void shouldModifyCaseWhenStatusSplitAddress() {
    checkModifyCaseForStatus(CaseStatus.SPLIT_ADDRESS);
  }

  @Test
  public void shouldModifyCaseWhenStatusMerged() {
    checkModifyCaseForStatus(CaseStatus.MERGED);
  }

  @Test(expected = ResponseStatusException.class)
  public void shouldRejectCaseNotFound() throws Exception {
    when(caseServiceClient.getCaseById(any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
    ModifyCaseRequestDTO dto = CaseServiceFixture.createModifyCaseRequestDTO();
    target.modifyCase(dto);
  }

  @Test
  public void shouldNotModifyCaseWhenStatusUnchanged() throws Exception {
    ModifyCaseRequestDTO dto = CaseServiceFixture.createModifyCaseRequestDTO();
    ResponseDTO response = target.modifyCase(dto);
    assertEquals(dto.getCaseId().toString(), response.getId());
    assertNotNull(response.getDateTime());
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  private void doLaunchTest(UUID caseId, String caseType, boolean individual) throws Exception {
    // Build case details to be returned from case search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    // Fake RM response for creating questionnaire ID
    String questionnaireId = "566786126";
    String formType = "H";
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto = new SingleUseQuestionnaireIdDTO();
    newQuestionnaireIdDto.setQuestionnaireId(questionnaireId);
    newQuestionnaireIdDto.setFormType(formType);
    Mockito.when(caseServiceClient.getSingleUseQuestionnaireId(eq(UUID_0), eq(individual), any()))
        .thenReturn(newQuestionnaireIdDto);

    // Mock appConfig data
    EqConfig eqConfig = new EqConfig();
    eqConfig.setHost("localhost");
    Mockito.when(appConfig.getEq()).thenReturn(eqConfig);

    // Mock out building of launch payload
    Mockito.when(
            eqLaunchService.getEqLaunchJwe(
                eq(Language.ENGLISH),
                eq(uk.gov.ons.ctp.common.model.Source.CONTACT_CENTRE_API),
                eq(uk.gov.ons.ctp.common.model.Channel.CC),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                isNull())) // keystore
        .thenReturn("simulated-encrypted-payload");

    // Build DTO for launch request
    LaunchRequestDTO launchRequestDTO = new LaunchRequestDTO();
    launchRequestDTO.setAgentId("1234");
    launchRequestDTO.setIndividual(individual);

    // Invoke method under test, and check returned url
    String launchUrl = target.getLaunchURLForCaseId(caseId, launchRequestDTO);
    assertEquals("https://localhost/session?token=simulated-encrypted-payload", launchUrl);

    // Verify call to RM to get qid is using the correct individual case id
    ArgumentCaptor<UUID> individualCaseIdCaptor = ArgumentCaptor.forClass(UUID.class);
    Mockito.verify(caseServiceClient)
        .getSingleUseQuestionnaireId(any(), eq(individual), individualCaseIdCaptor.capture());
    if (caseType.equals("HH") && individual) {
      assertNotEquals(UUID_0, individualCaseIdCaptor.getValue()); // expecting newly allocated uuid
    } else {
      assertNull(individualCaseIdCaptor.getValue());
    }

    // Verify correct data passed to eqLauncher
    ArgumentCaptor<CaseContainerDTO> caseCaptor = ArgumentCaptor.forClass(CaseContainerDTO.class);
    Mockito.verify(eqLaunchService)
        .getEqLaunchJwe(
            eq(Language.ENGLISH),
            eq(uk.gov.ons.ctp.common.model.Source.CONTACT_CENTRE_API),
            eq(uk.gov.ons.ctp.common.model.Channel.CC),
            caseCaptor.capture(),
            eq("1234"), // agent
            eq(questionnaireId),
            eq(formType),
            isNull(), // accountServiceUrl
            isNull(),
            any()); // keystore

    // Verify case details passed to eqLauncher
    CaseContainerDTO capturedCase = caseCaptor.getValue();
    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, capturedCase.getId());
    } else {
      assertEquals(UUID_0, capturedCase.getId());
    }

    // Verify surveyLaunched event published
    ArgumentCaptor<SurveyLaunchedResponse> surveyLaunchedResponseCaptor =
        ArgumentCaptor.forClass(SurveyLaunchedResponse.class);
    Mockito.verify(eventPublisher)
        .sendEvent(
            eq(EventType.SURVEY_LAUNCHED),
            eq(Source.CONTACT_CENTRE_API),
            eq(Channel.CC),
            (EventPayload) surveyLaunchedResponseCaptor.capture());

    // Verify payload for surveyLaunched event
    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, surveyLaunchedResponseCaptor.getValue().getCaseId());
    } else {
      assertEquals(caseId, surveyLaunchedResponseCaptor.getValue().getCaseId());
    }
    assertEquals(questionnaireId, surveyLaunchedResponseCaptor.getValue().getQuestionnaireId());
    assertEquals("1234", surveyLaunchedResponseCaptor.getValue().getAgentId());
  }

  private void doRespondentRefusalTest(
      UUID caseId, UUID expectedEventCaseId, String expectedResponseCaseId, Date dateTime)
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
            .region(Region.E)
            .reason(reason)
            .dateTime(dateTime)
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
    assertEquals("E", address.getRegion());
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
      List<DeliveryChannel> expectedAllowedDeliveryChannels) {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    caseFromCaseService.setHandDelivery(handDelivery);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);

    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(caseFromCaseService, caseEvents, expectedAllowedDeliveryChannels);
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
      List<DeliveryChannel> expectedAllowedDeliveryChannels) {
    long testCaseRef = 88234544;

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    caseFromCaseService.setHandDelivery(handDelivery);
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseByCaseReference(testCaseRef, requestParams);
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
            .allowedDeliveryChannels(expectedAllowedDeliveryChannels)
            .createdDateTime(caseFromCaseService.getCreatedDateTime())
            .addressLine1(caseFromCaseService.getAddressLine1())
            .addressLine2(caseFromCaseService.getAddressLine2())
            .addressLine3(caseFromCaseService.getAddressLine3())
            .townName(caseFromCaseService.getTownName())
            .region(caseFromCaseService.getRegion().substring(0, 1))
            .postcode(caseFromCaseService.getPostcode())
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
    Mockito.verify(dataRepo, never()).readCaseByUPRN(any());
    Mockito.verify(dataRepo, never()).storeCaseByUPRN(any());
    Mockito.verify(addressSvc, never()).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  private void verifyNewCase(AddressIndexAddressSplitDTO address, CaseDTO result) throws Exception {

    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1)).readCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(addressSvc, times(1)).uprnQuery(anyLong());

    CachedCase cachedCase = mapperFacade.map(address, CachedCase.class);
    cachedCase.setId(
        UUID.class.isInstance(result.getId())
            ? result.getId().toString()
            : UUID.randomUUID().toString());
    Mockito.verify(dataRepo, times(1)).storeCaseByUPRN(cachedCase);

    CaseDTO expectedNewCaseResult = mapperFacade.map(cachedCase, CaseDTO.class);
    assertEquals(expectedNewCaseResult, result);

    CollectionCaseNewAddress newAddress = mapperFacade.map(address, CollectionCaseNewAddress.class);
    newAddress.setId(cachedCase.getId());
    newAddress.setSurvey("CENSUS");
    newAddress.getAddress().setAddressLevel("U");
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
    assertEquals(expectedResult, result);

    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1)).readCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(dataRepo, never()).storeCaseByUPRN(any());
    Mockito.verify(addressSvc, never()).uprnQuery(anyLong());
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  private long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);

    return dateParser.parse(datetime).getTime();
  }

  private PostalFulfilmentRequestDTO getPostalFulfilmentRequestDTO(
      CaseContainerDTO caseFromCaseService, String title, String forename, String surname) {
    PostalFulfilmentRequestDTO requestBodyDTOFixture = new PostalFulfilmentRequestDTO();
    requestBodyDTOFixture.setCaseId(caseFromCaseService.getId());
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
      CaseContainerDTO caseData, String fulfilmentCode, Product.DeliveryChannel deliveryChannel) {
    return Product.builder()
        .fulfilmentCode(fulfilmentCode)
        .requestChannels(Arrays.asList(Product.RequestChannel.CC))
        .deliveryChannel(deliveryChannel)
        .regions(Arrays.asList(Product.Region.valueOf(caseData.getRegion().substring(0, 1))))
        .build();
  }

  private void doVerifyFulfilmentRequestByPostFailsValidation(
      Product.CaseType caseType, String title, String forename, String surname, boolean individual)
      throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(caseFromCaseService, title, forename, surname);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
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
      Product.CaseType caseType, String title, String forename, String surname, boolean individual)
      throws Exception {
    Mockito.clearInvocations(eventPublisher);

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(caseFromCaseService, title, forename, surname);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
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
    assertEquals(caseFromCaseService.getAddressLine1(), actualAddress.getAddressLine1());
    assertEquals(caseFromCaseService.getAddressLine2(), actualAddress.getAddressLine2());
    assertEquals(caseFromCaseService.getAddressLine3(), actualAddress.getAddressLine3());
    assertEquals(caseFromCaseService.getTownName(), actualAddress.getTownName());
    assertEquals(caseFromCaseService.getPostcode(), actualAddress.getPostcode());
    assertEquals(caseFromCaseService.getRegion(), actualAddress.getRegion());
    assertEquals(caseFromCaseService.getLatitude(), actualAddress.getLatitude());
    assertEquals(caseFromCaseService.getLongitude(), actualAddress.getLongitude());
    assertEquals(caseFromCaseService.getUprn(), actualAddress.getUprn());
    assertEquals(caseFromCaseService.getArid(), actualAddress.getArid());
    assertEquals(caseFromCaseService.getAddressType(), actualAddress.getAddressType());
    assertEquals(caseFromCaseService.getEstabType(), actualAddress.getEstabType());
  }

  private void doFulfilmentRequestBySMSSuccess(Product.CaseType caseType, boolean individual)
      throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseFromCaseService);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
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
}
