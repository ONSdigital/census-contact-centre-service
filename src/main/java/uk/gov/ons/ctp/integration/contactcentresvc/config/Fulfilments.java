package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class Fulfilments {
  private Set<String> blacklistedCodes;
}
