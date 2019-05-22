package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.*;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.event.ContactCentreEventPublisher;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.*;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * This class tests the CaseServiceImpl layer. It mocks out the layer below
 * (CaseServiceClientServiceImpl), which would deal with actually sending a HTTP request to the case
 * service.
 */
public class CaseServiceImplTest {

  @Mock ProductReference productReference;
  @Mock ContactCentreEventPublisher publisher;
  @Mock CaseServiceClientServiceImpl caseServiceClient;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks CaseService target = new CaseServiceImpl();

  private UUID uuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  private UUID uuid2 = UUID.fromString("b7565b5e-2222-2222-2222-918c0d3642ed");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void caseServiceGood() throws Exception {

    // The mocked productReference will return this product
    Product productFoundFixture =
        Product.builder()
            .caseType(Product.CaseType.H)
            .description("foobar")
            .fulfilmentCode("ABC123")
            .language("eng")
            .deliveryChannel(Product.DeliveryChannel.POST)
            .regions(new ArrayList<Product.Region>(List.of(Product.Region.E)))
            .requestChannels(
                new ArrayList<Product.RequestChannel>(
                    List.of(Product.RequestChannel.CC, Product.RequestChannel.FIELD)))
            .build();
    Mockito.when(productReference.searchProducts(any()))
        .thenReturn(new ArrayList<Product>(List.of(productFoundFixture)));

    PostalFulfilmentRequestDTO requestBodyDTOFixture = new PostalFulfilmentRequestDTO();

    requestBodyDTOFixture.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    requestBodyDTOFixture.setTitle("Mr");
    requestBodyDTOFixture.setForename("Mickey");
    requestBodyDTOFixture.setSurname("Mouse");
    requestBodyDTOFixture.setFulfilmentCode("ABC123");
    requestBodyDTOFixture.setDateTime(DateTimeUtil.nowUTC());

    UUID caseIdFixture = requestBodyDTOFixture.getCaseId();

    CaseContainerDTO caseContainerDTOFixture = new CaseContainerDTO();

    caseContainerDTOFixture.setRegion("E");

    String fulfilmentCodeFixture = requestBodyDTOFixture.getFulfilmentCode();
    Product.DeliveryChannel deliveryChannelFixture = Product.DeliveryChannel.POST;

    // execution - call the unit under test
    FulfilmentRequestedEvent fulfilmentRequestedEvent =
        target.searchProductsAndConstructEvent(
            fulfilmentCodeFixture, deliveryChannelFixture, caseContainerDTOFixture, caseIdFixture);

    // here you need to construct an Event to compare with the one returned...
    FulfilmentRequestedEvent fulfilmentRequestedEventFixture = new FulfilmentRequestedEvent();

    FulfilmentPayload fulfilmentPayloadFixture = fulfilmentRequestedEventFixture.getPayload();

    FulfilmentRequest fulfilmentRequestFixture = fulfilmentPayloadFixture.getFulfilmentRequest();

    fulfilmentRequestFixture.setFulfilmentCode(productFoundFixture.getFulfilmentCode());
    fulfilmentRequestFixture.setCaseId(caseIdFixture.toString());

    // verify the values of the fields within the fulfilmentRequestedEvent payload
    assertEquals(fulfilmentPayloadFixture, fulfilmentRequestedEvent.getPayload());
  }

  public void testGetHouseholdCaseByCaseId_withCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.H, true);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.H, false);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.C, true);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseId(CaseType.C, false);
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
    verifyCase(results.get(0), uuid2, CaseType.H, true);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.H, true);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.H, false);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.C, true);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.C, false);
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
    caseFromCaseService.get(0).setCaseType(CaseType.H.name());
    caseFromCaseService.get(1).setCaseType(CaseType.C.name());
    Mockito.when(caseServiceClient.getCaseByUprn(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    List<CaseDTO> results = target.getCaseByUPRN(uprn, requestParams);

    verifyCase(results.get(0), uuid, CaseType.H, caseEvents);
    verifyCase(results.get(1), uuid2, CaseType.C, caseEvents);
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
      assertEquals(2, results.getCaseEvents().size());
      CaseEventDTO event = results.getCaseEvents().get(0);
      assertEquals("Initial creation of case", event.getDescription());
      assertEquals("CASE_CREATED", event.getCategory());
      assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), event.getCreatedDateTime().getTime());
      event = results.getCaseEvents().get(1);
      assertEquals("Create Household Visit", event.getDescription());
      assertEquals("ACTION_CREATED", event.getCategory());
      assertEquals(asMillis("2019-05-16T12:12:12.343Z"), event.getCreatedDateTime().getTime());
    } else {
      assertNull(results.getCaseEvents());
    }
  }

  private long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);

    return dateParser.parse(datetime).getTime();
  }
}
