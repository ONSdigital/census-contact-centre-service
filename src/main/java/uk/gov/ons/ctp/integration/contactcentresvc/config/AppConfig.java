package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
  private ReportSettings reportSettings;
  // private Rabbitmq rabbitmq;
  private AddressIndexSettings addressIndexSettings;
  private Logging logging;
}
