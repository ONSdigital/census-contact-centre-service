package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressTypeChanged;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplModifyCaseTest extends CaseServiceImplTestBase {

  private ModifyCaseRequestDTO requestDTO;
  private CaseContainerDTO caseContainerDTO;
  private CachedCase cachedCase;

  @Captor private ArgumentCaptor<CachedCase> cachedCaseCaptor;

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
    CaseDTO response = target.modifyCase(requestDTO);
    assertNotNull(response);
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
    CaseDTO response = target.modifyCase(requestDTO);
    assertNotNull(response);
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
    AddressModification payload =
        verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);

    CollectionCaseCompact collectionCase = payload.getCollectionCase();
    assertEquals(caseContainerDTO.getId(), collectionCase.getId());
    assertEquals(requestDTO.getCaseId(), collectionCase.getId());
    assertEquals(requestCaseType.name(), collectionCase.getCaseType());
    assertEquals(requestDTO.getCeUsualResidents(), collectionCase.getCeExpectedCapacity());

    AddressCompact originalAddress = payload.getOriginalAddress();
    assertEquals(caseContainerDTO.getAddressLine1(), originalAddress.getAddressLine1());
    assertEquals(caseContainerDTO.getAddressLine2(), originalAddress.getAddressLine2());
    assertEquals(caseContainerDTO.getAddressLine3(), originalAddress.getAddressLine3());
    assertEquals(caseContainerDTO.getTownName(), originalAddress.getTownName());
    assertEquals(caseContainerDTO.getPostcode(), originalAddress.getPostcode());
    assertEquals(caseContainerDTO.getRegion(), originalAddress.getRegion());
    assertEquals(caseContainerDTO.getUprn(), originalAddress.getUprn());
    assertEquals(caseContainerDTO.getEstabType(), originalAddress.getEstabType());
    assertEquals(caseContainerDTO.getOrganisationName(), originalAddress.getOrganisationName());

    verifyChangedAddress(payload.getNewAddress());

    verifyEventNotSent(EventType.ADDRESS_TYPE_CHANGED);
  }

  private void verifyChangedAddress(AddressCompact newAddress) {
    assertEquals(requestDTO.getAddressLine1(), newAddress.getAddressLine1());
    assertEquals(requestDTO.getAddressLine2(), newAddress.getAddressLine2());
    assertEquals(requestDTO.getAddressLine3(), newAddress.getAddressLine3());
    assertEquals(caseContainerDTO.getTownName(), newAddress.getTownName());
    assertEquals(caseContainerDTO.getPostcode(), newAddress.getPostcode());
    assertEquals(caseContainerDTO.getRegion(), newAddress.getRegion());
    assertEquals(caseContainerDTO.getUprn(), newAddress.getUprn());
    assertEquals(requestDTO.getEstabType().getCode(), newAddress.getEstabType());
    assertEquals(requestDTO.getCeOrgName(), newAddress.getOrganisationName());
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
    AddressTypeChanged payload =
        verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);

    assertNotNull(payload.getNewCaseId());
    assertFalse(requestDTO.getCaseId().equals(payload.getNewCaseId()));

    CollectionCase collectionCase = payload.getCollectionCase();
    assertEquals(caseContainerDTO.getId().toString(), collectionCase.getId());
    assertEquals(requestDTO.getCaseId().toString(), collectionCase.getId());
    assertEquals(requestCaseType.name(), collectionCase.getCaseType());
    assertNull(collectionCase.getCaseRef());
    assertEquals(requestDTO.getCeUsualResidents(), collectionCase.getCeExpectedCapacity());

    Address newAddress = collectionCase.getAddress();
    verifyChangedAddress(newAddress);
    assertEquals(
        requestDTO.getEstabType().getAddressType().get().name(), newAddress.getAddressType());

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

  private void verifySavedCashedCase(boolean newCaseId) throws Exception {
    verify(dataRepo).writeCachedCase(cachedCaseCaptor.capture());
    CachedCase saved = cachedCaseCaptor.getValue();
    if (newCaseId) {
      assertFalse(caseContainerDTO.getId().toString().equals(saved.getId()));
    } else {
      assertEquals(caseContainerDTO.getId().toString(), saved.getId());
    }

    assertEquals(caseContainerDTO.getUprn(), saved.getUprn());
    assertEquals(caseContainerDTO.getCreatedDateTime(), saved.getCreatedDateTime());
    assertNull(saved.getFormattedAddress());
    assertEquals(requestDTO.getAddressLine1(), saved.getAddressLine1());
    assertEquals(requestDTO.getAddressLine2(), saved.getAddressLine2());
    assertEquals(requestDTO.getAddressLine3(), saved.getAddressLine3());
    assertEquals(caseContainerDTO.getTownName(), saved.getTownName());
    assertEquals(caseContainerDTO.getPostcode(), saved.getPostcode());
    assertEquals(requestDTO.getEstabType().getAddressType().get().name(), saved.getAddressType());
    assertEquals(requestDTO.getCaseType(), saved.getCaseType());
    assertEquals(requestDTO.getEstabType(), EstabType.forCode(saved.getEstabType()));
    assertEquals(caseContainerDTO.getRegion(), saved.getRegion());
    assertEquals(requestDTO.getCeOrgName(), saved.getCeOrgName());
  }

  @Test
  public void shouldSaveCachedCaseWhenAddressModified() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.HOUSEHOLD);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    mockRmHasCase();
    target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
    verifySavedCashedCase(false);
  }

  @Test
  public void shouldSaveCachedCaseWhenAddressTypeChanged() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    mockRmHasCase();
    target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
    verifySavedCashedCase(true);
  }

  private String uprnStr(UniquePropertyReferenceNumber uprn) {
    return uprn == null ? null : ("" + uprn.getValue());
  }

  private void verifyCaseResponse(CaseDTO response, boolean newCaseId) {
    assertNotNull(response);

    if (newCaseId) {
      assertFalse(caseContainerDTO.getId().equals(response.getId()));
      assertNull(response.getCaseRef());
    } else {
      assertEquals(caseContainerDTO.getId(), response.getId());
      assertEquals(caseContainerDTO.getCaseRef(), response.getCaseRef());
    }

    assertEquals(requestDTO.getCaseType().name(), response.getCaseType());
    assertEquals(
        requestDTO.getEstabType().getAddressType().get().name(), response.getAddressType());
    assertEquals(requestDTO.getEstabType().getCode(), response.getEstabDescription());
    assertEquals(requestDTO.getAddressLine1(), response.getAddressLine1());
    assertEquals(requestDTO.getAddressLine2(), response.getAddressLine2());
    assertEquals(requestDTO.getAddressLine3(), response.getAddressLine3());
    assertEquals(caseContainerDTO.getTownName(), response.getTownName());
    assertEquals(caseContainerDTO.getPostcode(), response.getPostcode());
    assertEquals(caseContainerDTO.getRegion().substring(0, 1), response.getRegion());
    assertEquals(requestDTO.getCeOrgName(), response.getCeOrgName());
    assertEquals(caseContainerDTO.getUprn(), uprnStr(response.getUprn()));
    assertEquals(caseContainerDTO.getEstabUprn(), uprnStr(response.getEstabUprn()));
  }

  @Test
  public void shouldReturnModifiedCaseWhenAddressModified() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.HOUSEHOLD);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);
    verifyCaseResponse(response, false);
    verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
  }

  @Test
  public void shouldReturnModifiedCaseWhenAddressTypeChanged() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);
    verifyCaseResponse(response, true);
    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
  }
}
