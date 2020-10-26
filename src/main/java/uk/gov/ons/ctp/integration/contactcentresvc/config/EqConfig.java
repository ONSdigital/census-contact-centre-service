package uk.gov.ons.ctp.integration.contactcentresvc.config;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EqConfig {
  @NotBlank private String protocol;
  @NotBlank private String host;
  @NotBlank private String path;
  @NotBlank private String responseIdSalt;
}
