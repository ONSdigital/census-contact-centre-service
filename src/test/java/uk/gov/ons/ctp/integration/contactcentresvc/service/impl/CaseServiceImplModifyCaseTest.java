package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplModifyCaseTest extends CaseServiceImplTestBase {

  private ModifyCaseRequestDTO requestDTO;
  private CaseContainerDTO caseContainerDTO;

  @Before
  public void setup() throws Exception {
    requestDTO = FixtureHelper.loadClassFixtures(ModifyCaseRequestDTO[].class).get(0);
    caseContainerDTO = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
  }

  private void verifyRejectIncompatible(EstabType estabType, CaseType caseType) throws Exception {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertEquals("Mismatching caseType and estabType", e.getMessage());
  }

  @Test
  public void shouldRejectIncompatibleCaseTypeAndEstabType() throws Exception {
    verifyRejectIncompatible(EstabType.APPROVED_PREMISES, CaseType.HH);
    verifyRejectIncompatible(EstabType.FOREIGN_OFFICES, CaseType.CE);
  }

  private void verifyAcceptCompatible(EstabType estabType, CaseType caseType) throws Exception {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    target.modifyCase(requestDTO);
    // WRITEME check ok . at minimum returns a CaseDTO
  }

  private void verifyRmCaseCall(int times) {
    verify(caseServiceClient, times(times)).getCaseById(any(), eq(false));
  }

  private void mockRmHasCase() {
    when(caseServiceClient.getCaseById(eq(UUID_0), eq(false))).thenReturn(caseContainerDTO);
  }

  @Test
  public void shouldAcceptCompatibleCaseTypeAndEstabType() throws Exception {
    mockRmHasCase();
    verifyAcceptCompatible(EstabType.APPROVED_PREMISES, CaseType.CE);
    verifyAcceptCompatible(EstabType.FOREIGN_OFFICES, CaseType.HH);
    verifyRmCaseCall(2);
  }

  @Test
  public void shouldAcceptAnyRelevantCaseTypeWithEstabTypeOTHER() throws Exception {
    mockRmHasCase();
    verifyAcceptCompatible(EstabType.OTHER, CaseType.CE);
    verifyAcceptCompatible(EstabType.OTHER, CaseType.HH);
    verifyAcceptCompatible(EstabType.OTHER, CaseType.SPG);
    verifyRmCaseCall(3);
  }

  @Test
  public void shouldModifyAddress() throws Exception {
    mockRmHasCase();
    target.modifyCase(requestDTO);
    verifyRmCaseCall(1);
  }
}
