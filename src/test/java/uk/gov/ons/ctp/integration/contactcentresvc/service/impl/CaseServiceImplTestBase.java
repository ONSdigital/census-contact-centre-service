package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import ma.glasnost.orika.MapperFacade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSPostcodesBean;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CCSPostcodes;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public abstract class CaseServiceImplTestBase {
  @Spy AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;

  @Mock CaseServiceClientServiceImpl caseServiceClient;

  @Mock EqLaunchService eqLaunchService;

  @Mock EventPublisher eventPublisher;

  @Spy MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Mock CaseDataRepository dataRepo;

  @Mock AddressService addressSvc;

  @Mock CCSPostcodesBean ccsPostcodesBean;

  static final List<DeliveryChannel> ALL_DELIVERY_CHANNELS =
      List.of(DeliveryChannel.POST, DeliveryChannel.SMS);

  @InjectMocks CaseService target = new CaseServiceImpl();

  void verifyTimeInExpectedRange(long minAllowed, long maxAllowed, Date dateTime) {
    long actualInMillis = dateTime.getTime();
    assertTrue(actualInMillis + " not after " + minAllowed, actualInMillis >= minAllowed);
    assertTrue(actualInMillis + " not before " + maxAllowed, actualInMillis <= maxAllowed);
  }

  long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    return dateParser.parse(datetime).getTime();
  }

  <T extends EventPayload> T verifyEventSent(EventType expectedEventType, Class<T> payloadClazz) {
    ArgumentCaptor<T> payloadCaptor = ArgumentCaptor.forClass(payloadClazz);
    verify(eventPublisher)
        .sendEvent(
            eq(expectedEventType),
            eq(Source.CONTACT_CENTRE_API),
            eq(Channel.CC),
            payloadCaptor.capture());

    return payloadCaptor.getValue();
  }

  void verifyEventNotSent() {
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  void verifyEventNotSent(EventType type) {
    verify(eventPublisher, never()).sendEvent(eq(type), any(), any(), any());
  }

  void verifyNotWrittenCachedCase() throws Exception {
    verify(dataRepo, never()).writeCachedCase(any());
  }

  void verifyCase(CaseDTO results, CaseDTO expectedCaseResult, boolean caseEventsExpected)
      throws Exception {
    assertEquals(expectedCaseResult.getId(), results.getId());
    assertEquals(expectedCaseResult.getCaseRef(), results.getCaseRef());
    assertEquals(expectedCaseResult.getCaseType(), results.getCaseType());
    assertEquals(expectedCaseResult.getCeOrgName(), results.getCeOrgName());
    assertEquals(
        expectedCaseResult.getAllowedDeliveryChannels(), results.getAllowedDeliveryChannels());

    if (!caseEventsExpected) {
      assertTrue(results.getCaseEvents().isEmpty());
    }

    assertEquals(expectedCaseResult, results);
    verifyNotWrittenCachedCase();
    Mockito.verify(addressSvc, never()).uprnQuery(anyLong());
    verifyEventNotSent();
  }

  CaseDTO createExpectedCaseDTO(CaseContainerDTO caseFromCaseService, boolean caseEvents) {

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
            .caseEvents(Collections.emptyList())
            .build();
    if (caseEvents) {
      expectedCaseResult.setCaseEvents(filterEvents(caseFromCaseService));
    }
    return expectedCaseResult;
  }

  List<CaseEventDTO> filterEvents(CaseContainerDTO caseFromCaseService) {
    return caseFromCaseService.getCaseEvents().stream()
        .filter(e -> !e.getDescription().contains("Should be filtered out"))
        .map(
            e ->
                CaseEventDTO.builder()
                    .description(e.getDescription())
                    .category(e.getEventType())
                    .createdDateTime(e.getCreatedDateTime())
                    .build())
        .collect(toList());
  }

  UniquePropertyReferenceNumber createUprn(String uprn) {
    return uprn == null ? null : new UniquePropertyReferenceNumber(uprn);
  }

  CaseContainerDTO mockGetCaseById(String caseType, String addressLevel, String region) {
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType(caseType);
    caseFromCaseService.setAddressLevel(addressLevel);
    caseFromCaseService.setRegion(region);
    when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);
    return caseFromCaseService;
  }

  void mockCaseEventWhiteList() {
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    Set<String> whitelistedSet = Set.of("CASE_CREATED", "CASE_UPDATED");
    caseServiceSettings.setWhitelistedEventCategories(whitelistedSet);
    when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }

  void mockCcsPostcodes() throws IOException {
    CCSPostcodes ccsPostcodes = new CCSPostcodes();
    Set<String> ccsPostcodesSet = Set.of("GW12 AAA", "GW12 AAB", "HP22 4HU");
    ccsPostcodes.setCcsDefaultPostcodes(ccsPostcodesSet);
    ccsPostcodes.setCcsPostcodePath("/etc/config/ccs-postcodes");
  }

  void assertCaseQIDRestClientFailureCaught(Exception ex, boolean caught) {
    mockGetCaseById("CE", "U", "W");
    Mockito.doThrow(ex)
        .when(caseServiceClient)
        .getSingleUseQuestionnaireId(eq(UUID_0), anyBoolean(), any());
    UACRequestDTO requestsFromCCSvc =
        UACRequestDTO.builder().adLocationId(AN_AGENT_ID).individual(true).build();
    try {
      target.getUACForCaseId(UUID_0, requestsFromCCSvc);
      fail();
    } catch (CTPException badRequest) {
      if (caught) {
        assertEquals(Fault.BAD_REQUEST, badRequest.getFault());
      } else {
        fail();
      }
    } catch (ResponseStatusException other) {
      if (!caught) {
        assertSame(ex, other);
      } else {
        fail();
      }
    }
  }
}
