package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class TelephoneCapture {
  private Set<String> disabled;
}
