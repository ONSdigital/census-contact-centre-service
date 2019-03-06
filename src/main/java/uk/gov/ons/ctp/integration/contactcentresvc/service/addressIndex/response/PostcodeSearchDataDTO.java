package uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response;

import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties({"apiVersion"})
public class PostcodeSearchDataDTO {

  private String dataVersion;

  private PostcodeSearchResponseDTO response;

  private ResponseStatusData status;

  private ArrayList<String> errors;
}
