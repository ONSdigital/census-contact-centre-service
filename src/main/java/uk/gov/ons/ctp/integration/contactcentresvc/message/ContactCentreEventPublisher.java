package uk.gov.ons.ctp.integration.contactcentresvc.message;

/**
 * Service responsible for the publication of contact centre requests to the Response Management
 * System.
 */
public interface ContactCentreEventPublisher {

  /**
   * Method to send event to Response Management
   *
   * @param event CaseEvent to publish.
   */
  void sendEvent(String event);
}
