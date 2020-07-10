package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_QUESTIONNAIRE_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.config.EqConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Unit Test {@link CaseService#getLaunchURLForCaseId(UUID, LaunchRequestDTO)
 * getLaunchURLForCaseId}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplLaunchTest extends CaseServiceImplTestBase {

  @Captor private ArgumentCaptor<UUID> individualCaseIdCaptor;
  @Captor private ArgumentCaptor<CaseContainerDTO> caseCaptor;

  @Before
  public void setup() {
    EqConfig eqConfig = new EqConfig();
    eqConfig.setProtocol("https");
    eqConfig.setHost("localhost");
    eqConfig.setPath("/en/start/launch-eq/?token=");
    appConfig.setEq(eqConfig);

    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
  }

  @Test
  public void testLaunchCECase() throws Exception {
    doLaunchTest("CE", false);
  }

  @Test
  public void testLaunchCECaseForIndividual() throws Exception {
    doLaunchTest("CE", true);
  }

  @Test
  public void testLaunchHHCase() throws Exception {
    doLaunchTest("HH", false);
  }

  @Test
  public void testLaunchSPGCase() throws Exception {
    doLaunchTest("SPG", false);
  }

  @Test
  public void testLaunchSPGCaseForIndividual() throws Exception {
    doLaunchTest("SPG", true);
  }

  @Test
  public void testLaunchHHCaseForIndividual() throws Exception {
    doLaunchTest("HH", true);
  }

  @Test
  public void testLaunchHICase() {
    try {
      doLaunchTest("HI", false);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage(), e.getMessage().contains("must be SPG, CE or HH"));
    }
  }

  @Test(expected = CTPException.class)
  public void testLaunch_caseServiceNotFoundException_cachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.of(new CachedCase()));
    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);
    launchRequestDTO.setIndividual(false);
    target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
  }

  @Test(expected = ResponseStatusException.class)
  public void testLaunch_caseServiceNotFoundException_noCachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.empty());
    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);
    launchRequestDTO.setIndividual(true);
    target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
  }

  @Test(expected = ResponseStatusException.class)
  public void testLaunch_caseServiceResponseStatusException() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);
    launchRequestDTO.setIndividual(true);
    target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(CaseContainerDTO dto, String expectedMsg) {
    try {
      doLaunchTest(false, dto, FormType.C);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains(expectedMsg));
    }
  }

  @SneakyThrows
  private void assertThatCeManagerFormFromUnitRegionIsRejected(CaseContainerDTO dto) {
    assertThatInvalidLaunchComboIsRejected(
        dto, "A CE Manager form can only be launched against an establishment address not a UNIT.");
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionEast() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "E");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionWest() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "W");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionNorth() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "N");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectNorthernIslandCallsFromCeManagers() {
    CaseContainerDTO dto = mockGetCaseById("CE", "E", "N");
    assertThatInvalidLaunchComboIsRejected(
        dto,
        "All Northern Ireland calls from CE Managers are to be escalated to the NI management team.");
  }

  private void mockEqLaunchJwe() throws Exception {
    // Mock out building of launch payload
    Mockito.when(
            eqLaunchService.getEqLaunchJwe(
                eq(Language.ENGLISH),
                eq(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API),
                eq(uk.gov.ons.ctp.common.domain.Channel.CC),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                isNull())) // keystore
        .thenReturn("simulated-encrypted-payload");
  }

  private void verifyEqLaunchJwe(
      String questionnaireId, boolean individual, String caseType, FormType formType)
      throws Exception {
    Mockito.verify(eqLaunchService)
        .getEqLaunchJwe(
            eq(Language.ENGLISH),
            eq(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API),
            eq(uk.gov.ons.ctp.common.domain.Channel.CC),
            caseCaptor.capture(),
            eq(AN_AGENT_ID), // agent
            eq(questionnaireId),
            eq(formType.name()),
            isNull(), // accountServiceUrl
            isNull(),
            any()); // keystore

    CaseContainerDTO capturedCase = caseCaptor.getValue();
    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, capturedCase.getId());
    } else {
      assertEquals(UUID_0, capturedCase.getId());
    }
  }

  private void verifySurveyLaunchedEventPublished(
      String caseType, boolean individual, UUID caseId, String questionnaireId) {
    SurveyLaunchedResponse payloadSent =
        verifyEventSent(EventType.SURVEY_LAUNCHED, SurveyLaunchedResponse.class);
    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, payloadSent.getCaseId());
    } else {
      assertEquals(caseId, payloadSent.getCaseId());
    }
    assertEquals(questionnaireId, payloadSent.getQuestionnaireId());
    assertEquals(AN_AGENT_ID, payloadSent.getAgentId());
  }

  private void verifyCorrectIndividualCaseId(String caseType, boolean individual) {
    // Verify call to RM to get qid is using the correct individual case id
    Mockito.verify(caseServiceClient)
        .getSingleUseQuestionnaireId(any(), eq(individual), individualCaseIdCaptor.capture());
    if (caseType.equals("HH") && individual) {
      assertNotEquals(UUID_0, individualCaseIdCaptor.getValue()); // expecting newly allocated uuid
    } else {
      assertNull(individualCaseIdCaptor.getValue());
    }
  }

  private CaseContainerDTO mockGetCaseById(String caseType, String addressLevel, String region) {
    CaseContainerDTO caseFromCaseService =
        FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(0);
    caseFromCaseService.setCaseType(caseType);
    caseFromCaseService.setAddressLevel(addressLevel);
    caseFromCaseService.setRegion(region);
    when(caseServiceClient.getCaseById(eq(UUID_0), any())).thenReturn(caseFromCaseService);
    return caseFromCaseService;
  }

  private void doLaunchTest(String caseType, boolean individual) throws Exception {
    CaseContainerDTO caseFromCaseService = mockGetCaseById(caseType, "U", A_REGION.name());
    doLaunchTest(individual, caseFromCaseService, FormType.H);
  }

  private void doLaunchTest(
      boolean individual, CaseContainerDTO caseFromCaseService, FormType formType)
      throws Exception {
    String caseType = caseFromCaseService.getCaseType();

    // Fake RM response for creating questionnaire ID
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto = new SingleUseQuestionnaireIdDTO();
    newQuestionnaireIdDto.setQuestionnaireId(A_QUESTIONNAIRE_ID);
    newQuestionnaireIdDto.setFormType(formType.name());
    Mockito.when(caseServiceClient.getSingleUseQuestionnaireId(eq(UUID_0), eq(individual), any()))
        .thenReturn(newQuestionnaireIdDto);

    mockEqLaunchJwe();

    //    LaunchRequestDTO launchRequestDTO = CaseServiceFixture.createLaunchRequestDTO(individual);
    List<LaunchRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(LaunchRequestDTO[].class);
    LaunchRequestDTO launchRequestDTO = requestsFromCCSvc.get(0);
    launchRequestDTO.setIndividual(individual);

    // Invoke method under test, and check returned url
    String launchUrl = target.getLaunchURLForCaseId(UUID_0, launchRequestDTO);
    assertEquals(
        appConfig.getEq().getProtocol()
            + "://"
            + appConfig.getEq().getHost()
            + appConfig.getEq().getPath()
            + "simulated-encrypted-payload",
        launchUrl);

    verifyCorrectIndividualCaseId(caseType, individual);
    verifyEqLaunchJwe(A_QUESTIONNAIRE_ID, individual, caseType, formType);
    verifySurveyLaunchedEventPublished(caseType, individual, UUID_0, A_QUESTIONNAIRE_ID);
  }
}
