package uk.gov.ons.ctp.integration.contactcentresvc.cloud;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CachedCase {

  private String id;

  private String uprn;

  private String formattedAddress;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String postcode;

  private String addressType;

  private String estabType;

  private String region;
}
