package uk.gov.ons.ctp.integration.contactcentresvc;

import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseStatus;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CaseServiceFixture {
  public static final UUID UUID_0 = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  public static final UUID UUID_1 = UUID.fromString("b7565b5e-2222-2222-2222-918c0d3642ed");
  public static final EstabType AN_ESTAB_TYPE = EstabType.HOUSEHOLD;
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

  //  public static ModifyCaseRequestDTO createModifyCaseRequestDTO() {
  //    ModifyCaseRequestDTO dto =
  //        ModifyCaseRequestDTO.builder().caseId(UUID_0).estabType(AN_ESTAB_TYPE).build();
  //
  //    dto.setAddressLine1(AN_ADDRESS_LINE_1);
  //    dto.setAddressLine2(AN_ADDRESS_LINE_2);
  //    dto.setAddressLine3(AN_ADDRESS_LINE_3);
  //    dto.setTownName(A_TOWN_NAME);
  //    dto.setPostcode(A_POSTCODE);
  //    dto.setRegion(A_REGION);
  //    dto.setDateTime(A_REQUEST_DATE_TIME);
  //    return dto;
  //  }

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

  public static LaunchRequestDTO createLaunchRequestDTO(boolean individual) {
    LaunchRequestDTO tdo = new LaunchRequestDTO();
    tdo.setAgentId(AN_AGENT_ID);
    tdo.setIndividual(individual);
    return tdo;
  }

  //  public static CaseContainerDTO createCaseContainerDTO() {
  //    EventDTO eventDTO1 = new EventDTO();
  //    eventDTO1.setId("101");
  //    eventDTO1.setEventType("CASE_CREATED");
  //    eventDTO1.setDescription("Initial creation of case");
  //    eventDTO1.setCreatedDateTime(new Date());
  //
  //    EventDTO eventDTO2 = new EventDTO();
  //    eventDTO2.setId("102");
  //    eventDTO2.setEventType("X11");
  //    eventDTO2.setDescription("Should be filtered out");
  //    eventDTO2.setCreatedDateTime(new Date());
  //
  //    EventDTO eventDTO3 = new EventDTO();
  //    eventDTO3.setId("103");
  //    eventDTO3.setEventType("CASE_UPDATED");
  //    eventDTO3.setDescription("Create Household Visit");
  //    eventDTO3.setCreatedDateTime(new Date());
  //
  //    CaseContainerDTO dto = new CaseContainerDTO();
  //    dto.setCaseRef("1000000000000001");
  //    dto.setEstabType("ET");
  //    dto.setUprn("334999999999");
  //    dto.setEstabUprn("334111111111");
  //    dto.setCreatedDateTime(new Date());
  //    dto.setLastUpdated(new Date());
  //    dto.setAddressLine1("Napier House");
  //    dto.setAddressLine2("12 Park Street");
  //    dto.setAddressLine3("Parkhead");
  //    dto.setTownName("Glasgow");
  //    dto.setPostcode("G1 2AA");
  //    ;
  //    dto.setOrganisationName("The Invalidating Company");
  //    dto.setAddressLevel("E");
  //    dto.setAbpCode("AACC");
  //    dto.setLatitude("41.40338");
  //    dto.setLongitude("2.17403");
  //    dto.setOa("EE22");
  //    dto.setLsoa("x1");
  //    dto.setMsoa("x2");
  //    dto.setLad("H1");
  //
  //    List<EventDTO> caseEvents = new ArrayList<>();
  //    caseEvents.add(eventDTO1);
  //    caseEvents.add(eventDTO2);
  //    caseEvents.add(eventDTO3);
  //
  //    dto.setCaseEvents(caseEvents);
  //    dto.setId(UUID_0);
  //    dto.setCollectionExerciseId(UUID.fromString("22684ede-7d5f-4f53-9069-2398055c61b2"));
  //    dto.setCaseType("HH");
  //    dto.setAddressType("HH");
  //    dto.setRegion("E12345678");
  //    dto.setSurveyType("x");
  //    dto.setHandDelivery(false);
  //    dto.setSecureEstablishment(false);
  //    return dto;
  //  }
}
