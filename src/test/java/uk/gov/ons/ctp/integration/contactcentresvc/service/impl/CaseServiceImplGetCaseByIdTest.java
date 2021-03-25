package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#getCaseById(UUID, CaseQueryRequestDTO) getCaseById}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplGetCaseByIdTest extends CaseServiceImplTestBase {
  private static final boolean CASE_EVENTS_TRUE = true;
  private static final boolean CASE_EVENTS_FALSE = false;

  private static final boolean USE_CACHED_CASE = true;
  private static final boolean NO_CACHED_CASE = false;

  private static final String AN_ESTAB_UPRN = "334111111111";

  private static final String CACHED_CASE_UPRN_0 = "1347459987";
  private static final String RM_CASE_UPRN_0 = "1347459988";

  @Before
  public void setup() {
    mockCaseEventWhiteList();
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withCaseDetails() {
    doTestGetCaseByCaseId(CaseType.HH, CASE_EVENTS_TRUE, NO_CACHED_CASE);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withNoCaseDetails() {
    doTestGetCaseByCaseId(CaseType.HH, CASE_EVENTS_FALSE, NO_CACHED_CASE);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withCaseDetails() {
    doTestGetCaseByCaseId(CaseType.CE, CASE_EVENTS_TRUE, NO_CACHED_CASE);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withNoCaseDetails() {
    doTestGetCaseByCaseId(CaseType.CE, CASE_EVENTS_FALSE, NO_CACHED_CASE);
  }

  @Test
  public void testGetCaseByCaseId_caseSPG() {
    doTestGetCaseByCaseId(CaseType.SPG, CASE_EVENTS_FALSE, NO_CACHED_CASE);
  }

  @Test
  public void testGetCaseByCaseId_caseSPG_fromCache() {
    doTestGetCaseByCaseId(CaseType.SPG, CASE_EVENTS_FALSE, USE_CACHED_CASE);
  }

  @Test
  public void testGetCaseByCaseId_caseSPG_fromCacheWithEvents() {
    doTestGetCaseByCaseId(CaseType.SPG, CASE_EVENTS_TRUE, USE_CACHED_CASE);
  }

  @Test
  public void shouldAdaptNullEstabTypeToHousehold() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setEstabType(null);
    caseFromCaseService.setCaseType(CaseType.HH.name());
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);
    assertEquals(EstabType.HOUSEHOLD, results.getEstabType());
  }

  @Test
  public void shouldAdaptNullEstabTypeToOther() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(CaseType.CE.name());
    caseFromCaseService.setEstabType(null);
    Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);

    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(true);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);
    assertEquals(EstabType.OTHER, results.getEstabType());
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
  public void testGetLatestFromCacheWhenResultsFromBothRmAndCache() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    List<CachedCase> listCachedCases = FixtureHelper.loadPackageFixtures(CachedCase[].class);
    CachedCase caseFromRepository = listCachedCases.get(0);

    setUpCachedCase(caseFromRepository, UUID_0.toString(), CACHED_CASE_UPRN_0);
    setUpCaseFromCaseService(caseFromCaseService, UUID_0, RM_CASE_UPRN_0);

    // Make sure that expected case has the most recent date
    caseFromRepository.setCreatedDateTime(new Date());

    doGetCaseById(caseFromCaseService, caseFromRepository, UUID_0, CACHED_CASE_UPRN_0);
  }

  @Test
  public void testGetLatestFromCacheWhenResultsFromBothRmAndCacheWithSmallTimeDifference()
      throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    List<CachedCase> listCachedCases = FixtureHelper.loadPackageFixtures(CachedCase[].class);
    CachedCase caseFromRepository = listCachedCases.get(0);

    setUpCachedCase(caseFromRepository, UUID_0.toString(), CACHED_CASE_UPRN_0);
    setUpCaseFromCaseService(caseFromCaseService, UUID_0, RM_CASE_UPRN_0);

    // Make sure that expected case has the most recent date and that it is only 1 second closer
    // than the next most recent date
    caseFromRepository.setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 6)));
    caseFromCaseService.setLastUpdated(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 5)));

    doGetCaseById(caseFromCaseService, caseFromRepository, UUID_0, CACHED_CASE_UPRN_0);
  }

  @Test
  public void testGetLatestFromRmWhenResultsFromBothRmAndCacheWithSmallTimeDifferences()
      throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    List<CachedCase> listCachedCases = FixtureHelper.loadPackageFixtures(CachedCase[].class);
    CachedCase caseFromRepository = listCachedCases.get(0);

    setUpCachedCase(caseFromRepository, UUID_0.toString(), CACHED_CASE_UPRN_0);
    setUpCaseFromCaseService(caseFromCaseService, UUID_0, RM_CASE_UPRN_0);

    // Make sure that expected case has the most recent date and that it is only 1 second closer
    // than the next most recent date
    caseFromCaseService.setLastUpdated(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 6)));
    caseFromRepository.setCreatedDateTime(utcDate(LocalDateTime.of(2020, 2, 3, 10, 4, 5)));

    doGetCaseById(caseFromCaseService, caseFromRepository, UUID_0, RM_CASE_UPRN_0);
  }

  @Test
  public void testGetLatestFromRmWhenResultsFromBothRmAndCache() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    List<CachedCase> listCachedCases = FixtureHelper.loadPackageFixtures(CachedCase[].class);
    CachedCase caseFromRepository = listCachedCases.get(0);

    setUpCachedCase(caseFromRepository, UUID_0.toString(), CACHED_CASE_UPRN_0);
    setUpCaseFromCaseService(caseFromCaseService, UUID_0, RM_CASE_UPRN_0);

    // Make sure that expected case has the most recent date
    caseFromCaseService.setLastUpdated(new Date());

    doGetCaseById(caseFromCaseService, caseFromRepository, UUID_0, RM_CASE_UPRN_0);
  }

  @Test
  public void testRMCaseNotFound() throws CTPException {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    List<CachedCase> casesFromRepository = FixtureHelper.loadPackageFixtures(CachedCase[].class);

    setUpCachedCase(casesFromRepository.get(0), UUID_0.toString(), CACHED_CASE_UPRN_0);
    setUpCaseFromCaseService(caseFromCaseService, UUID_0, RM_CASE_UPRN_0);

    // The RM case should not be found so it should not matter if it has the most recent date
    caseFromCaseService.setLastUpdated(new Date());

    doGetCaseByIdNotFoundInRM(caseFromCaseService, casesFromRepository, UUID_0, CACHED_CASE_UPRN_0);
  }

  @Test
  public void testRMAndCachedCaseNotFound() throws CTPException {
    doGetCaseByIdNotFound(UUID_0);
  }

  @Test
  public void testHandleErrorFromRM() throws CTPException {
    doGetCaseByIdGetsError(UUID_0);
  }

  @SneakyThrows
  private void doTestGetCaseByCaseId(CaseType caseType, boolean caseEvents, boolean cached) {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    CachedCase caseFromRepository;
    CaseDTO expectedCaseResult;

    if (cached) {
      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
          .when(caseServiceClient)
          .getCaseById(eq(UUID_0), any());

      List<CachedCase> casesFromRepository = FixtureHelper.loadPackageFixtures(CachedCase[].class);
      caseFromRepository = casesFromRepository.get(1);
      caseFromRepository.setCaseType(caseType);
      Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0)))
          .thenReturn(Optional.of(caseFromRepository));

      expectedCaseResult = mapperFacade.map(caseFromRepository, CaseDTO.class);

      // We need to account for the mapping from a CachedCase to a CaseDTO missing a few fields:
      expectedCaseResult.setAllowedDeliveryChannels(ALL_DELIVERY_CHANNELS);
      expectedCaseResult.setEstabType(EstabType.CARE_HOME);
      if (!caseEvents) {
        expectedCaseResult.setCaseEvents(Collections.emptyList());
      }

    } else {
      Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any()))
          .thenReturn(caseFromCaseService);
      expectedCaseResult = createExpectedCaseDTO(caseFromCaseService, caseEvents);
    }

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);

    verifyCase(results, expectedCaseResult, caseEvents);

    if (cached) {
      assertEquals(asMillis("2020-06-12T11:55:23.195Z"), results.getCreatedDateTime().getTime());
    } else {
      assertEquals(
          asMillis("2019-05-14T16:11:41.343+01:00"), results.getCreatedDateTime().getTime());
    }
  }

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }

  private Date utcDate(LocalDateTime dateTime) {
    return Date.from(dateTime.toInstant(ZoneOffset.UTC));
  }

  private void doGetCaseById(
      CaseContainerDTO caseFromCaseService,
      CachedCase caseFromRepository,
      UUID caseId,
      String expectedUprn)
      throws CTPException {
    Mockito.when(caseServiceClient.getCaseById(eq(caseId), any())).thenReturn(caseFromCaseService);
    Mockito.when(dataRepo.readCachedCaseById(eq(caseId)))
        .thenReturn(Optional.of(caseFromRepository));

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    CaseDTO results = target.getCaseById(caseId, requestParams);

    // Check the value of the uprn to confirm that it is the expected case that has been returned
    assertEquals(new UniquePropertyReferenceNumber(expectedUprn), results.getUprn());
  }

  private void doGetCaseByIdNotFoundInRM(
      CaseContainerDTO caseFromCaseService,
      List<CachedCase> casesFromRepository,
      UUID caseId,
      String expectedUprn)
      throws CTPException {
    Mockito.when(caseServiceClient.getCaseById(eq(caseId), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)); // Not in RM
    Mockito.when(dataRepo.readCachedCaseById(eq(caseId)))
        .thenReturn(Optional.of(casesFromRepository.get(0)));

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    CaseDTO results = target.getCaseById(caseId, requestParams);

    // Check the value of the uprn to confirm that it is the expected case that has been returned
    assertEquals(new UniquePropertyReferenceNumber(expectedUprn), results.getUprn());
  }

  private void doGetCaseByIdNotFound(UUID caseId) throws CTPException {
    Mockito.when(caseServiceClient.getCaseById(eq(caseId), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)); // Not in RM
    Mockito.when(dataRepo.readCachedCaseById(eq(caseId))).thenReturn(Optional.empty());

    Fault fault = null;
    String message = null;
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    // Run the request
    try {
      target.getCaseById(caseId, requestParams);
    } catch (CTPException e) {
      fault = e.getFault();
      message = e.getMessage();
    }

    assertEquals(Fault.RESOURCE_NOT_FOUND, fault);
    assertTrue(message.contains("Case Id Not Found:"));
  }

  private void doGetCaseByIdGetsError(UUID caseId) throws CTPException {
    Mockito.when(caseServiceClient.getCaseById(eq(caseId), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)); // RM problems

    HttpStatus status = null;
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    // Run the request
    try {
      target.getCaseById(caseId, requestParams);
    } catch (ResponseStatusException e) {
      status = e.getStatus();
    }

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status);
  }

  private void setUpCaseFromCaseService(
      CaseContainerDTO caseFromCaseService, UUID caseId, String uprn) {
    caseFromCaseService.setId(caseId);
    caseFromCaseService.setUprn(uprn);
  }

  private void setUpCachedCase(CachedCase cachedCase, String caseId, String uprn) {
    cachedCase.setId(caseId);
    cachedCase.setUprn(uprn);
  }
}
