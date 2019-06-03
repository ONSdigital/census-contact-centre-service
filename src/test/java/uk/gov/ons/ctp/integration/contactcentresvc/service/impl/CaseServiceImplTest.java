package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalDetails;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.event.impl.FulfilmentEventPublisher;
import uk.gov.ons.ctp.integration.contactcentresvc.event.impl.RespondentRefusalEventPublisher;
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

/**
 * This class tests the CaseServiceImpl layer. It mocks out the layer below (caseServiceClientImpl),
 * which would deal with actually sending a HTTP request to the case service.
 */
public class CaseServiceImplTest {
  @Mock AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;
  @Mock FulfilmentEventPublisher fulfilmentPublisher;
  @Mock RespondentRefusalEventPublisher respondentRefusalPublisher;
  @Mock CaseServiceClientServiceImpl caseServiceClient;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks CaseService target = new CaseServiceImpl();

  private UUID uuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  private UUID uuid2 = UUID.fromString("b7565b5e-2222-2222-2222-918c0d3642ed");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    // Mock out a whitelist of allowable case events
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_CREATED", "CASE_UPDATED");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    Mockito.when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }

  @Test
  public void testFulfilmentRequestByPost_individualFailsWithNullContactDetails() throws Exception {
    // All of the following fail validation because one of the contact detail fields is always null
    // or empty
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HI, null, "John", "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HI, "", "John", "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HI, "Mr", null, "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HI, "Mr", "", "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HI, "Mr", "John", null);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HI, "Mr", "John", "");

    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CI, null, "John", "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CI, "", "John", "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CI, "Mr", null, "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CI, "Mr", "", "Smith");
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CI, "Mr", "John", null);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CI, "Mr", "John", "");
  }

  @Test
  public void testFulfilmentRequestByPost_nonIndividualAllowsNullContactDetails() throws Exception {
    // Test that non-individual cases allow null/empty contact details
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, null, null, null);
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "", "", "");

    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, null, null, null);
    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, "", "", "");
  }

  @Test
  public void testFulfilmentRequestByPostSuccess_withCaseTypeHH() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "Mr", "Mickey", "Mouse");
  }

  @Test
  public void testFulfilmentRequestByPostSuccess_withCaseTypeHI() throws Exception {
    doFulfilmentRequestByPostSuccess(Product.CaseType.HI, "Mr", "Mickey", "Mouse");
  }

  @Test
  public void testFulfilmentRequestByPostFailure_productNotFound() throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(uuid), any())).thenReturn(caseData);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(caseData, "Mr", "Mickey", "Mouse");

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseData, requestBodyDTOFixture.getFulfilmentCode(), Product.DeliveryChannel.POST);

    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>());

    try {
      // execution - call the unit under test
      ResponseDTO responseDTOFixture = target.fulfilmentRequestByPost(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertEquals("Compatible product cannot be found", e.getMessage());
      assertEquals("BAD_REQUEST", e.getFault().name());
    }
  }

  @Test
  public void testFulfilmentRequestBySMSSuccess_withCaseTypeHH() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HH);
  }

  @Test
  public void testFulfilmentRequestBySMSSuccess_withCaseTypeHI() throws Exception {
    doFulfilmentRequestBySMSSuccess(Product.CaseType.HI);
  }

  @Test
  public void testFulfilmentRequestBySMSFailure_productNotFound() throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(uuid), any())).thenReturn(caseData);

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseData, requestBodyDTOFixture.getFulfilmentCode(), Product.DeliveryChannel.SMS);

    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>());

    try {
      // execution - call the unit under test
      ResponseDTO responseDTOFixture = target.fulfilmentRequestBySMS(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertEquals("Compatible product cannot be found", e.getMessage());
      assertEquals("BAD_REQUEST", e.getFault().name());
    }
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.HH, true);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.HH, false);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.CE, true);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.CE, false);
  }

  @Test
  public void testGetCaseByCaseId_nonHouseholdOrCommunalCase() throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType("HI"); // Not household case
    Mockito.when(caseServiceClient.getCaseById(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseById(uuid, new CaseRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is a not a household or communal case", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void testGetCaseByUprn_withCaseDetails() throws Exception {
    doTestGetCaseByUprn(true);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetails() throws Exception {
    doTestGetCaseByUprn(false);
  }

  @Test
  public void testGetCaseByUprn_nonHouseholdCase_emptyResultSet() throws Exception {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(1235532324343434L);

    // Build results to be returned from search
    List<CaseContainerDTO> caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    caseFromCaseService.get(0).setCaseType("CI");
    caseFromCaseService.get(1).setCaseType("HI");
    Mockito.when(caseServiceClient.getCaseByUprn(eq(uprn.getValue()), any()))
        .thenReturn(caseFromCaseService);

    // Run the request, and check that there are no results (all filtered out as there are no
    // household or communal cases)
    List<CaseDTO> results = target.getCaseByUPRN(uprn, new CaseRequestDTO(true));
    assertTrue(results.isEmpty());
  }

  @Test
  public void testGetCaseByUprn_nonHouseholdCase_mixed() throws Exception {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(1235532324343434L);

    // Build results to be returned from search
    List<CaseContainerDTO> caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    caseFromCaseService.get(0).setCaseType("X"); // Not household case
    Mockito.when(caseServiceClient.getCaseByUprn(eq(uprn.getValue()), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    List<CaseDTO> results = target.getCaseByUPRN(uprn, new CaseRequestDTO(true));
    assertEquals(1, results.size());
    verifyCase(results.get(0), uuid2, CaseType.HH, true);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.HH, true);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.HH, false);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.CE, true);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.CE, false);
  }

  @Test
  public void testGetCaseByCaseRef_nonHouseholdCase() throws Exception {
    long testCaseRef = 88234544;

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType("X"); // Not household case
    Mockito.when(caseServiceClient.getCaseByCaseRef(eq(testCaseRef), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseByCaseReference(testCaseRef, new CaseRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is a not a household or communal case", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void testRespondentRefusal_withUUID() throws Exception {
    UUID caseId = UUID.randomUUID();
    doRespondentRefusalTest(caseId);
  }

  @Test
  public void testRespondentRefusal_forUnknownUUID() throws Exception {
    UUID unknownCaseId = null;
    doRespondentRefusalTest(unknownCaseId);
  }

  private void doRespondentRefusalTest(UUID caseId) throws Exception {
    RefusalRequestDTO refusalPayload =
        RefusalRequestDTO.builder()
            .caseId(caseId == null ? null : caseId.toString())
            .notes("Description of refusal")
            .title("Mr")
            .forename("Steve")
            .surname("Jones")
            .telNo("+447890000000")
            .build();

    // report the refusal
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO refusalResponse = target.reportRefusal(caseId, refusalPayload);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response to the refusal
    assertEquals(caseId == null ? null : caseId.toString(), refusalResponse.getId());
    verifyTimeInExpectedRange(
        timeBeforeInvocation, timeAfterInvocation, refusalResponse.getDateTime());

    // Grab the published event
    ArgumentCaptor<RespondentRefusalEvent> refusalEventCaptor =
        ArgumentCaptor.forClass(RespondentRefusalEvent.class);
    verify(respondentRefusalPublisher).sendEvent(refusalEventCaptor.capture());

    // Validate the header of the published event
    RespondentRefusalEvent publishedMessage = refusalEventCaptor.getValue();
    Header header = publishedMessage.getEvent();
    assertEquals("REFUSAL_RECEIVED", header.getType());
    assertEquals("CONTACT_CENTRE_API", header.getSource());
    assertEquals(Product.RequestChannel.CC.name(), header.getChannel());
    verifyTimeInExpectedRange(timeBeforeInvocation, timeAfterInvocation, header.getDateTime());
    assertNotNull(UUID.fromString(header.getTransactionId()));
    assertNotEquals(caseId, header.getTransactionId()); // Must be new transaction id

    // Validate payload of published event
    RespondentRefusalDetails refusal = publishedMessage.getPayload().getRefusal();
    assertEquals("HARD_REFUSAL", refusal.getType());
    assertEquals("Description of refusal", refusal.getReport());
    assertNull(refusal.getAgentId());
    assertEquals(caseId, refusal.getCollectionCase().getId());
    Contact actualContact = refusal.getContact();
    assertEquals("Mr", actualContact.getTitle());
    assertEquals("Steve", actualContact.getForename());
    assertEquals("Jones", actualContact.getSurname());
    assertNull(actualContact.getEmail());
    assertEquals("+447890000000", actualContact.getTelNo());
  }

  private void verifyTimeInExpectedRange(long minAllowed, long maxAllowed, Date dateTime) {
    long actualInMillis = dateTime.getTime();
    assertTrue(actualInMillis + " not after " + minAllowed, actualInMillis >= minAllowed);
    assertTrue(actualInMillis + " not before " + maxAllowed, actualInMillis <= maxAllowed);
  }

  private void doTestGetCaseByCaseId(CaseType caseType, boolean caseEvents) throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType(caseType.name());
    Mockito.when(caseServiceClient.getCaseById(eq(uuid), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(uuid, requestParams);

    verifyCase(results, uuid, caseType, caseEvents);
  }

  private void doTestGetCaseByUprn(boolean caseEvents) throws Exception {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(1235532324343434L);

    // Build results to be returned from search
    List<CaseContainerDTO> caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    caseFromCaseService.get(0).setCaseType(CaseType.HH.name());
    caseFromCaseService.get(1).setCaseType(CaseType.CE.name());
    Mockito.when(caseServiceClient.getCaseByUprn(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    List<CaseDTO> results = target.getCaseByUPRN(uprn, requestParams);

    verifyCase(results.get(0), uuid, CaseType.HH, caseEvents);
    verifyCase(results.get(1), uuid2, CaseType.CE, caseEvents);
  }

  private void doTestGetCaseByCaseRef(CaseType caseType, boolean caseEvents) throws Exception {
    long testCaseRef = 88234544;

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType(caseType.name());
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = target.getCaseByCaseReference(testCaseRef, requestParams);

    verifyCase(results, uuid, caseType, caseEvents);
  }

  private void verifyCase(
      CaseDTO results, UUID expectedUUID, CaseType expectedCaseType, boolean caseEventsExpected)
      throws ParseException {
    assertEquals(expectedUUID, results.getId());
    assertEquals("1000000000000001", results.getCaseRef());
    assertEquals(expectedCaseType.name(), results.getCaseType());
    assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), results.getCreatedDateTime().getTime());

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
      Product.CaseType caseType, Product.DeliveryChannel deliveryChannel) {
    return Product.builder()
        .caseType(caseType)
        .description("foobar")
        .fulfilmentCode("ABC123")
        .language("eng")
        .deliveryChannel(deliveryChannel)
        .regions(new ArrayList<Product.Region>(List.of(Product.Region.E)))
        .requestChannels(
            new ArrayList<Product.RequestChannel>(
                List.of(Product.RequestChannel.CC, Product.RequestChannel.FIELD)))
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
      Product.CaseType caseType, String title, String forename, String surname) throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(uuid), any())).thenReturn(caseFromCaseService);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(caseFromCaseService, title, forename, surname);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.POST);

    // The mocked productReference will return this product
    Product productFoundFixture = getProductFoundFixture(caseType, Product.DeliveryChannel.POST);
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
      Product.CaseType caseType, String title, String forename, String surname) throws Exception {
    Mockito.clearInvocations(fulfilmentPublisher);

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(uuid), any())).thenReturn(caseFromCaseService);

    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(caseFromCaseService, title, forename, surname);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.POST);

    // The mocked productReference will return this product
    Product productFoundFixture = getProductFoundFixture(caseType, Product.DeliveryChannel.POST);
    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    // execution - call the unit under test
    ResponseDTO responseDTOFixture = target.fulfilmentRequestByPost(requestBodyDTOFixture);

    ArgumentCaptor<FulfilmentRequestedEvent> fulfilmentRequestedEventArg =
        ArgumentCaptor.forClass(FulfilmentRequestedEvent.class);
    verify(fulfilmentPublisher).sendEvent(fulfilmentRequestedEventArg.capture());

    Header actualHeader = fulfilmentRequestedEventArg.getValue().getEvent();

    assertEquals("FULFILMENT_REQUESTED", actualHeader.getType());
    assertEquals("CONTACT_CENTRE_API", actualHeader.getSource());
    assertEquals(Product.RequestChannel.CC.name(), actualHeader.getChannel());

    FulfilmentRequest actualFulfilmentRequest =
        fulfilmentRequestedEventArg.getValue().getPayload().getFulfilmentRequest();

    assertEquals(
        requestBodyDTOFixture.getFulfilmentCode(), actualFulfilmentRequest.getFulfilmentCode());
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), actualFulfilmentRequest.getCaseId());

    // If the caseType is HI then the individualCaseId should be set, otherwise it should be empty.
    if (caseType == Product.CaseType.HI) {
      assertNotEquals(null, actualFulfilmentRequest.getIndividualCaseId());
    } else {
      assertEquals(null, actualFulfilmentRequest.getIndividualCaseId());
    }

    Contact actualContact = actualFulfilmentRequest.getContact();

    assertEquals(requestBodyDTOFixture.getTitle(), actualContact.getTitle());
    assertEquals(requestBodyDTOFixture.getForename(), actualContact.getForename());
    assertEquals(requestBodyDTOFixture.getSurname(), actualContact.getSurname());
  }

  private void doFulfilmentRequestBySMSSuccess(Product.CaseType caseType) throws Exception {

    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(caseServiceClient.getCaseById(eq(uuid), any())).thenReturn(caseFromCaseService);

    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseFromCaseService);

    Product expectedSearchCriteria =
        getExpectedSearchCriteria(
            caseFromCaseService,
            requestBodyDTOFixture.getFulfilmentCode(),
            Product.DeliveryChannel.SMS);

    // The mocked productReference will return this product
    Product productFoundFixture = getProductFoundFixture(caseType, Product.DeliveryChannel.SMS);
    Mockito.when(productReference.searchProducts(eq(expectedSearchCriteria)))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    // execution - call the unit under test
    ResponseDTO responseDTOFixture = target.fulfilmentRequestBySMS(requestBodyDTOFixture);

    ArgumentCaptor<FulfilmentRequestedEvent> fulfilmentRequestedEventArg =
        ArgumentCaptor.forClass(FulfilmentRequestedEvent.class);
    verify(publisher).sendEvent(fulfilmentRequestedEventArg.capture());

    Header actualHeader = fulfilmentRequestedEventArg.getValue().getEvent();

    assertEquals("FULFILMENT_REQUESTED", actualHeader.getType());
    assertEquals("CONTACT_CENTRE_API", actualHeader.getSource());
    assertEquals(Product.RequestChannel.CC.name(), actualHeader.getChannel());

    FulfilmentRequest actualFulfilmentRequest =
        fulfilmentRequestedEventArg.getValue().getPayload().getFulfilmentRequest();

    assertEquals(
        requestBodyDTOFixture.getFulfilmentCode(), actualFulfilmentRequest.getFulfilmentCode());
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), actualFulfilmentRequest.getCaseId());

    // If the caseType is HI then the individualCaseId should be set, otherwise it should be empty.
    if (caseType == Product.CaseType.HI) {
      assertNotEquals(null, actualFulfilmentRequest.getIndividualCaseId());
    } else {
      assertEquals(null, actualFulfilmentRequest.getIndividualCaseId());
    }

    Contact actualContact = actualFulfilmentRequest.getContact();

    assertEquals(requestBodyDTOFixture.getTelNo(), actualContact.getTelNo());
  }
}
