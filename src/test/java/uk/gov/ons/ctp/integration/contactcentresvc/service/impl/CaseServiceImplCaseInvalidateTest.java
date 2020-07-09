package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture;
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
    Mockito.when(appConfig.getChannel()).thenReturn(Channel.CC);
  }

  @SneakyThrows
  private void checkInvalidateCaseForStatus(CaseStatus status) {
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    dto.setStatus(status);

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
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    CTPException exception = assertThrows(CTPException.class, () -> target.invalidateCase(dto));
    assertEquals(Fault.RESOURCE_NOT_FOUND, exception.getFault());
  }

  @Test
  public void shouldInvalidateCaseWhenCaseOnlyInCache() throws Exception {
    when(caseServiceClient.getCaseById(any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)); // Not in RM
    when(dataRepo.readCachedCaseById(any()))
        .thenReturn(Optional.of(new CachedCase())); // It's in cache
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    target.invalidateCase(dto);
  }
}
