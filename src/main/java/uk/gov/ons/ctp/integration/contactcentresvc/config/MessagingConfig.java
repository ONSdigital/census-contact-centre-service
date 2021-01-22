package uk.gov.ons.ctp.integration.contactcentresvc.config;

import lombok.Data;

@Data
public class MessagingConfig {
  private PublishConfig publish;

  @Data
  public static class PublishConfig {
    private int maxAttempts;
  }
}
