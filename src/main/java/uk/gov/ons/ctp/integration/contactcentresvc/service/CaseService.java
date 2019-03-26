package uk.gov.ons.ctp.integration.contactcentresvc.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;

public interface CaseService {

  public default CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO) {
    return createFakeCaseDTO("Id");
  }

  public default CaseDTO getCaseByUPRN(
      final UniquePropertyReferenceNumber uprn, CaseRequestDTO requestParamsDTO) {
    return createFakeCaseDTO("UPRN");
  }

  public default CaseDTO getCaseByCaseReference(final long ref, CaseRequestDTO requestParamsDTO) {
    return createFakeCaseDTO("CaseReference");
  }

  public default String getLaunchURLForCaseId(
      final UUID caseId, LaunchRequestDTO requestParamsDTO) {
    return "https://www.google.co.uk/search?q=FAKE+"
        + (caseId.hashCode() & 0xFF)
        + (requestParamsDTO.getAgentId().hashCode() & 0xFF)
        + "\n";
  }

  public default CaseDTO createFakeCaseDTO(String createdForMarker) {
    CaseResponseDTO caseResponseDTO1 =
        CaseResponseDTO.builder()
            .dateTime("2016-11-09T11:44:44.797")
            .inboundChannel("FAKE channel response")
            .build();
    CaseResponseDTO caseResponseDTO2 =
        CaseResponseDTO.builder()
            .dateTime("2018-05-15T08:08:08.888")
            .inboundChannel("Another FAKE channel response")
            .build();

    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .id(createSemiRandomFakeUUID())
            .description("Made up case event DTO")
            .category("CAT-FAKE")
            .createdDateTime(LocalDateTime.now())
            .build();
    CaseEventDTO caseEventDTO2 =
        CaseEventDTO.builder()
            .id(createSemiRandomFakeUUID())
            .description("Another fake case event DTO")
            .category("CAT-FAKE2")
            .createdDateTime(LocalDateTime.now())
            .build();
    CaseEventDTO caseEventDTO3 =
        CaseEventDTO.builder()
            .id(createSemiRandomFakeUUID())
            .description("Yet another fake case event DTO")
            .category("CAT-FAKE")
            .createdDateTime(LocalDateTime.now())
            .build();

    CaseDTO fakeCaseDTO =
        CaseDTO.builder()
            .id(createSemiRandomFakeUUID())
            .caseRef("123-fake-456")
            .caseType("caseType-pmb")
            .createdDateTime(LocalDateTime.now())
            .addressLine1("The Novelty Rock Emporium")
            .addressLine2("Rock House")
            .addressLine3("Cowick Lane")
            .addressLine4(createdForMarker)
            .town("Exeter")
            .region("FAKE")
            .postcode("EX2 9HY")
            .responses(Arrays.asList(caseResponseDTO1, caseResponseDTO2))
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
