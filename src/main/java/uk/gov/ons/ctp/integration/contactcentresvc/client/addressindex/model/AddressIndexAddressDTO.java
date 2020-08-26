package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressIndexAddressDTO {

  private String uprn;

  private AddressIndexAddressCensusDTO census;

  private String formattedAddress;

  private String formattedAddressNag;

  private String formattedAddressPaf;

  private String welshFormattedAddressNag;

  private String welshFormattedAddressPaf;

  private String lpiLogicalStatus;
}
