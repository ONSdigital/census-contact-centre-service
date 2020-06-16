package uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Address Index query result splitting address into Census component fields */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressIndexAddressCompositeDTO {

  private String uprn;

  private String formattedAddress;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String postcode;

  private String foundAddressType;

  private String censusAddressType;

  private String censusEstabType; // Holds value of EstabType.code

  private String countryCode;
}
