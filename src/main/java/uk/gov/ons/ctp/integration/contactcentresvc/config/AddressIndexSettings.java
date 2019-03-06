package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;
import uk.gov.ons.ctp.common.rest.RestClientConfig;

@Data
public class AddressIndexSettings {
  String postcodeLookupPath;
  RestClientConfig restClientConfig;
}
