package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseDetailsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * This class tests the CaseServiceImpl layer. It mocks out the layer below
 * (CaseServiceClientServiceImpl), which would deal with actually sending a HTTP request to the case
 * service.
 */
public class CaseServiceImplTest {

  @Mock CaseServiceClientServiceImpl CaseServiceClientService = new CaseServiceClientServiceImpl();

  @InjectMocks CaseService caseService = new CaseServiceImpl();

  private UUID uuid = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetCaseByCaseId_withCaseDetails() throws Exception {
    // Build results to be returned from search
    CaseDetailsDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseDetailsDTO[].class).get(0);
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    boolean caseEvents = true;
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = caseService.getCaseById(uuid, requestParams);

    verifyCase(results, caseEvents);
  }

  @Test
  public void testGetCaseByCaseId_withNoCaseDetails() throws Exception {
    // Build results to be returned from search
    CaseDetailsDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseDetailsDTO[].class).get(0);
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request, with caseEvents turned off
    boolean caseEvents = false;
    CaseRequestDTO requestParams = new CaseRequestDTO(caseEvents);
    CaseDTO results = caseService.getCaseById(uuid, requestParams);

    verifyCase(results, caseEvents);
  }

  @Test
  public void testGetCaseByCaseId_nonHouseholdCase() throws Exception {
    // Build results to be returned from search
    CaseDetailsDTO caseFromCaseService =
        FixtureHelper.loadClassFixtures(CaseDetailsDTO[].class).get(0);
    caseFromCaseService.setSampleUnitType("X"); // Not household case
    Mockito.when(CaseServiceClientService.getCaseById(any(), any()))
        .thenReturn(caseFromCaseService);

    // Run the request
    try {
      caseService.getCaseById(uuid, new CaseRequestDTO(true));
      fail();
    } catch (ResponseStatusException e) {
      assertEquals("Case is a non-household case", e.getReason());
      assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
  }

  private void verifyCase(CaseDTO results, boolean caseEventsExpected) {
    assertEquals(uuid, results.getId());
    assertEquals("1000000000000001", results.getCaseRef());
    assertEquals("H", results.getCaseType());
    assertEquals("2019-05-14T16:11:41.561", formatDate(results.getCreatedDateTime()));

    assertEquals(1, results.getResponses().size());
    CaseResponseDTO response = results.getResponses().get(0);
    assertEquals("2019-05-14T16:11:41.558+01", response.getDateTime());
    assertEquals("ONLINE", response.getInboundChannel());

    if (caseEventsExpected) {
      assertEquals(2, results.getCaseEvents().size());
      CaseEventDTO event = results.getCaseEvents().get(0);
      assertEquals("Initial creation of case", event.getDescription());
      assertEquals("CASE_CREATED", event.getCategory());
      assertEquals("2019-05-14T16:11:41.561", formatDate(event.getCreatedDateTime()));
      event = results.getCaseEvents().get(1);
      assertEquals("Create Household Visit", event.getDescription());
      assertEquals("ACTION_CREATED", event.getCategory());
      assertEquals("2019-05-15T16:02:12.835", formatDate(event.getCreatedDateTime()));
    } else {
      assertNull(results.getCaseEvents());
    }
  }

  private String formatDate(LocalDateTime createdDateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    return createdDateTime.format(formatter);
  }
}
