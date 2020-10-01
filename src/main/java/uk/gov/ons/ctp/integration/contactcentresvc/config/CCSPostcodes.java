package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class CCSPostcodes {
  private String ccsPostcodePath;
  private Set<String> ccsDefaultPostcodes;
}
