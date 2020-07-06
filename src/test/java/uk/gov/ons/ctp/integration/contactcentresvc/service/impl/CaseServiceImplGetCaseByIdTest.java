package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#getCaseById(UUID, CaseQueryRequestDTO) getCaseById}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplGetCaseByIdTest extends CaseServiceImplTestBase {
  private static final boolean HAND_DELIVERY_TRUE = true;
  private static final boolean HAND_DELIVERY_FALSE = false;

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
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_TRUE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetHouseholdCaseByCaseId_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseHHhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.HH,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.CE,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_TRUE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCommunalCaseByCaseId_withNoCaseDetails() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.CE,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryTrue() {
    List<DeliveryChannel> expectedDeliveryChannels = Arrays.asList(DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG,
        HAND_DELIVERY_TRUE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryFalse() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_FALSE,
        NO_CACHED_CASE,
        expectedDeliveryChannels);
  }

  @Test
  public void testGetCaseByCaseId_caseSPGhandDeliveryFalse_fromCache() {
    List<DeliveryChannel> expectedDeliveryChannels =
        Arrays.asList(DeliveryChannel.POST, DeliveryChannel.SMS);
    doTestGetCaseByCaseId(
        CaseType.SPG,
        HAND_DELIVERY_FALSE,
        CASE_EVENTS_FALSE,
        USE_CACHED_CASE,
        expectedDeliveryChannels);
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
  private void doTestGetCaseByCaseId(
      CaseType caseType,
      boolean handDelivery,
      boolean caseEvents,
      boolean cached,
      List<DeliveryChannel> expectedAllowedDeliveryChannels) {
    // Build results to be returned from search
    CaseContainerDTO caseFromCaseService = casesFromCaseService().get(0);
    caseFromCaseService.setCaseType(caseType.name());
    caseFromCaseService.setHandDelivery(handDelivery);
    CaseDTO expectedCaseResult;

    if (cached) {
      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
          .when(caseServiceClient)
          .getCaseById(eq(UUID_0), any());

      CachedCase caseFromRepository = caseFromRepository();
      caseFromRepository.setCreatedDateTime(caseFromCaseService.getCreatedDateTime());
      Mockito.when(dataRepo.readCachedCaseById(eq(UUID_0)))
          .thenReturn(Optional.of(caseFromRepository));

      CaseContainerDTO expectedCase = mapperFacade.map(caseFromRepository, CaseContainerDTO.class);
      expectedCaseResult =
          createExpectedCaseDTO(expectedCase, caseEvents, expectedAllowedDeliveryChannels);

    } else {
      Mockito.when(caseServiceClient.getCaseById(eq(UUID_0), any()))
          .thenReturn(caseFromCaseService);
      expectedCaseResult =
          createExpectedCaseDTO(caseFromCaseService, caseEvents, expectedAllowedDeliveryChannels);
    }

    // Run the request
    CaseQueryRequestDTO requestParams = new CaseQueryRequestDTO(caseEvents);
    CaseDTO results = target.getCaseById(UUID_0, requestParams);

    verifyCase(results, expectedCaseResult, caseEvents);
    assertEquals(asMillis("2019-05-14T16:11:41.343+01:00"), results.getCreatedDateTime().getTime());
  }

  private UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  private CaseDTO createExpectedCaseDTO(
      CaseContainerDTO caseFromCaseService,
      boolean caseEvents,
      List<DeliveryChannel> expectedAllowedDeliveryChannels) {

    CaseDTO expectedCaseResult =
        CaseDTO.builder()
            .id(caseFromCaseService.getId())
            .caseRef(caseFromCaseService.getCaseRef())
            .caseType(caseFromCaseService.getCaseType())
            .estabType(EstabType.forCode(caseFromCaseService.getEstabType()))
            .estabDescription(caseFromCaseService.getEstabType())
            .allowedDeliveryChannels(expectedAllowedDeliveryChannels)
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
            .handDelivery(caseFromCaseService.isHandDelivery())
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
    assertEquals(expectedCaseResult.isHandDelivery(), results.isHandDelivery());

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

  private List<CaseContainerDTO> casesFromCaseService() {
    return FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
  }

  private CachedCase caseFromRepository() {
    return FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);
  }
}
