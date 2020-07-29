package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Set;
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
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getCaseByCaseReference(long, CaseQueryRequestDTO)
 * getCaseByCaseReference}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplGetCaseByCaseRefTest extends CaseServiceImplTestBase {
  private static final boolean CASE_EVENTS_TRUE = true;
  private static final boolean CASE_EVENTS_FALSE = false;

  private static final long VALID_CASE_REF = 882_345_440L;

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
  public void testGetHouseholdCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.HH, CASE_EVENTS_TRUE);
  }

  @Test
  public void testGetHouseholdCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.HH, CASE_EVENTS_FALSE);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.CE, CASE_EVENTS_TRUE);
  }

  @Test
  public void testGetCommunalCaseByCaseRef_withNoCaseDetails() throws Exception {
    doTestGetCaseByCaseRef(CaseType.CE, CASE_EVENTS_FALSE);
  }

  @Test
  public void testGetCaseByCaseRef_caseSPG() throws Exception {
    doTestGetCaseByCaseRef(CaseType.SPG, CASE_EVENTS_FALSE);
  }

  @Test
  public void testGetCaseByCaseRef_householdIndividualCase() throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType("HI"); // Household Individual case
    Mockito.when(caseServiceClient.getCaseByCaseRef(eq(VALID_CASE_REF), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    try {
      target.getCaseByCaseReference(VALID_CASE_REF, new CaseQueryRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is not suitable", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  @Test
  public void shouldGetSecureEstablishmentByCaseReference() throws Exception {
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(1);
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    CaseDTO results = target.getCaseByCaseReference(VALID_CASE_REF, new CaseQueryRequestDTO(false));
    assertTrue(results.isSecureEstablishment());
    assertEquals(new UniquePropertyReferenceNumber(AN_ESTAB_UPRN), results.getEstabUprn());
  }

  private void rejectNonLuhn(long caseRef) {
    CaseQueryRequestDTO dto = new CaseQueryRequestDTO(false);
    CTPException e =
        assertThrows(CTPException.class, () -> target.getCaseByCaseReference(caseRef, dto));
    assertEquals("Invalid Case Reference", e.getMessage());
  }

  @Test
  public void shouldRejectGetByNonLuhnCaseReference() {
    rejectNonLuhn(123);
    rejectNonLuhn(1231);
    rejectNonLuhn(100000000);
    rejectNonLuhn(999999999);
  }

  private void acceptLuhn(long caseRef) throws Exception {
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any()))
        .thenReturn(casesFromCaseService().get(0));
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(false);
    target.getCaseByCaseReference(caseRef, requestParams);
  }

  @Test
  public void shouldAcceptLuhnCompliantGetByCaseReference() throws Exception {
    acceptLuhn(1230);
    acceptLuhn(VALID_CASE_REF);
    acceptLuhn(100000009);
    acceptLuhn(999999998);
  }

  private void doTestGetCaseByCaseRef(CaseType caseType, boolean caseEvents) throws Exception {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    Mockito.when(caseServiceClient.getCaseByCaseRef(any(), any())).thenReturn(caseFromCaseService);

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseByCaseReference(VALID_CASE_REF, requestParams);
    CaseDTO expectedCaseResult = createExpectedCaseDTO(caseFromCaseService, caseEvents);
    verifyCase(results, expectedCaseResult, caseEvents);
  }

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
  }
}
