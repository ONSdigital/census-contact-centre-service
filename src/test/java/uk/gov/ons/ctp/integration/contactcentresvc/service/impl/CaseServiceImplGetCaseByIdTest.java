package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
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

  @Before
  public void setup() {
    // For case retrieval, mock out a whitelist of allowable case events
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_CREATED", "CASE_UPDATED");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    Mockito.when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
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

  @SneakyThrows
  private void doTestGetCaseByCaseId(CaseType caseType, boolean caseEvents, boolean cached) {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    CaseDTO expectedCaseResult;

    if (cached) {
      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
          .when(caseServiceClient)
          .getCaseById(eq(UUID_0), any());

      CachedCase caseFromRepository = FixtureHelper.loadPackageFixtures(CachedCase[].class).get(0);
      caseFromRepository.setCreatedDateTime(caseFromCaseService.getCreatedDateTime());
      Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0)))
          .thenReturn(Optional.of(caseFromRepository));

      CaseContainerDTO expectedCase = mapperFacade.map(caseFromRepository, CaseContainerDTO.class);
      expectedCaseResult = createExpectedCaseDTO(expectedCase, caseEvents);

    } else {
      Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any()))
          .thenReturn(caseFromCaseService);
      expectedCaseResult = createExpectedCaseDTO(caseFromCaseService, caseEvents);
    }

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);

    verifyCase(results, expectedCaseResult, caseEvents);
    assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), results.getCreatedDateTime().getTime());
  }

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }
}
