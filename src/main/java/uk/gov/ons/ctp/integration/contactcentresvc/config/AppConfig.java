package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
  private ReportSettings reportSettings;
  // private Rabbitmq rabbitmq;
  private AddressIndexSettings addressIndexSettings;
  private CaseServiceSettings caseServiceSettings;
  private KeyStore keystore;
  private EqConfig eq;
  private Logging logging;
  private String mode;
}
