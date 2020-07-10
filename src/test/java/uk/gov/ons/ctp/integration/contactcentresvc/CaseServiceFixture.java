package uk.gov.ons.ctp.integration.contactcentresvc;

import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CaseServiceFixture {
  public static final UUID UUID_0 = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  public static final UUID UUID_1 = UUID.fromString("b7565b5e-2222-2222-2222-918c0d3642ed");
  public static final EstabType AN_ESTAB_TYPE = EstabType.HOUSEHOLD;
  public static final CaseType A_CASE_TYPE = CaseType.HH;
  public static final CaseStatus A_CASE_STATUS = CaseStatus.DEMOLISHED;
  public static final String SOME_NOTES = "must buy olives from the deli";
  public static final String AN_ADDRESS_LINE_1 = "1 High Street";
  public static final String AN_ADDRESS_LINE_2 = "Delph";
  public static final String AN_ADDRESS_LINE_3 = "Oldham";
  public static final String A_TOWN_NAME = "Manchester";
  public static final String A_POSTCODE = "OL3 5DJ";
  public static final Region A_REGION = Region.E;
  public static final String A_RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705Z";
  public static final Date A_REQUEST_DATE_TIME = new Date();
  public static final String AN_AGENT_ID = "123";
  public static final String A_QUESTIONNAIRE_ID = "566786126";
  // <<<<<<< HEAD
  // =======
  //
  //  public static ModifyCaseRequestDTO createModifyCaseRequestDTO() {
  //    ModifyCaseRequestDTO dto = ModifyCaseRequestDTO.builder().caseId(UUID_0).build();
  //
  //    dto.setCaseType(A_CASE_TYPE);
  //    dto.setEstabType(AN_ESTAB_TYPE);
  //    dto.setAddressLine1(AN_ADDRESS_LINE_1);
  //    dto.setAddressLine2(AN_ADDRESS_LINE_2);
  //    dto.setAddressLine3(AN_ADDRESS_LINE_3);
  //    dto.setDateTime(A_REQUEST_DATE_TIME);
  //    return dto;
  //  }
  //
  //  public static InvalidateCaseRequestDTO createInvalidateCaseRequestDTO() {
  //    InvalidateCaseRequestDTO dto =
  //        InvalidateCaseRequestDTO.builder()
  //            .caseId(UUID_0)
  //            .status(A_CASE_STATUS)
  //            .notes(SOME_NOTES)
  //            .dateTime(A_REQUEST_DATE_TIME)
  //            .build();
  //    return dto;
  //  }
  //
  //  public static LaunchRequestDTO createLaunchRequestDTO(boolean individual) {
  //    LaunchRequestDTO tdo = new LaunchRequestDTO();
  //    tdo.setAgentId(AN_AGENT_ID);
  //    tdo.setIndividual(individual);
  //    return tdo;
  //  }
  // >>>>>>> master
}
