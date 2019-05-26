package uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDTO {

  private String id;

  private String category;

  private String description;

  private Date createdDateTime;
}
