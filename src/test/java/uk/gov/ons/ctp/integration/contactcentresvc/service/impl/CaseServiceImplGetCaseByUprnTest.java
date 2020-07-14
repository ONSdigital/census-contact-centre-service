package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

  List<CaseContainerDTO> casesFromRm;
  List<CachedCase> casesFromCache;
  private AddressIndexAddressCompositeDTO addressFromAI;

  @Before
  public void setup() {
    // For case retrieval, mock out a whitelist of allowable case events
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_CREATED", "CASE_UPDATED");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);

    when(appConfig.getChannel()).thenReturn(Channel.CC);
    when(appConfig.getSurveyName()).thenReturn(SURVEY_NAME);
    when(appConfig.getCollectionExerciseId()).thenReturn(COLLECTION_EXERCISE_ID);

    casesFromRm = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
    casesFromCache = FixtureHelper.loadPackageFixtures(CachedCase[].class);
    addressFromAI = FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(true);
    verifyCase(result, true, 0);
  }

  @Test
  public void testGetCaseByUprn_withCaseDetailsForCaseTypeCE() throws Exception {
    casesFromRm.get(1).setCaseType(CaseType.CE.name());
    makeSecondRmCaseTheLatest();
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(true);
    verifyCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeHH() throws Exception {
    casesFromRm.get(0).setCaseType(CaseType.HH.name());
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(false);
    verifyCase(result, false, 0);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseDetailsForCaseTypeCE() throws Exception {
    casesFromRm.get(1).setCaseType(CaseType.CE.name());
    makeSecondRmCaseTheLatest();
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(false);
    verifyCase(result, false, 1);
  }

  @Test
  public void testGetCaseByUprn_householdIndividualCase_emptyResultSet_noCachedCase()
      throws Exception {

    casesFromRm.get(0).setCaseType("HI");
    casesFromRm.get(1).setCaseType("HI");

    mockCasesFromRm(UPRN);
    mockNothingInTheCache();
    mockAddressFromAI();

    CaseDTO result = getCasesByUprn(false);
    verifyNewCase(result);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_HH() throws Exception {

    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    mockNothingInTheCache();
    mockAddressFromAI();

    CaseDTO result = getCasesByUprn(false);
    verifyNewCase(result);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_SPG() throws Exception {

    addressFromAI.setCensusEstabType("marina");

    mockNothingInRm();
    mockNothingInTheCache();
    mockAddressFromAI();

    CaseDTO result = getCasesByUprn(false);
    verifyNewCase(result);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_CE() throws Exception {

    addressFromAI.setCensusEstabType("CARE HOME");

    mockNothingInRm();
    mockNothingInTheCache();
    mockAddressFromAI();

    CaseDTO result = getCasesByUprn(false);
    verifyNewCase(result);
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_NA() throws Exception {

    addressFromAI.setCensusEstabType("NA");

    mockNothingInRm();
    mockNothingInTheCache();
    mockAddressFromAI();

    CaseDTO result = getCasesByUprn(false);
    verifyNewCase(result);
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_addressServiceNotFound()
      throws Exception {

    mockNothingInRm();
    mockNothingInTheCache();

    doThrow(new CTPException(Fault.RESOURCE_NOT_FOUND)).when(addressSvc).uprnQuery(UPRN.getValue());
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
    verify(caseServiceClient, times(1)).getCaseByUprn(any(Long.class), any(Boolean.class));
    verifyHasReadCachedCases();
    verifyNotWrittenCachedCase();
    verify(addressSvc, times(1)).uprnQuery(anyLong());
    verifyEventNotSent();
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_addressSvcRestClientException()
      throws Exception {

    mockNothingInRm();
    mockNothingInTheCache();

    doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(addressSvc)
        .uprnQuery(eq(UPRN.getValue()));

    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_noCachedCase_scottishAddress()
      throws Exception {

    addressFromAI.setCountryCode("S");

    mockNothingInRm();
    mockNothingInTheCache();
    mockAddressFromAI();

    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test
  public void testGetCaseByUprn_caseSvcNotFoundResponse_cachedCase() throws Exception {
    mockNothingInRm();
    mockCachedCase();
    CaseDTO result = getCasesByUprn(false);
    verifyCachedCase(result);
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetCaseByUprn_caseSvcRestClientException() throws Exception {

    doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());

    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test(expected = CTPException.class)
  public void testGetCaseByUprn_caseSvcNotFoundResponse_NoCachedCase_RetriesExhausted()
      throws Exception {

    mockNothingInRm();
    mockNothingInTheCache();
    mockAddressFromAI();

    doThrow(new CTPException(Fault.SYSTEM_ERROR, new Exception(), "Retries exhausted"))
        .when(dataRepo)
        .writeCachedCase(any());
    target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(false));
  }

  @Test
  public void testGetCaseByUprn_mixedCaseTypes() throws Exception {
    casesFromRm.get(0).setCaseType("HI"); // Household Individual case
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(true);
    verifyCase(result, true, 1);
  }

  @Test
  public void testGetCaseByUprn_caseSPG() throws Exception {
    casesFromRm.get(0).setCaseType("SPG");
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(true);
    verifyCase(result, true, 0);
  }

  @Test
  public void testGetCaseByUprn_caseHH() throws Exception {
    casesFromRm.get(0).setCaseType("HH");
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(true);
    verifyCase(result, true, 0);
  }

  @Test
  public void shouldGetSecureEstablishmentByUprn() throws Exception {
    makeSecondRmCaseTheLatest();
    mockCasesFromRm(UPRN);
    CaseDTO result = getCasesByUprn(false);
    assertTrue(result.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), result.getEstabUprn());
  }

  private UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  private void makeSecondRmCaseTheLatest() {
    LocalDateTime firstRmCaseLastUpdated =
        LocalDateTime.ofInstant(casesFromRm.get(0).getLastUpdated().toInstant(), ZoneOffset.UTC);

    Date laterDate = Date.from(firstRmCaseLastUpdated.plusDays(1).toInstant(ZoneOffset.UTC));
    casesFromRm.get(1).setLastUpdated(laterDate);
  }

  private void mockCasesFromRm(UniquePropertyReferenceNumber uprn) {
    when(caseServiceClient.getCaseByUprn(eq(uprn.getValue()), any())).thenReturn(casesFromRm);
  }

  private void mockNothingInRm() {
    doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseByUprn(eq(UPRN.getValue()), any());
  }

  private void verifyCallToGetCasesFromRm() {
    verify(caseServiceClient).getCaseByUprn(any(Long.class), any(Boolean.class));
  }

  private void mockCachedCase() throws Exception {
    when(dataRepo.readCachedCasesByUprn(UPRN)).thenReturn(List.of(casesFromCache.get(0)));
  }

  private void mockNothingInTheCache() throws Exception {
    when(dataRepo.readCachedCasesByUprn(UPRN)).thenReturn(new ArrayList<>());
  }

  private void verifyHasReadCachedCases() throws Exception {
    verify(dataRepo).readCachedCasesByUprn(any(UniquePropertyReferenceNumber.class));
  }

  private CachedCase verifyHasWrittenCachedCase() throws Exception {
    ArgumentCaptor<CachedCase> cachedCaseCaptor = ArgumentCaptor.forClass(CachedCase.class);
    verify(dataRepo).writeCachedCase(cachedCaseCaptor.capture());
    return cachedCaseCaptor.getValue();
  }

  private void verifyNotWrittenCachedCase() throws Exception {
    verify(dataRepo, never()).writeCachedCase(any());
  }

  private void mockAddressFromAI() throws Exception {
    when(addressSvc.uprnQuery(UPRN.getValue())).thenReturn(addressFromAI);
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

  private void verifyCase(CaseDTO results, boolean caseEventsExpected, int dataIndex)
      throws Exception {
    CaseDTO expectedCaseResult =
        createExpectedCaseDTO(casesFromRm.get(dataIndex), caseEventsExpected);
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
    verifyHasReadCachedCases();
    verifyNotWrittenCachedCase();
    verify(addressSvc, never()).uprnQuery(anyLong());
    verifyEventNotSent();
  }

  private void verifyNewCase(CaseDTO result) throws Exception {

    verifyCallToGetCasesFromRm();
    verifyHasReadCachedCases();
    verify(addressSvc, times(1)).uprnQuery(anyLong());

    // Verify content of case written to Firestore
    CachedCase capturedCase = verifyHasWrittenCachedCase();
    verifyCachedCaseContent(result.getId(), CaseType.HH, capturedCase);

    // Verify response
    CachedCase cachedCase = mapperFacade.map(addressFromAI, CachedCase.class);
    cachedCase.setId(result.getId().toString());
    verifyCaseDTOContent(cachedCase, CaseType.HH.name(), false, result);

    // Verify the NewAddressEvent
    CollectionCaseNewAddress newAddress =
        mapperFacade.map(addressFromAI, CollectionCaseNewAddress.class);
    newAddress.setId(cachedCase.getId());
    verifyNewAddressEventSent(
        addressFromAI.getCensusAddressType(), addressFromAI.getCensusEstabType(), newAddress);
  }

  private void verifyCachedCaseContent(
      UUID expectedId, CaseType expectedCaseType, CachedCase actualCapturedCase) {
    assertEquals(expectedId.toString(), actualCapturedCase.getId());
    assertEquals(addressFromAI.getUprn(), actualCapturedCase.getUprn());
    assertEquals(addressFromAI.getFormattedAddress(), actualCapturedCase.getFormattedAddress());
    assertEquals(addressFromAI.getAddressLine1(), actualCapturedCase.getAddressLine1());
    assertEquals(addressFromAI.getAddressLine2(), actualCapturedCase.getAddressLine2());
    assertEquals(addressFromAI.getAddressLine3(), actualCapturedCase.getAddressLine3());
    assertEquals(addressFromAI.getTownName(), actualCapturedCase.getTownName());
    assertEquals(addressFromAI.getPostcode(), actualCapturedCase.getPostcode());
    assertEquals(addressFromAI.getCensusAddressType(), actualCapturedCase.getAddressType());
    assertEquals(expectedCaseType, actualCapturedCase.getCaseType());
    assertEquals(addressFromAI.getCensusEstabType(), actualCapturedCase.getEstabType());
    assertEquals(addressFromAI.getCountryCode(), actualCapturedCase.getRegion());
    assertEquals(addressFromAI.getOrganisationName(), actualCapturedCase.getCeOrgName());
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

  private void verifyCachedCase(CaseDTO result) throws Exception {
    CachedCase cachedCase = casesFromCache.get(0);

    CaseDTO expectedResult = mapperFacade.map(cachedCase, CaseDTO.class);
    expectedResult.setCaseType(CaseType.HH.name());
    expectedResult.setEstabType(EstabType.forCode(cachedCase.getEstabType()));
    expectedResult.setAllowedDeliveryChannels(Arrays.asList(DeliveryChannel.values()));

    assertEquals(expectedResult, result);

    verifyCallToGetCasesFromRm();
    verifyHasReadCachedCases();
    verifyNotWrittenCachedCase();
    verify(addressSvc, never()).uprnQuery(anyLong());
    verifyEventNotSent();
  }

  private CaseDTO getCasesByUprn(boolean caseEvents) throws CTPException {
    List<CaseDTO> results = target.getCaseByUPRN(UPRN, new CaseQueryRequestDTO(caseEvents));
    assertEquals(1, results.size());
    return results.get(0);
  }
}
