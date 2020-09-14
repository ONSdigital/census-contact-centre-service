package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.EnableRetry;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {

  private String surveyName;
  private String collectionExerciseId;
  private AddressIndexSettings addressIndexSettings;
  private CaseServiceSettings caseServiceSettings;
  private KeyStore keystore;
  private EqConfig eq;
  private Logging logging;
  private Channel channel;
  private Resource publicPgpKey1;
  private Resource publicPgpKey2;

  public void setChannel(Channel channel) {
    if (channel.equals(Channel.CC) || channel.equals(Channel.AD)) {
      this.channel = channel;
    } else {
      throw new IllegalArgumentException("Channel can only be one of CC or AD");
    }
  }
}
