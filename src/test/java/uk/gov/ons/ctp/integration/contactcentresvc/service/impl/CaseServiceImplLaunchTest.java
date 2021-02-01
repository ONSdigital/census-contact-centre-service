package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
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
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;

/**
 * Unit Test {@link CaseService#getLaunchURLForCaseId(UUID, LaunchRequestDTO)
 * getLaunchURLForCaseId}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplLaunchTest extends CaseServiceImplTestBase {

  @Captor private ArgumentCaptor<UUID> individualCaseIdCaptor;
  @Captor private ArgumentCaptor<EqLaunchData> eqLaunchDataCaptor;
  @Mock private KeyStore keyStoreEncryption;

  @Before
  public void setup() {
    EqConfig eqConfig = new EqConfig();
    eqConfig.setProtocol("https");
    eqConfig.setHost("localhost");
    eqConfig.setPath("/en/start/launch-eq/?token=");
    eqConfig.setResponseIdSalt("CENSUS");
    appConfig.setEq(eqConfig);

    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
    Mockito.when(appConfig.getEq()).thenReturn(eqConfig);
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

  // Even though this should never happen, we will let an invalid region through (the EQ launch
  // token will default the region to english).
  // This may be useful at the Scottish border, where things might get a little fuzzy.
  @Test
  public void shouldLaunchCaseFromNonValidRegion() throws Exception {
    CaseContainerDTO caseDetails = mockGetCaseById("HH", "E", "S");
    doLaunchTest(false, caseDetails, FormType.H);
  }

  @Test
  public void shouldLaunchCcsCase() throws Exception {
    CaseContainerDTO caseDetails = mockGetCaseById("HH", "E", A_REGION.name());
    caseDetails.setSurveyType("CCS");
    doLaunchTest(false, caseDetails, FormType.H);
  }

  @Test
  public void shouldRejectCcsCaseForCE() throws Exception {
    CaseContainerDTO caseDetails = mockGetCaseById("CE", "E", A_REGION.name());
    caseDetails.setSurveyType("CCS");
    assertThatInvalidLaunchComboIsRejected(
        caseDetails,
        "Telephone capture feature is not available for CCS Communal establishment's. CCS CE's must submit their survey via CCS Paper Questionnaire",
        Fault.RESOURCE_NOT_FOUND);
  }

  @Test
  public void shouldLaunchWelshCaseWithAddressLevel_E_forCE() throws Exception {
    CaseContainerDTO caseDetails = mockGetCaseById("CE", "E", "W");
    doLaunchTest(false, caseDetails, FormType.C);
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
    target.getLaunchURLForCaseId(UUID_0, new LaunchRequestDTO());
  }

  @Test(expected = ResponseStatusException.class)
  public void testLaunch_caseServiceNotFoundException_noCachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.empty());
    target.getLaunchURLForCaseId(UUID_0, new LaunchRequestDTO());
  }

  @Test(expected = ResponseStatusException.class)
  public void testLaunch_caseServiceResponseStatusException() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    target.getLaunchURLForCaseId(UUID_0, new LaunchRequestDTO());
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(
      CaseContainerDTO dto, String expectedMsg, Fault expectedFault) {
    try {
      doLaunchTest(false, dto, FormType.C);
      fail();
    } catch (CTPException e) {
      assertEquals(expectedFault, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains(expectedMsg));
    }
  }

  @SneakyThrows
  private void assertThatCeManagerFormFromUnitRegionIsRejected(CaseContainerDTO dto) {
    assertThatInvalidLaunchComboIsRejected(
        dto,
        "A CE Manager form can only be launched against an establishment address not a UNIT.",
        Fault.BAD_REQUEST);
  }

  @Test
  public void testLaunch_caseServiceQidRequestResponseStatusExceptionBadRequestCause() {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Bad Request",
            new HttpClientErrorException(HttpStatus.BAD_REQUEST)),
        true);
  }

  @Test
  public void testLaunch_caseServiceQidRequestResponseStatusExceptionOtherCause() {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Other",
            new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT)),
        false);
  }

  @Test
  public void testLaunch_caseServiceQidRequestResponseStatusExceptionNoCause() {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal processing error"),
        false);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionE() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "E");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionW() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "W");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionN() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "N");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionS() {
    CaseContainerDTO dto = mockGetCaseById("CE", "U", "S");
    assertThatCeManagerFormFromUnitRegionIsRejected(dto);
  }

  @Test
  public void shouldRejectCeManagerFormFromEstabRegionN() {
    CaseContainerDTO dto = mockGetCaseById("CE", "E", "N");
    assertThatInvalidLaunchComboIsRejected(
        dto,
        "All Northern Ireland calls from CE Managers are to be escalated to the NI management team.",
        Fault.BAD_REQUEST);
  }

  private void verifyEqLaunchJwe(
      String questionnaireId, boolean individual, String caseType, FormType formType)
      throws Exception {
    Mockito.verify(eqLaunchService).getEqLaunchJwe(eqLaunchDataCaptor.capture());
    EqLaunchData eqLaunchData = eqLaunchDataCaptor.getValue();

    assertEquals(Language.ENGLISH, eqLaunchData.getLanguage());
    assertEquals(uk.gov.ons.ctp.common.domain.Source.CONTACT_CENTRE_API, eqLaunchData.getSource());
    assertEquals(uk.gov.ons.ctp.common.domain.Channel.CC, eqLaunchData.getChannel());
    assertEquals(AN_AGENT_ID, eqLaunchData.getUserId());
    assertEquals(questionnaireId, eqLaunchData.getQuestionnaireId());
    assertEquals(formType.name(), eqLaunchData.getFormType());
    assertNull(eqLaunchData.getAccountServiceLogoutUrl());
    assertNull(eqLaunchData.getAccountServiceUrl());
    if (caseType.equals("HH") && individual) {
      // Should have used a new caseId, ie, not the uuid that we started with
      assertNotEquals(UUID_0, eqLaunchData.getCaseContainer().getId());
    } else {
      assertEquals(UUID_0, eqLaunchData.getCaseContainer().getId());
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

    // Mock out building of launch payload
    Mockito.when(eqLaunchService.getEqLaunchJwe(any(EqLaunchData.class)))
        .thenReturn("simulated-encrypted-payload");

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
