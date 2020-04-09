package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import lombok.Data;

@Data
@JsonIgnoreProperties({"apiVersion"})
public class AddressIndexSearchResultsSplitDTO {

  private String dataVersion;

  private AddressIndexResponseSplitDTO response;

  private ResponseStatusData status;

  private ArrayList<String> errors;
}
