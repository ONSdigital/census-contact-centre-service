package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
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
                  e -> {
                    CaseEventDTO expectedEvent =
                        CaseEventDTO.builder()
                            .description(e.getDescription())
                            .category(e.getEventType())
                            .createdDateTime(e.getCreatedDateTime())
                            .build();
                    return expectedEvent;
                  })
              .collect(Collectors.toList());
      expectedCaseResult.setCaseEvents(expectedCaseEvents);
    }
    return expectedCaseResult;
  }

  @SneakyThrows
  private void verifyCase(CaseDTO results, CaseDTO expectedCaseResult, boolean caseEventsExpected) {
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
    Mockito.verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  @SneakyThrows
  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
  }
}
