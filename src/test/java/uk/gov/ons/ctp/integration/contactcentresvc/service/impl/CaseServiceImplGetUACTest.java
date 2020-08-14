package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.AN_AGENT_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_QUESTIONNAIRE_ID;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_REGION;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_UAC;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.NI_LAUNCH_ERR_MSG;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UNIT_LAUNCH_ERR_MSG;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.SingleUseQuestionnaireIdDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#getUACForCaseId(UUID, UACRequestDTO) getUACForCaseId}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplGetUACTest extends CaseServiceImplTestBase {

  @Test
  public void testGetUACCECase() throws Exception {
    doGetUACTest("CE", false);
  }

  @Test
  public void testGetUACCECaseForIndividual() throws Exception {
    doGetUACTest("CE", true);
  }

  @Test
  public void testGetUACHHCase() throws Exception {
    doGetUACTest("HH", false);
  }

  @Test
  public void testGetUACHHCaseForIndividual() throws Exception {
    doGetUACTest("HH", true);
  }

  @Test
  public void testGetUACSPGCase() throws Exception {
    doGetUACTest("SPG", false);
  }

  @Test
  public void testGetUACSPGCaseForIndividual() throws Exception {
    doGetUACTest("SPG", true);
  }

  @Test
  public void testGetUACHICase() {
    try {
      doGetUACTest("HI", false);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage(), e.getMessage().contains("must be SPG, CE or HH"));
    }
  }

  @Test(expected = CTPException.class)
  public void testGetUAC_caseServiceCaseNotFoundException_cachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.of(new CachedCase()));
    target.getUACForCaseId(UUID_0, new UACRequestDTO());
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetUAC_caseServiceCaseNotFoundException_noCachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.empty());
    target.getUACForCaseId(UUID_0, new UACRequestDTO());
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetUAC_caseServiceCaseRequestResponseStatusException() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    target.getUACForCaseId(UUID_0, new UACRequestDTO());
  }

  @Test
  public void testGetUAC_caseServiceQidRequestResponseStatusExceptionBadRequestCause() {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Bad Request",
            new HttpClientErrorException(HttpStatus.BAD_REQUEST)),
        true);
  }

  @Test
  public void testGetUAC_caseServiceQidRequestResponseStatusExceptionOtherCause() {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Other",
            new HttpClientErrorException(HttpStatus.I_AM_A_TEAPOT)),
        false);
  }

  @Test
  public void testGetUAC_caseServiceQidRequestResponseStatusExceptionNoCause() {
    assertCaseQIDRestClientFailureCaught(
        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal processing error"),
        false);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionE() {
    mockGetCaseById("CE", "U", "E");
    assertThatInvalidLaunchComboIsRejected(UNIT_LAUNCH_ERR_MSG);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionW() {
    mockGetCaseById("CE", "U", "W");
    assertThatInvalidLaunchComboIsRejected(UNIT_LAUNCH_ERR_MSG);
  }

  @Test
  public void shouldRejectCeManagerFormFromUnitRegionN() {
    mockGetCaseById("CE", "U", "N");
    assertThatInvalidLaunchComboIsRejected(UNIT_LAUNCH_ERR_MSG);
  }

  @Test
  public void shouldRejectCeManagerFormFromEstabRegionN() {
    mockGetCaseById("CE", "E", "N");
    assertThatInvalidLaunchComboIsRejected(NI_LAUNCH_ERR_MSG);
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(String expectedMsg) {
    try {
      doGetUACTest(false, FormType.C);
      fail();
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains(expectedMsg));
    }
  }

  private void doGetUACTest(String caseType, boolean individual) throws Exception {
    mockGetCaseById(caseType, "U", A_REGION.name());
    doGetUACTest(individual, FormType.H);
  }

  private void doGetUACTest(boolean individual, FormType formType) throws Exception {

    // Fake RM response for creating questionnaire ID
    SingleUseQuestionnaireIdDTO newQuestionnaireIdDto = new SingleUseQuestionnaireIdDTO();
    newQuestionnaireIdDto.setQuestionnaireId(A_QUESTIONNAIRE_ID);
    newQuestionnaireIdDto.setUac(A_UAC);
    newQuestionnaireIdDto.setFormType(formType.name());
    Mockito.when(caseServiceClient.getSingleUseQuestionnaireId(eq(UUID_0), eq(individual), any()))
        .thenReturn(newQuestionnaireIdDto);

    UACRequestDTO requestsFromCCSvc =
        UACRequestDTO.builder().adLocationId(AN_AGENT_ID).individual(individual).build();

    long timeBeforeInvocation = System.currentTimeMillis();
    UACResponseDTO uac = target.getUACForCaseId(UUID_0, requestsFromCCSvc);
    long timeAfterInvocation = System.currentTimeMillis();

    assertEquals(A_UAC, uac.getUac());
    assertEquals(A_QUESTIONNAIRE_ID, uac.getId());
    verifyTimeInExpectedRange(timeBeforeInvocation, timeAfterInvocation, uac.getDateTime());
  }
}
