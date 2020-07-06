package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressNotValid;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture;
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

    ArgumentCaptor<AddressNotValid> payloadCaptor = ArgumentCaptor.forClass(AddressNotValid.class);

    verify(eventPublisher)
        .sendEvent(
            eq(EventType.ADDRESS_NOT_VALID),
            eq(Source.CONTACT_CENTRE_API),
            eq(Channel.CC),
            payloadCaptor.capture());

    AddressNotValid payload = payloadCaptor.getValue();
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

  @Test(expected = ResponseStatusException.class)
  public void shouldRejectCaseNotFound() throws Exception {
    when(caseServiceClient.getCaseById(any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    target.invalidateCase(dto);
  }

  @Test(expected = Exception.class)
  public void shouldRejectCaseOfTypeCE() throws Exception {
    InvalidateCaseRequestDTO dto = CaseServiceFixture.createInvalidateCaseRequestDTO();
    dto.setCaseId(UUID.fromString("77346443-64ae-422e-9b93-d5250f48a27a"));
    CaseContainerDTO ccDto = CaseServiceFixture.createCaseContainerDTO();
    Mockito.when(
            caseServiceClient.getCaseById(
                UUID.fromString("77346443-64ae-422e-9b93-d5250f48a27a"), false))
        .thenReturn(ccDto);
    target.invalidateCase(dto);
  }
}
