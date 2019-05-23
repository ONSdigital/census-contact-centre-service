package uk.gov.ons.ctp.integration.contactcentresvc.config;

import java.util.Set;
import lombok.Data;
import uk.gov.ons.ctp.common.rest.RestClientConfig;

@Data
public class CaseServiceSettings {
  private String caseByIdQueryPath;
  private String caseByUprnQueryPath;
  private String caseByCaseReferenceQueryPath;
  private Set<String> whitelistedEventCategories;
  private RestClientConfig restClientConfig;
}
