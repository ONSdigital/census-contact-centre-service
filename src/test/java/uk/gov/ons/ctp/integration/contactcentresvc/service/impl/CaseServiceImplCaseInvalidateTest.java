package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.UUID_0;

import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#invalidateCase(InvalidateCaseRequestDTO) invalidateCase}. */
@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplCaseInvalidateTest extends CaseServiceImplTestBase {

  @Before
  public void setup() {
    when(appConfig.getChannel()).thenReturn(Channel.CC);
  }

  @SneakyThrows
  private void checkInvalidateCaseForStatus(CaseStatus status) {
    List<InvalidateCaseRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(InvalidateCaseRequestDTO[].class);
    InvalidateCaseRequestDTO dto = requestsFromCCSvc.get(0);
    dto.setStatus(status);
    dto.setCaseId(UUID_0);
    List<CaseContainerDTO> casesFromCaseService =
        FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
    CaseContainerDTO ccDto = casesFromCaseService.get(0);
    when(caseServiceClient.getCaseById(UUID_0, false)).thenReturn(ccDto);
    ResponseDTO response = target.invalidateCase(dto);

    assertEquals(dto.getCaseId().toString(), response.getId());
    assertNotNull(response.getDateTime());

    AddressNotValid payload = verifyEventSent(EventType.ADDRESS_NOT_VALID, AddressNotValid.class);
    assertEquals(dto.getCaseId(), payload.getCollectionCase().getId());
    assertEquals(dto.getNotes(), payload.getNotes());
    assertEquals(dto.getStatus().name(), payload.getReason());
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDerelict() {
    checkInvalidateCaseForStatus(CaseStatus.DERELICT);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDemolished() {
    checkInvalidateCaseForStatus(CaseStatus.DEMOLISHED);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusNonResidential() {
    checkInvalidateCaseForStatus(CaseStatus.NON_RESIDENTIAL);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusUnderConstruction() {
    checkInvalidateCaseForStatus(CaseStatus.UNDER_CONSTRUCTION);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusSplitAddress() {
    checkInvalidateCaseForStatus(CaseStatus.SPLIT_ADDRESS);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusMerged() {
    checkInvalidateCaseForStatus(CaseStatus.MERGED);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDuplicate() {
    checkInvalidateCaseForStatus(CaseStatus.DUPLICATE);
  }

  @Test
  public void shouldInvalidateCaseWhenStatusDoesNotExist() {
    checkInvalidateCaseForStatus(CaseStatus.DOES_NOT_EXIST);
  }

  @Test
  public void shouldRejectCaseNotFoundInRMOrCache() throws Exception {
    when(caseServiceClient.getCaseById(any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)); // Not in RM
    when(dataRepo.readCachedCaseById(any())).thenReturn(Optional.empty()); // Not in cache either
    List<InvalidateCaseRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(InvalidateCaseRequestDTO[].class);
    InvalidateCaseRequestDTO dto = requestsFromCCSvc.get(0);
    CTPException exception = assertThrows(CTPException.class, () -> target.invalidateCase(dto));
    assertEquals(Fault.RESOURCE_NOT_FOUND, exception.getFault());
  }

  @Test
  public void shouldInvalidateCaseWhenCaseOnlyInCache() throws Exception {
    when(caseServiceClient.getCaseById(any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)); // Not in RM
    CachedCase cc = new CachedCase();
    cc.setCaseType(CaseType.HH);
    when(dataRepo.readCachedCaseById(any())).thenReturn(Optional.of(cc));
    List<InvalidateCaseRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(InvalidateCaseRequestDTO[].class);
    InvalidateCaseRequestDTO dto = requestsFromCCSvc.get(0);
    target.invalidateCase(dto);
  }

  @Test
  public void shouldRejectCaseOfTypeCE() throws Exception {
    List<InvalidateCaseRequestDTO> requestsFromCCSvc =
        FixtureHelper.loadClassFixtures(InvalidateCaseRequestDTO[].class);
    InvalidateCaseRequestDTO dto = requestsFromCCSvc.get(0);
    dto.setCaseId(UUID_0);
    List<CaseContainerDTO> casesFromCaseService =
        FixtureHelper.loadPackageFixtures(CaseContainerDTO[].class);
    CaseContainerDTO ccDto = casesFromCaseService.get(0);
    ccDto.setCaseType("CE");
    when(caseServiceClient.getCaseById(UUID_0, false)).thenReturn(ccDto);
    Exception e = assertThrows(Exception.class, () -> target.invalidateCase(dto));
    assertEquals(
        "All CE addresses will be validated by a Field Officer. "
            + "It is not necessary to submit this Invalidation request.",
        e.getMessage());
  }
}
