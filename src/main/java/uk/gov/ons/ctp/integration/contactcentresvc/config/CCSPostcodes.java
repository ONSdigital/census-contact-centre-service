package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class CCSPostcodes {
  private Set<String> ccsPostcodesToCheck;

  public boolean isInCCSPostcodes(String postcode) {
    return ccsPostcodesToCheck.contains(postcode);
  }
}
