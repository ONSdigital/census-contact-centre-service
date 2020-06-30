package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#fulfilmentRequestByPost(PostalFulfilmentRequestDTO)
 * fulfilmentRequestByPost} and {@link CaseService#fulfilmentRequestBySMS(SMSFulfilmentRequestDTO)
 * fulfilmentRequestBySMS}
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplFulfilmentTest extends CaseServiceImplTestBase {

  private static final EventType FULFILMENT_EVENT_TYPE_FIELD_VALUE = EventType.FULFILMENT_REQUESTED;
  private static final Source FULFILMENT_SOURCE_FIELD_VALUE = Source.CONTACT_CENTRE_API;
  private static final Channel FULFILMENT_CHANNEL_FIELD_VALUE = Channel.CC;

  @Before
  public void setup() {
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
        .sendEventWithPersistance(
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
        .sendEventWithPersistance(
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
