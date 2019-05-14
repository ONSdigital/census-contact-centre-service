package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostcodeSearchResponseDTO {

  private String postcode;

  private ArrayList<AddressIndexAddressDTO> addresses;

  private int limit;

  private int offset;

  private int total;
}
