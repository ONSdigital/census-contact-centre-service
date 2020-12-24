package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.Fulfilments;
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

  private static final String BLACK_LISTED_FULFILMENT_CODE = "P_TB_TBBEN1";

  @Before
  public void setup() {
    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
    Mockito.when(appConfig.getSurveyName()).thenReturn("CENSUS");

    Fulfilments fulfilments = new Fulfilments();
    fulfilments.setBlacklistedCodes(Set.of(BLACK_LISTED_FULFILMENT_CODE));
    Mockito.when(appConfig.getFulfilments()).thenReturn(fulfilments);
  }

  @Test
  public void testFulfilmentRequestByPost_individualFailsWithNullContactDetails() throws Exception {
    // All of the following fail validation because one of the contact detail fields is always null
    // or empty
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", null, "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", "", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", "John", null, true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.HH, "Mr", "John", "", true);

    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", null, "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", "", "Smith", true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", "John", null, true);
    doVerifyFulfilmentRequestByPostFailsValidation(Product.CaseType.CE, "Mr", "John", "", true);
  }

  @Test
  public void testFulfilmentRequestByPost_individualAllowsNullTitle() throws Exception {
    // Test that individual cases allow null/empty contact details
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, null, "Bill", "Bloggs", true, false);
    doFulfilmentRequestByPostSuccess(Product.CaseType.HH, "", "Bill", "Bloggs", true, false);

    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, null, "Bill", "Bloggs", true, false);
    doFulfilmentRequestByPostSuccess(Product.CaseType.CE, "", "Bill", "Bloggs", true, false);
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
  public void testFulfilmentRequestByPost_blackListedFulfilmentCode() throws Exception {
    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_0, "Mr", "Mickey", "Mouse");
    requestBodyDTOFixture.setFulfilmentCode(BLACK_LISTED_FULFILMENT_CODE);

    try {
      target.fulfilmentRequestByPost(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("deprecated"));
      assertEquals(Fault.BAD_REQUEST, e.getFault());
    }
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
  public void testFulfilmentRequestBySMS_blackListedFulfilmentCode() throws Exception {
    CaseContainerDTO caseData = casesFromCaseService().get(1);
    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);
    requestBodyDTOFixture.setFulfilmentCode(BLACK_LISTED_FULFILMENT_CODE);

    try {
      target.fulfilmentRequestBySMS(requestBodyDTOFixture);
      fail();
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("deprecated"));
      assertEquals(Fault.BAD_REQUEST, e.getFault());
    }
  }

  @Test
  public void testFulfilmentRequestBySMS_caseSvcResponseCaseWithSurveyTypeCCS()
      throws CTPException {
    CaseContainerDTO caseData = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_1), any())).thenReturn(caseData);
    SMSFulfilmentRequestDTO requestBodyDTOFixture = getSMSFulfilmentRequestDTO(caseData);
    CTPException e =
        assertThrows(
            CTPException.class, () -> target.fulfilmentRequestBySMS(requestBodyDTOFixture));
    assertEquals(CTPException.Fault.BAD_REQUEST, e.getFault());
    assertEquals("Operation not permissible for a CCS Case", e.getMessage());
  }

  @Test
  public void testFulfilmentRequestByPost_caseSvcResponseCaseWithSurveyTypeCCS()
      throws CTPException {
    CaseContainerDTO caseData = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_1), any())).thenReturn(caseData);
    PostalFulfilmentRequestDTO requestBodyDTOFixture =
        getPostalFulfilmentRequestDTO(UUID_1, "Mr", "Mickey", "Mouse");
    CTPException e =
        assertThrows(
            CTPException.class, () -> target.fulfilmentRequestByPost(requestBodyDTOFixture));
    assertEquals(CTPException.Fault.BAD_REQUEST, e.getFault());
    assertEquals("Operation not permissible for a CCS Case", e.getMessage());
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
    caseFromCaseService.setCaseType(caseType.name());
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
    FulfilmentRequest actualFulfilmentRequest =
        verifyEventSent(EventType.FULFILMENT_REQUESTED, FulfilmentRequest.class);
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
    FulfilmentRequest actualFulfilmentRequest =
        verifyEventSent(EventType.FULFILMENT_REQUESTED, FulfilmentRequest.class);
    assertEquals(
        requestBodyDTOFixture.getFulfilmentCode(), actualFulfilmentRequest.getFulfilmentCode());
    assertEquals(requestBodyDTOFixture.getCaseId().toString(), actualFulfilmentRequest.getCaseId());

    if (caseType == Product.CaseType.HH && individual) {
      assertNotNull(actualFulfilmentRequest.getIndividualCaseId());
    } else {
      assertNull(actualFulfilmentRequest.getIndividualCaseId());
    }

    Contact actualContact = actualFulfilmentRequest.getContact();
    assertNull(actualContact.getTitle());
    assertNull(actualContact.getForename());
    assertNull(actualContact.getSurname());
    assertEquals(requestBodyDTOFixture.getTelNo(), actualContact.getTelNo());
  }

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }

  private CachedCase caseFromRepository() {
    return FixtureHelper.loadPackageFixtures(CachedCase[].class).get(0);
  }
}
