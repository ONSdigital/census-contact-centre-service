package uk.gov.ons.ctp.integration.contactcentresvc.client.caseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.MultiValueMap;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CaseServiceSettings;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;

/**
 * This class contains unit tests for the CaseServiceClientServiceImpl class. It mocks out the Rest
 * calls and returns dummy responses to represent what would be returned by the case service.
 */
public class CaseServiceClientServiceImplTest {
  @Mock AppConfig appConfig = new AppConfig();

  @Mock RestClient restClient;

  @InjectMocks
  CaseServiceClientServiceImpl caseServiceClientService = new CaseServiceClientServiceImpl();

  @Captor ArgumentCaptor<MultiValueMap<String, String>> queryParamsCaptor;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    // Mock the case service settings
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    caseServiceSettings.setCaseByIdQueryPath("/cases/{uuid}");
    caseServiceSettings.setCaseByUprnQueryPath("/cases/uprn/{uprn}");
    caseServiceSettings.setCaseByCaseReferenceQueryPath("/cases/ref/{reference}");
    Mockito.when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }

  @Test
  public void testGetCaseById_withCaseEvents() throws Exception {
    doTestGetCaseById(true);
  }

  @Test
  public void testGetCaseById_withNoCaseEvents() throws Exception {
    doTestGetCaseById(false);
  }

  private void doTestGetCaseById(boolean requireCaseEvents) throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

    // Build results to be returned by the case service
    CaseContainerDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{uuid}"),
                eq(CaseContainerDTO.class),
                any(),
                any(),
                eq(testUuid.toString())))
        .thenReturn(resultsFromCaseService);

    // Run the request
    CaseContainerDTO results = caseServiceClientService.getCaseById(testUuid, requireCaseEvents);

    // Sanity check the response
    assertEquals(testUuid, results.getId());
    assertNotNull(
        results.getCaseEvents()); // Response will have events as not removed at this level
    verifyRequestUsedCaseEventsQueryParam(requireCaseEvents);
  }

  @Test
  public void testGetCaseByUprn_withCaseEvents() throws Exception {
    doTestGetCaseByUprn(true);
  }

  @Test
  public void testGetCaseByUprn_withNoCaseEvents() throws Exception {
    doTestGetCaseByUprn(false);
  }

  private void doTestGetCaseByUprn(boolean requireCaseEvents) throws Exception {
    String caseId1 = "b7565b5e-1396-4965-91a2-918c0d3642ed";
    String caseId2 = "b7565b5e-2222-2222-2222-918c0d3642ed";
    UniquePropertyReferenceNumber uprn = new UniquePropertyReferenceNumber(1235532324343434L);

    // Build results to be returned by the case service
    List<CaseContainerDTO> caseData = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class);
    Mockito.when(
            restClient.getResources(
                eq("/cases/uprn/{uprn}"),
                eq(CaseContainerDTO[].class),
                any(),
                any(),
                eq(Long.toString(uprn.getValue()))))
        .thenReturn(caseData);

    // Run the request
    List<CaseContainerDTO> results =
        caseServiceClientService.getCaseByUprn(uprn.getValue(), requireCaseEvents);

    // Sanity check the response
    assertEquals(UUID.fromString(caseId1), results.get(0).getId());
    assertEquals(Long.toString(uprn.getValue()), results.get(0).getUprn());
    assertNotNull(results.get(0).getCaseEvents()); // Events not removed yet

    assertEquals(UUID.fromString(caseId2), results.get(1).getId());
    assertEquals(Long.toString(uprn.getValue()), results.get(1).getUprn());
    assertNotNull(results.get(1).getCaseEvents()); // Events not removed yet

    // Make sure the caseEvents arg was passed through correctly
    Mockito.verify(restClient)
        .getResources(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[" + requireCaseEvents + "]", queryParams.get("caseEvents").toString());
    assertEquals(1, queryParams.keySet().size());
  }

  @Test
  public void testGetCaseByCaseRef_withCaseEvents() throws Exception {
    doTestGetCaseByCaseRef(true);
  }

  @Test
  public void testGetCaseByCaseRef_withNoCaseEvents() throws Exception {
    doTestGetCaseByCaseRef(false);
  }

  private void doTestGetCaseByCaseRef(boolean requireCaseEvents) throws Exception {
    UUID testUuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
    Long testCaseRef = 52224L;

    // Build results to be returned by the case service
    CaseContainerDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/ref/{reference}"),
                eq(CaseContainerDTO.class),
                any(),
                any(),
                eq(testCaseRef)))
        .thenReturn(resultsFromCaseService);

    // Run the request
    CaseContainerDTO results =
        caseServiceClientService.getCaseByCaseRef(testCaseRef, requireCaseEvents);

    // Sanity check the response
    assertEquals(Long.toString(testCaseRef), results.getCaseRef());
    assertEquals(testUuid, results.getId());
    assertNotNull(
        results.getCaseEvents()); // Response will have events as not removed at this level
    verifyRequestUsedCaseEventsQueryParam(requireCaseEvents);
  }

  private void verifyRequestUsedCaseEventsQueryParam(boolean expectedCaseEventsValue) {
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[" + expectedCaseEventsValue + "]", queryParams.get("caseEvents").toString());
    assertEquals(1, queryParams.keySet().size());
  }
}
