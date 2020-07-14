package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseByUPRN(UniquePropertyReferenceNumber, CaseQueryRequestDTO)
 * getCaseByUPRN}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplGetCaseByUprnTest extends CaseServiceImplTestBase {
  private static final String AN_ESTAB_UPRN = "334111111111";
  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber(334999999999L);

  // the actual census name & id as per the application.yml and also RM
  private static final String SURVEY_NAME = "CENSUS";
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @Before
  public void initMocks() {
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
  public void testGetCaseByUprn_withCaseDetails() throws Exception {
    doTestGetCaseByUprn(true);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetails() throws Exception {
    doTestGetCaseByUprn(false);
  }

  @Test
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet_noCachedCase()
      throws Exception {

    List<CaseContainerDTO> caseFromCaseService = casesFromCaseService();
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
    verifyEventNotSent();
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
    CachedCase cachedCase = FixtureHelper.loadPackageFixtures(CachedCase[].class).get(0);
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
  public void testGetCaseByUprn_mixedCaseTypes() throws Exception {

    // Build results to be returned from search
    List<CaseContainerDTO> caseFromCaseService = casesFromCaseService();
    caseFromCaseService.get(0).setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    assertEquals(1, results.size());

    CaseDTO expectedCaseResult = createExpectedCaseDTO(caseFromCaseService.get(1), caseEvents);
    verifyCase(results.get(0), expectedCaseResult, caseEvents);
  }

  @Test
  public void testGetCaseByUprn_caseSPG() throws Exception {
    doTestGetCasesByUprn("SPG");
  }

  @Test
  public void testGetCaseByUprn_caseHH() throws Exception {
    doTestGetCasesByUprn("HH");
  }

  @Test
  public void shouldGetSecureEstablishmentByUprn() throws Exception {

    Mockito.when(caseServiceClient.getCaseByUprn(eq(UPRN.getValue()), any()))
        .thenReturn(casesFromCaseService());

    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    assertEquals(2, results.size());
    assertTrue(results.get(1).isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.get(1).getEstabUprn());
  }

  private void doTestGetCasesByUprn(String caseType) throws Exception {
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(334999999999L);

    List<CaseContainerDTO> caseFromCaseService = casesFromCaseService();
    caseFromCaseService.get(0).setCaseType(caseType);
    Mockito.when(caseServiceClient.getCaseByUprn(eq(uprn.getValue()), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    List<CaseDTO> results = target.getCaseByUPRN(uprn, new CaseQueryRequestDTO(caseEvents));
    assertEquals(2, results.size());

    CaseDTO expectedCaseResult = createExpectedCaseDTO(caseFromCaseService.get(0), caseEvents);
    verifyCase(results.get(0), expectedCaseResult, caseEvents);
  }

  private void doTestGetCaseByUprn(boolean caseEvents) throws Exception {
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
    CaseDTO expectedCaseResult0 = createExpectedCaseDTO(caseFromCaseService.get(0), caseEvents);
    verifyCase(results.get(0), expectedCaseResult0, caseEvents);

    CaseDTO expectedCaseResult1 = createExpectedCaseDTO(caseFromCaseService.get(1), caseEvents);
    verifyCase(results.get(1), expectedCaseResult1, caseEvents);
  }

  private UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  private CaseDTO createExpectedCaseDTO(CaseContainerDTO caseFromCaseService, boolean caseEvents) {

    CaseDTO expectedCaseResult =
        CaseDTO.builder()
            .id(caseFromCaseService.getId())
            .caseRef(caseFromCaseService.getCaseRef())
            .caseType(caseFromCaseService.getCaseType())
            .estabType(EstabType.forCode(caseFromCaseService.getEstabType()))
            .estabDescription(caseFromCaseService.getEstabType())
            .allowedDeliveryChannels(ALL_DELIVERY_CHANNELS)
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
            .secureEstablishment(caseFromCaseService.isSecureEstablishment())
            .build();
    if (caseEvents) {
      List<CaseEventDTO> expectedCaseEvents =
          caseFromCaseService
              .getCaseEvents()
              .stream()
              .filter(e -> !e.getDescription().contains("Should be filtered out"))
              .map(
                  e ->
                      CaseEventDTO.builder()
                          .description(e.getDescription())
                          .category(e.getEventType())
                          .createdDateTime(e.getCreatedDateTime())
                          .build())
              .collect(Collectors.toList());
      expectedCaseResult.setCaseEvents(expectedCaseEvents);
    }
    return expectedCaseResult;
  }

  private void verifyCase(CaseDTO results, CaseDTO expectedCaseResult, boolean caseEventsExpected)
      throws Exception {
    assertEquals(expectedCaseResult.getId(), results.getId());
    assertEquals(expectedCaseResult.getCaseRef(), results.getCaseRef());
    assertEquals(expectedCaseResult.getCaseType(), results.getCaseType());
    assertEquals(expectedCaseResult.getCeOrgName(), results.getCeOrgName());
    assertEquals(
        expectedCaseResult.getAllowedDeliveryChannels(), results.getAllowedDeliveryChannels());

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
    verifyEventNotSent();
  }

  private void verifyNewCase(AddressIndexAddressCompositeDTO address, CaseDTO result)
      throws Exception {

    Mockito.verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    Mockito.verify(dataRepo, times(1))
        .readCachedCaseByUPRN(any(UniquePropertyReferenceNumber.class));
    Mockito.verify(addressSvc, times(1)).uprnQuery(anyLong());

    // Verify content of case written to Firestore
    ArgumentCaptor<CachedCase> cachedCaseCaptor = ArgumentCaptor.forClass(CachedCase.class);
    Mockito.verify(dataRepo, times(1)).writeCachedCase(cachedCaseCaptor.capture());
    CachedCase capturedCase = cachedCaseCaptor.getValue();
    verifyCachedCaseContent(address, result.getId(), CaseType.HH, capturedCase);

    // Verify response
    CachedCase cachedCase = mapperFacade.map(address, CachedCase.class);
    cachedCase.setId(result.getId().toString());
    verifyCaseDTOContent(cachedCase, CaseType.HH.name(), false, result);

    // Verify the NewAddressEvent
    CollectionCaseNewAddress newAddress = mapperFacade.map(address, CollectionCaseNewAddress.class);
    newAddress.setId(cachedCase.getId());
    verifyNewAddressEventSent(
        address.getCensusAddressType(), address.getCensusEstabType(), newAddress);
  }

  private void verifyCachedCaseContent(
      AddressIndexAddressCompositeDTO expectedAddress,
      UUID expectedId,
      CaseType expectedCaseType,
      CachedCase actualCapturedCase) {
    assertEquals(expectedId.toString(), actualCapturedCase.getId());
    assertEquals(expectedAddress.getUprn(), actualCapturedCase.getUprn());
    assertEquals(expectedAddress.getFormattedAddress(), actualCapturedCase.getFormattedAddress());
    assertEquals(expectedAddress.getAddressLine1(), actualCapturedCase.getAddressLine1());
    assertEquals(expectedAddress.getAddressLine2(), actualCapturedCase.getAddressLine2());
    assertEquals(expectedAddress.getAddressLine3(), actualCapturedCase.getAddressLine3());
    assertEquals(expectedAddress.getTownName(), actualCapturedCase.getTownName());
    assertEquals(expectedAddress.getPostcode(), actualCapturedCase.getPostcode());
    assertEquals(expectedAddress.getCensusAddressType(), actualCapturedCase.getAddressType());
    assertEquals(expectedCaseType, actualCapturedCase.getCaseType());
    assertEquals(expectedAddress.getCensusEstabType(), actualCapturedCase.getEstabType());
    assertEquals(expectedAddress.getCountryCode(), actualCapturedCase.getRegion());
    assertEquals(expectedAddress.getOrganisationName(), actualCapturedCase.getCeOrgName());
    assertEquals(0, actualCapturedCase.getCaseEvents().size());
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
    expectedNewCaseResult.setCaseEvents(new ArrayList<CaseEventDTO>());
    assertEquals(expectedNewCaseResult, actualCaseDto);
  }

  private void verifyNewAddressEventSent(
      String expectedAddressType,
      String expectedEstabTypeCode,
      CollectionCaseNewAddress newAddress) {
    newAddress.setCaseType(expectedAddressType);
    newAddress.setSurvey(SURVEY_NAME);
    newAddress.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    newAddress.setCeExpectedCapacity(0);
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
    NewAddress payloadSent = verifyEventSent(EventType.NEW_ADDRESS_REPORTED, NewAddress.class);
    assertEquals(payload, payloadSent);
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
    verifyEventNotSent();
  }

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }
}
