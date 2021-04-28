package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class UPRNBlacklist {
  private String uprnBlacklistPath;
  private Set<Long> defaultUprnBlacklist;
}
