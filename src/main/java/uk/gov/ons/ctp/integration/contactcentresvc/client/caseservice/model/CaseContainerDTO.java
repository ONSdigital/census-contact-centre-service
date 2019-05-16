package uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class CaseContainerDTO {

  private UUID id;

  private String arid;

  private String estabArid;

  private String estabType;

  private String uprn;

  private String caseRef;

  private String caseType;

  private String createdDateTime;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String postcode;

  private String organisationName;

  private String addressLevel;

  private String abpCode;

  private String region;

  private String latitude;

  private String longitude;

  private String oa;

  private String lsoa;

  private String msoa;

  private String lad;

  private List<EventDTO> caseEvents;
}
