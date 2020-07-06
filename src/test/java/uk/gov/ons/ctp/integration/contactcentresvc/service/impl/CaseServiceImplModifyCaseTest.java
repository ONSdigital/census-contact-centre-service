package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplModifyCaseTest extends CaseServiceImplTestBase {

  private ModifyCaseRequestDTO requestDTO;
  private CaseContainerDTO caseContainerDTO;
  private CachedCase cachedCase;

  @Before
  public void setup() throws Exception {
    requestDTO = FixtureHelper.loadClassFixtures(ModifyCaseRequestDTO[].class).get(0);
    caseContainerDTO = loadJson(CaseContainerDTO[].class);
    cachedCase = loadJson(CachedCase[].class);
    when(appConfig.getChannel()).thenReturn(Channel.CC);
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
    verifyRmCaseCall(0);
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

  private void mockRmCannotFindCase() {
    ResponseStatusException rmLookupMockException =
        new ResponseStatusException(HttpStatus.NOT_FOUND);
    when(caseServiceClient.getCaseById(eq(UUID_0), eq(false))).thenThrow(rmLookupMockException);
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
  public void shouldAcceptCaseWhenFoundFromCache() throws Exception {
    mockRmCannotFindCase();
    when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.of(cachedCase));
    target.modifyCase(requestDTO);
    // WRITEME check results
  }

  @Test
  public void shouldReturnNotFoundWhenNeitherRmOrCacheCaseExists() throws Exception {
    mockRmCannotFindCase();
    when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.empty());
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
  }

  private void verifyModifyAddress(
      CaseType requestCaseType, EstabType requestEstabType, String existingEstabType)
      throws Exception {
    requestDTO.setCaseType(requestCaseType);
    requestDTO.setEstabType(requestEstabType);
    caseContainerDTO.setEstabType(existingEstabType);
    mockRmHasCase();
    target.modifyCase(requestDTO);
    verifyRmCaseCall(1);
    verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
    verifyEventNotSent(EventType.ADDRESS_TYPE_CHANGED);
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingHouseHold() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, EstabType.HOUSEHOLD.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingCastleHH() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, EstabType.CASTLES.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingEmbassySPG() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, EstabType.EMBASSY.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestHH_ExistingOtherNull() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, "Oblivion Sky Tower");
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingHouseHold() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, EstabType.HOUSEHOLD.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingCastleHH() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, EstabType.CASTLES.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingEmbassySPG() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, EstabType.EMBASSY.getCode());
  }

  @Test
  public void shouldModifyAddress_RequestSPG_ExistingOtherNull() throws Exception {
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, "Oblivion Sky Tower");
  }

  @Test
  public void shouldModifyAddress_RequestCE_ExistingPrisonCE() throws Exception {
    verifyModifyAddress(CaseType.CE, EstabType.HOLIDAY_PARK, "prison");
  }

  private void verifyAddressTypeChanged(
      CaseType requestCaseType, EstabType requestEstabType, String existingEstabType)
      throws Exception {
    requestDTO.setCaseType(requestCaseType);
    requestDTO.setEstabType(requestEstabType);
    caseContainerDTO.setEstabType(existingEstabType);
    mockRmHasCase();
    target.modifyCase(requestDTO);
    verifyRmCaseCall(1);
    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressModification.class);
    verifyEventNotSent(EventType.ADDRESS_MODIFIED);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_ExistingPrisonCE() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.HOUSEHOLD, EstabType.PRISON.getCode());
  }

  @Test
  public void shouldChangeAddressType_RequestSPG_ExistingPrisonCE() throws Exception {
    verifyAddressTypeChanged(CaseType.SPG, EstabType.EMBASSY, EstabType.PRISON.getCode());
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingHousehold() throws Exception {
    verifyAddressTypeChanged(CaseType.CE, EstabType.HOLIDAY_PARK, EstabType.HOUSEHOLD.getCode());
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingEmbassySPG() throws Exception {
    verifyAddressTypeChanged(CaseType.CE, EstabType.HOLIDAY_PARK, EstabType.EMBASSY.getCode());
  }

  @Test
  public void shouldRejectNorthernIrelandChangeFromHouseholdToCE() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setRegion(Region.N.name());
    mockRmHasCase();
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertEquals(
        "Cannot convert Northern Ireland Household to Communal Establishment", e.getMessage());
  }
}
