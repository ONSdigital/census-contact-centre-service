package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;
import uk.gov.ons.ctp.common.rest.RestClientConfig;

@Data
public class CaseServiceSettings {
  private String caseByIdQueryPath;
  private String caseByUprnQueryPath;
  private String caseByCaseReferenceQueryPath;
  private RestClientConfig restClientConfig;
}
