package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * This class tests the CaseServiceImpl layer. It mocks out the layer below
 * (CaseServiceClientServiceImpl), which would deal with actually sending a HTTP request to the case
 * service.
 */
public class CaseServiceImplTest {

  private static final String REQUEST_DATE_TIME = "2017-02-11T16:32:11.863";

  @Mock ProductReference productReference;
  @Mock ContactCentreEventPublisher publisher;
  @Mock CaseServiceClientServiceImpl CaseServiceClientService = new CaseServiceClientServiceImpl();

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks CaseService target = new CaseServiceImpl();

  private UUID uuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void caseServiceGood() throws Exception {

    // The mocked productReference will return this product
    Product returnedProduct =
        Product.builder()
            .caseType(Product.CaseType.H)
            .description("foobar")
            .fulfilmentCode("ABC123")
            .language("eng")
            .deliveryChannel(Product.DeliveryChannel.POST)
            .regions(new ArrayList<Product.Region>(List.of(Product.Region.E, Product.Region.W)))
            .requestChannels(
                new ArrayList<Product.RequestChannel>(
                    List.of(Product.RequestChannel.CC, Product.RequestChannel.FIELD)))
            .build();
    Mockito.when(productReference.searchProducts(any()))
        .thenReturn(new ArrayList<Product>(List.of(returnedProduct)));

    UUID caseIdFixture = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    PostalFulfilmentRequestDTO requestBodyDTOFixture = new PostalFulfilmentRequestDTO();

    requestBodyDTOFixture.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    requestBodyDTOFixture.setTitle("Mr");
    requestBodyDTOFixture.setForename("Mickey");
    requestBodyDTOFixture.setSurname("Mouse");
    requestBodyDTOFixture.setProductCode("P_OR_H1");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    requestBodyDTOFixture.setDateTime(LocalDateTime.parse(REQUEST_DATE_TIME, formatter));

    // execution - call the first unit under test
    ResponseDTO responseDTO = target.fulfilmentRequestByPost(caseIdFixture, requestBodyDTOFixture);

    // verify the value of the id in the ResponseDTO
    assertEquals(responseDTO.getId(), caseIdFixture.toString());

    // execution - call the second unit under test
    String fulfilmentCodeFixture = requestBodyDTOFixture.getProductCode();
    Product.DeliveryChannel deliveryChannelFixture = Product.DeliveryChannel.POST;

    FulfilmentRequestedEvent fulfilmentRequestedEvent =
        target.searchProductsAndConstructEvent(fulfilmentCodeFixture, deliveryChannelFixture);

    // here you need to construct an Event to compare with the one returned...
    FulfilmentRequestedEvent fulfilmentRequestedEventFixture = new FulfilmentRequestedEvent();

    FulfilmentPayload fulfilmentPayloadFixture = fulfilmentRequestedEventFixture.getPayload();

    FulfilmentRequest fulfilmentRequestFixture = fulfilmentPayloadFixture.getFulfilmentRequest();

    fulfilmentRequestFixture.setFulfilmentCode("XXXXXX-XXXXXX");
    fulfilmentRequestFixture.setCaseId("bbd55984-0dbf-4499-bfa7-0aa4228700e9");

    Address addressFixture = fulfilmentRequestFixture.getAddress();
    addressFixture.setAddressLine1("1 main street");
    addressFixture.setAddressLine2("upper upperingham");
    addressFixture.setAddressLine3("");
    addressFixture.setTownName("upton");
    addressFixture.setPostcode("UP103UP");
    addressFixture.setRegion("E");
    addressFixture.setLatitude("50.863849");
    addressFixture.setLongitude("-1.229710");
    addressFixture.setUprn("XXXXXXXXXXXXX");
    addressFixture.setArid("XXXXX");
    addressFixture.setAddressType("CE");
    addressFixture.setEstabType("XXX");

    Contact contactFixture = fulfilmentRequestFixture.getContact();
    contactFixture.setTitle("Ms");
    contactFixture.setForename("jo");
    contactFixture.setSurname("smith");
    contactFixture.setEmail("me@example.com");
    contactFixture.setTelNo("+447890000000");

    Header headerFixture = new Header();
    headerFixture.setType("FULFILMENT_REQUESTED");
    headerFixture.setSource("CONTACT_CENTRE_API");
    headerFixture.setChannel("CC");
    //    headerFixture.setDateTime(DateTimeUtil.getCurrentDateTimeInJsonFormat());
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    fulfilmentRequestedEventFixture.setEvent(headerFixture);

    // verify the values of the fields within the fulfilmentRequestedEvent header
    assertEquals(headerFixture, fulfilmentRequestedEvent.getEvent());

    // verify the values of the fields within the fulfilmentRequestedEvent payload
    assertEquals(fulfilmentPayloadFixture, fulfilmentRequestedEvent.getPayload());
  }

  public void testGetCaseByCaseId_withCaseDetails() throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(uuid, requestParams);

    verifyCase(results, caseEvents);
  }

  @Test
  public void testGetCaseByCaseId_withNoCaseDetails() throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request, with caseEvents turned off
    boolean caseEvents = false;
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(uuid, requestParams);

    verifyCase(results, caseEvents);
  }

  @Test
  public void testGetCaseByCaseId_nonHouseholdCase() throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType("X"); // Not household case
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseById(uuid, new CaseRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is a non-household case", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  private void verifyCase(CaseDTO results, boolean caseEventsExpected) throws ParseException {
    assertEquals(uuid, results.getId());
    assertEquals("1000000000000001", results.getCaseRef());
    assertEquals("H", results.getCaseType());
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
