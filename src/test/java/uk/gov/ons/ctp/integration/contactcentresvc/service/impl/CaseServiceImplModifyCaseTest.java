package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplModifyCaseTest extends CaseServiceImplTestBase {

  private ModifyCaseRequestDTO requestDTO;
  private CaseContainerDTO caseContainerDTO;
  private CachedCase cachedCase;

  @Captor private ArgumentCaptor<CachedCase> cachedCaseCaptor;

  @Before
  public void setup() {
    mockCaseEventWhiteList();
    requestDTO = FixtureHelper.loadClassFixtures(ModifyCaseRequestDTO[].class).get(0);
    caseContainerDTO = FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(0);
    cachedCase = FixtureHelper.loadPackageFixtures(CachedCase[].class).get(0);
    when(appConfig.getChannel()).thenReturn(Channel.CC);
  }

  private void verifyRejectIncompatible(EstabType estabType, CaseType caseType) {
    requestDTO.setEstabType(estabType);
    requestDTO.setCaseType(caseType);
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertTrue(e.getMessage().contains("is not compatible with caseType of"));
  }

  @Test
  public void shouldRejectIncompatibleCaseTypeAndEstabType() {
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
    verify(caseServiceClient, times(times)).getCaseById(any(), eq(true));
  }

  private void mockRmHasCase() {
    when(caseServiceClient.getCaseById(eq(UUID_0), eq(true))).thenReturn(caseContainerDTO);
  }

  private void mockRmCannotFindCase() {
    ResponseStatusException rmLookupMockException =
        new ResponseStatusException(HttpStatus.NOT_FOUND);
    when(caseServiceClient.getCaseById(eq(UUID_0), eq(true))).thenThrow(rmLookupMockException);
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

  @Test
  public void shouldRejectExistingHouseholdIndividualCase() {
    caseContainerDTO.setCaseType(CaseType.HI.name());
    mockRmHasCase();
    ResponseStatusException e =
        assertThrows(ResponseStatusException.class, () -> target.modifyCase(requestDTO));
    assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    assertEquals("Case is not suitable", e.getReason());
  }

  private void verifyModifyAddress(
      CaseType requestCaseType, EstabType requestEstabType, String existingEstabType)
      throws Exception {
    verifyModifyAddress(requestCaseType, requestEstabType, existingEstabType, requestCaseType);
  }

  private void verifyModifyAddress(
      CaseType requestCaseType,
      EstabType requestEstabType,
      String existingEstabType,
      CaseType existingCaseType)
      throws Exception {
    requestDTO.setCaseType(requestCaseType);
    requestDTO.setEstabType(requestEstabType);
    caseContainerDTO.setEstabType(existingEstabType);
    caseContainerDTO.setCaseType(existingCaseType.name());
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
    verifyModifyAddress(CaseType.HH, EstabType.HOUSEHOLD, "Oblivion Sky Tower", CaseType.SPG);
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
    verifyModifyAddress(CaseType.SPG, EstabType.EMBASSY, "Oblivion Sky Tower", CaseType.HH);
  }

  @Test
  public void shouldModifyAddress_RequestCE_ExistingPrisonCE() throws Exception {
    verifyModifyAddress(CaseType.CE, EstabType.HOLIDAY_PARK, "prison");
  }

  @Test
  public void shouldModifyAddress_RequestHH_EstabTypeOTHER() throws Exception {
    verifyModifyAddress(CaseType.HH, EstabType.OTHER, EstabType.HOUSEHOLD.getCode());
  }

  // these fields are not needed for addressTypeChanged event
  private void verifyNullFields(CollectionCase collectionCase) {
    assertNull(collectionCase.getCaseType());
    assertNull(collectionCase.getCaseRef());
    assertNull(collectionCase.getSurvey());
    assertNull(collectionCase.getCollectionExerciseId());
    assertNull(collectionCase.getContact());
    assertNull(collectionCase.getActionableFrom());
    assertNull(collectionCase.getCreatedDateTime());
  }

  private void verifyAddressForTypeChange(Address address) {
    assertEquals(requestDTO.getCaseType().name(), address.getAddressType());
    assertEquals(requestDTO.getEstabType().getCode(), address.getEstabType());
    assertEquals(requestDTO.getCeOrgName(), address.getOrganisationName());

    assertEquals(requestDTO.getAddressLine1(), address.getAddressLine1());
    assertEquals(requestDTO.getAddressLine2(), address.getAddressLine2());
    assertEquals(requestDTO.getAddressLine3(), address.getAddressLine3());

    // none of the following elements are required
    assertNull(address.getTownName());
    assertNull(address.getPostcode());
    assertNull(address.getRegion());
    assertNull(address.getUprn());
    assertNull(address.getLatitude());
    assertNull(address.getLongitude());
    assertNull(address.getEstabUprn());
    assertNull(address.getAddressLevel());
  }

  private void verifyAddressTypeChanged(
      CaseType requestCaseType,
      EstabType requestEstabType,
      String existingEstabType,
      CaseType existingCaseType)
      throws Exception {
    requestDTO.setCaseType(requestCaseType);
    requestDTO.setEstabType(requestEstabType);
    caseContainerDTO.setEstabType(existingEstabType);
    caseContainerDTO.setCaseType(existingCaseType.name());
    mockRmHasCase();
    target.modifyCase(requestDTO);
    verifyRmCaseCall(1);
    AddressTypeChanged payload =
        verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);

    assertNotNull(payload.getNewCaseId());
    assertNotEquals(requestDTO.getCaseId(), payload.getNewCaseId());

    CollectionCase collectionCase = payload.getCollectionCase();
    assertEquals(caseContainerDTO.getId().toString(), collectionCase.getId());
    assertEquals(requestDTO.getCaseId().toString(), collectionCase.getId());
    assertEquals(requestDTO.getCeUsualResidents(), collectionCase.getCeExpectedCapacity());
    verifyNullFields(collectionCase);

    Address address = collectionCase.getAddress();
    verifyAddressForTypeChange(address);

    verifyEventNotSent(EventType.ADDRESS_MODIFIED);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_ExistingOtherCE() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.HOUSEHOLD, "Oblivion Sky Tower", CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_ExistingPrisonCE() throws Exception {
    verifyAddressTypeChanged(
        CaseType.HH, EstabType.HOUSEHOLD, EstabType.PRISON.getCode(), CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestSPG_ExistingPrisonCE() throws Exception {
    verifyAddressTypeChanged(
        CaseType.SPG, EstabType.EMBASSY, EstabType.PRISON.getCode(), CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingHousehold() throws Exception {
    verifyAddressTypeChanged(
        CaseType.CE, EstabType.HOLIDAY_PARK, EstabType.HOUSEHOLD.getCode(), CaseType.HH);
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingEmbassySPG() throws Exception {
    verifyAddressTypeChanged(
        CaseType.CE, EstabType.HOLIDAY_PARK, EstabType.EMBASSY.getCode(), CaseType.SPG);
  }

  @Test
  public void shouldChangeAddressType_RequestHH_WithEstabTypeOTHER() throws Exception {
    verifyAddressTypeChanged(CaseType.HH, EstabType.OTHER, EstabType.PRISON.getCode(), CaseType.CE);
  }

  @Test
  public void shouldChangeAddressType_RequestCE_ExistingOtherSPG() throws Exception {
    verifyAddressTypeChanged(
        CaseType.CE, EstabType.HOLIDAY_PARK, "Oblivion Sky Tower", CaseType.SPG);
  }

  @Test
  public void shouldRejectNorthernIrelandChangeFromHouseholdToCE() {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setCaseType(CaseType.HH.name());
    caseContainerDTO.setRegion(Region.N.name());
    mockRmHasCase();
    CTPException e = assertThrows(CTPException.class, () -> target.modifyCase(requestDTO));
    assertEquals(Fault.BAD_REQUEST, e.getFault());
    assertEquals(
        "All queries relating to Communal Establishments in Northern Ireland should be escalated to NISRA HQ",
        e.getMessage());
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotInNorthernIreland() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setCaseType(CaseType.HH.name());
    caseContainerDTO.setRegion(Region.E.name());
    mockRmHasCase();
    target.modifyCase(requestDTO);
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotInNorthernIrelandAndNotRequestedCE()
      throws Exception {
    requestDTO.setCaseType(CaseType.SPG);
    requestDTO.setEstabType(EstabType.EMBASSY);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    caseContainerDTO.setCaseType(CaseType.HH.name());
    caseContainerDTO.setRegion(Region.E.name());
    mockRmHasCase();
    target.modifyCase(requestDTO);
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotRequestedCE() throws Exception {
    requestDTO.setCaseType(CaseType.SPG);
    requestDTO.setEstabType(EstabType.EMBASSY);
    caseContainerDTO.setEstabType(EstabType.YOUTH_HOSTEL.getCode());
    caseContainerDTO.setCaseType(CaseType.CE.name());
    caseContainerDTO.setRegion(Region.N.name());
    mockRmHasCase();
    target.modifyCase(requestDTO);
  }

  @Test
  public void shouldNotRejectNorthernIrelandChangeWhenNotExistingHouseHold() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    caseContainerDTO.setCaseType(CaseType.SPG.name());
    caseContainerDTO.setRegion(Region.N.name());
    mockRmHasCase();
    target.modifyCase(requestDTO);
  }

  private CachedCase createExpectedCachedCaseFromExistingCache(UUID id, Date createdDateTime) {
    assertTrue(createdDateTime.after(cachedCase.getCreatedDateTime()));

    return CachedCase.builder()
        .id(id.toString())
        .uprn(cachedCase.getUprn())
        .createdDateTime(createdDateTime)
        .formattedAddress(null)
        .addressLine1(requestDTO.getAddressLine1())
        .addressLine2(requestDTO.getAddressLine2())
        .addressLine3(requestDTO.getAddressLine3())
        .townName(cachedCase.getTownName())
        .postcode(cachedCase.getPostcode())
        .addressType(requestDTO.getCaseType().name())
        .caseType(requestDTO.getCaseType())
        .estabType(requestDTO.getEstabType().getCode())
        .region(cachedCase.getRegion())
        .ceOrgName(requestDTO.getCeOrgName())
        .caseEvents(cachedCase.getCaseEvents())
        .build();
  }

  private Date originalRmCaseCreationDate() {
    return FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class).get(0).getCreatedDateTime();
  }

  private CachedCase createExpectedCachedCaseFromExistingRmCase(UUID id, Date createdDateTime) {
    List<CaseEventDTO> expectedCaseEvents = filterEvents(caseContainerDTO);

    assertTrue(createdDateTime.after(originalRmCaseCreationDate()));

    return CachedCase.builder()
        .id(id.toString())
        .uprn(caseContainerDTO.getUprn())
        .createdDateTime(createdDateTime)
        .formattedAddress(null)
        .addressLine1(requestDTO.getAddressLine1())
        .addressLine2(requestDTO.getAddressLine2())
        .addressLine3(requestDTO.getAddressLine3())
        .townName(caseContainerDTO.getTownName())
        .postcode(caseContainerDTO.getPostcode())
        .addressType(requestDTO.getCaseType().name())
        .caseType(requestDTO.getCaseType())
        .estabType(requestDTO.getEstabType().getCode())
        .region(caseContainerDTO.getRegion())
        .ceOrgName(requestDTO.getCeOrgName())
        .caseEvents(expectedCaseEvents)
        .build();
  }

  private void verifySavedCashedCase(CachedCase expected) throws Exception {
    verify(dataRepo).writeCachedCase(cachedCaseCaptor.capture());
    CachedCase saved = cachedCaseCaptor.getValue();
    assertEquals(expected, saved);
  }

  @Test
  public void shouldSaveCachedCaseWhenAddressModified() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.HOUSEHOLD);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
    verifySavedCashedCase(
        createExpectedCachedCaseFromExistingRmCase(
            caseContainerDTO.getId(), response.getCreatedDateTime()));
  }

  @Test
  public void shouldUpdateCachedCaseWhenAddressModified() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.HOUSEHOLD);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());

    mockRmCannotFindCase();
    when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.of(cachedCase));

    CaseDTO response = target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
    verifySavedCashedCase(
        createExpectedCachedCaseFromExistingCache(
            UUID.fromString(cachedCase.getId()), response.getCreatedDateTime()));
  }

  @Test
  public void shouldSaveCachedCaseWhenAddressModifiedToEstabTypeOTHER() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.OTHER);
    caseContainerDTO.setEstabType(EstabType.HOUSEHOLD.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_MODIFIED, AddressModification.class);
    verifySavedCashedCase(
        createExpectedCachedCaseFromExistingRmCase(
            caseContainerDTO.getId(), response.getCreatedDateTime()));
  }

  @Test
  public void shouldSaveCachedCaseWhenAddressTypeChanged() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
    verifySavedCashedCase(
        createExpectedCachedCaseFromExistingRmCase(
            response.getId(), response.getCreatedDateTime()));
  }

  @Test
  public void shouldUpdateCachedCaseWhenAddressTypeChanged() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.HOLIDAY_PARK);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());

    mockRmCannotFindCase();
    when(dataRepo.readCachedCaseById(eq(UUID_0))).thenReturn(Optional.of(cachedCase));

    CaseDTO response = target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
    verifySavedCashedCase(
        createExpectedCachedCaseFromExistingCache(response.getId(), response.getCreatedDateTime()));
  }

  @Test
  public void shouldSaveCachedCaseWhenAddressTypeChangedToEstabTypeOTHER() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.OTHER);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);

    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
    verifySavedCashedCase(
        createExpectedCachedCaseFromExistingRmCase(
            response.getId(), response.getCreatedDateTime()));
  }

  private String uprnStr(UniquePropertyReferenceNumber uprn) {
    return uprn == null ? null : ("" + uprn.getValue());
  }

  private void verifyCaseResponse(CaseDTO response, boolean newCaseIdExpected) {
    assertNotNull(response);

    if (newCaseIdExpected) {
      assertNotEquals(caseContainerDTO.getId(), response.getId());
      assertNull(response.getCaseRef());
    } else {
      assertEquals(caseContainerDTO.getId(), response.getId());
      assertEquals(caseContainerDTO.getCaseRef(), response.getCaseRef());
    }

    assertEquals(requestDTO.getCaseType().name(), response.getCaseType());
    assertEquals(requestDTO.getCaseType().name(), response.getAddressType());
    assertEquals(requestDTO.getEstabType(), response.getEstabType());
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
    assertEquals(ALL_DELIVERY_CHANNELS, response.getAllowedDeliveryChannels());
    assertTrue(response.getCaseEvents().isEmpty());
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
  public void shouldReturnModifiedCaseWhenAddressModifiedToEstabTypeOTHER() throws Exception {
    requestDTO.setCaseType(CaseType.HH);
    requestDTO.setEstabType(EstabType.OTHER);
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

  @Test
  public void shouldReturnModifiedCaseWhenAddressTypeChangedToEstabTypeOTHER() throws Exception {
    requestDTO.setCaseType(CaseType.CE);
    requestDTO.setEstabType(EstabType.OTHER);
    caseContainerDTO.setEstabType(EstabType.EMBASSY.getCode());
    mockRmHasCase();
    CaseDTO response = target.modifyCase(requestDTO);
    verifyCaseResponse(response, true);
    verifyEventSent(EventType.ADDRESS_TYPE_CHANGED, AddressTypeChanged.class);
  }
}
