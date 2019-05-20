package uk.gov.ons.ctp.integration.contactcentresvc.client.caseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

  private UUID uuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);

    // Mock the case service settings
    CaseServiceSettings caseServiceSettings = new CaseServiceSettings();
    caseServiceSettings.setCaseByIdQueryPath("/cases/{uuid}");
    Mockito.when(appConfig.getCaseServiceSettings()).thenReturn(caseServiceSettings);
  }

  @Test
  public void testGetCaseById_withCaseEvents() throws Exception {
    // Build results to be returned by the case service
    CaseContainerDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{uuid}"), eq(CaseContainerDTO.class), any(), any(), any()))
        .thenReturn(resultsFromCaseService);

    // Run the request
    boolean requireCaseEvents = true;
    CaseContainerDTO results = caseServiceClientService.getCaseById(uuid, requireCaseEvents);

    // Sanity check the response
    assertEquals(uuid, results.getId());
    assertNotNull(results.getCaseEvents());
    verifyCaseEventsQueryParam(requireCaseEvents);
  }

  @Test
  public void testGetCaseById_withNoCaseEvents() throws Exception {
    // Build results to be returned by the case service
    CaseContainerDTO resultsFromCaseService =
        FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Mockito.when(
            restClient.getResource(
                eq("/cases/{uuid}"), eq(CaseContainerDTO.class), any(), any(), any()))
        .thenReturn(resultsFromCaseService);

    // Run the request
    boolean requireCaseEvents = false;
    CaseContainerDTO results = caseServiceClientService.getCaseById(uuid, requireCaseEvents);

    // Sanity check the response
    assertEquals(uuid, results.getId());
    assertNotNull(results.getCaseEvents()); // Not removed at this level
    verifyCaseEventsQueryParam(requireCaseEvents);
  }

  private void verifyCaseEventsQueryParam(boolean expectedCaseEventsValue) {
    Mockito.verify(restClient).getResource(any(), any(), any(), queryParamsCaptor.capture(), any());
    MultiValueMap<String, String> queryParams = queryParamsCaptor.getValue();
    assertEquals("[" + expectedCaseEventsValue + "]", queryParams.get("caseEvents").toString());
    assertEquals(1, queryParams.keySet().size());
  }
}
