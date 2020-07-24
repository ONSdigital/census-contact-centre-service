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
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
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
  public void testGetUACHICase() throws Exception {
    try {
      doGetUACTest("HI", false);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage(), e.getMessage().contains("must be SPG, CE or HH"));
    }
  }

  @Test(expected = CTPException.class)
  public void testGetUAC_caseServiceNotFoundException_cachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.of(new CachedCase()));
    target.getUACForCaseId(UUID_0, new UACRequestDTO());
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetUAC_caseServiceNotFoundException_noCachedCase() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    Mockito.when(dataRepo.readCachedCaseById(UUID_0)).thenReturn(Optional.empty());
    target.getUACForCaseId(UUID_0, new UACRequestDTO());
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetUAC_caseServiceResponseStatusException() throws Exception {
    Mockito.doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT))
        .when(caseServiceClient)
        .getCaseById(UUID_0, false);
    target.getUACForCaseId(UUID_0, new UACRequestDTO());
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
  public void shouldRejectCeManagerFormFromEstabRegionN() {
    CaseContainerDTO dto = mockGetCaseById("CE", "E", "N");
    assertThatInvalidLaunchComboIsRejected(
        dto,
        "All Northern Ireland calls from CE Managers are to be escalated to the NI management team.");
  }

  @SneakyThrows
  private void assertThatInvalidLaunchComboIsRejected(CaseContainerDTO dto, String expectedMsg) {
    try {
      doGetUACTest(false, dto, FormType.C);
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

  private void doGetUACTest(String caseType, boolean individual) throws Exception {
    CaseContainerDTO caseFromCaseService = mockGetCaseById(caseType, "U", A_REGION.name());
    doGetUACTest(individual, caseFromCaseService, FormType.H);
  }

  private void doGetUACTest(
      boolean individual, CaseContainerDTO caseFromCaseService, FormType formType)
      throws Exception {

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
