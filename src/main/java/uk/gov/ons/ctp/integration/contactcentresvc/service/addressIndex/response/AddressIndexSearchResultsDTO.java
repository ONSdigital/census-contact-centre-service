package uk.gov.ons.ctp.integration.contactcentresvc.service.addressIndex.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import lombok.Data;

@Data
@JsonIgnoreProperties({"apiVersion"})
public class AddressIndexSearchResultsDTO {

  private String dataVersion;

  private PostcodeSearchResponseDTO response;

  private ResponseStatusData status;

  private ArrayList<String> errors;
}
