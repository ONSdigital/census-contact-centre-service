package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AppointmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;

public interface CaseService {

  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO);

  public default List<CaseDTO> getCaseByUPRN(
      final UniquePropertyReferenceNumber uprn, CaseRequestDTO requestParamsDTO) {
    List<CaseDTO> cases = new ArrayList<>();
    cases.add(createFakeCaseDTO("Tinky Winky"));
    cases.add(createFakeCaseDTO("LaLa"));
    cases.add(createFakeCaseDTO("Po"));
    return cases;
  }

  public default CaseDTO getCaseByCaseReference(final long ref, CaseRequestDTO requestParamsDTO) {
    return createFakeCaseDTO("Stoke Hill");
  }

  public default String getLaunchURLForCaseId(
      final UUID caseId, LaunchRequestDTO requestParamsDTO) {
    return "{\"url\": \"https://www.google.co.uk/search?q=FAKE+"
        + (caseId.hashCode() & 0xFF)
        + (requestParamsDTO.getAgentId().hashCode() & 0xFF)
        + "\"}\n";
  }

  public default ResponseDTO fulfilmentRequestByPost(
      UUID caseId, PostalFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(caseId.toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO fulfilmentRequestBySMS(
      UUID caseId, SMSFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(caseId.toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO fulfilmentUnresolvedRequestByPost(
      PostalUnresolvedFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO fulfilmentUnresolvedRequestBySMS(
      SMSUnresolvedFulfilmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO makeAppointment(UUID caseId, AppointmentRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(caseId.toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return fakeResponse;
  }

  public default ResponseDTO reportRefusal(UUID caseId, @Valid RefusalRequestDTO requestBodyDTO) {
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(caseId.toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return fakeResponse;
  }

  private CaseDTO createFakeCaseDTO(String addressLine4) {
    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .description("Made up case event DTO")
            .category("create")
            .createdDateTime(new Date())
            .build();
    CaseEventDTO caseEventDTO2 =
        CaseEventDTO.builder()
            .description("Another fake case event DTO")
            .category("update")
            .createdDateTime(new Date())
            .build();
    CaseEventDTO caseEventDTO3 =
        CaseEventDTO.builder()
            .description("Yet another fake case event DTO")
            .category("update")
            .createdDateTime(new Date())
            .build();

    CaseDTO fakeCaseDTO =
        CaseDTO.builder()
            .id(createSemiRandomFakeUUID())
            .caseRef("123456789")
            .caseType("HI")
            .createdDateTime(new Date())
            .addressLine1("The Novelty Rock Emporium")
            .addressLine2("Rock House")
            .addressLine3("Cowick Lane")
            .townName("Exeter")
            .region("E")
            .postcode("EX2 9HY")
            .caseEvents(Arrays.asList(caseEventDTO1, caseEventDTO2, caseEventDTO3))
            .build();

    return fakeCaseDTO;
  }

  private UUID createSemiRandomFakeUUID() {
    String randomUUID = UUID.randomUUID().toString();
    String firstPart = randomUUID.substring(0, 9);
    String lastPart = randomUUID.substring(23);
    String semiRandomUUID = firstPart + "aaaa-bbbb-cccc" + lastPart;

    return UUID.fromString(semiRandomUUID);
  }
}
