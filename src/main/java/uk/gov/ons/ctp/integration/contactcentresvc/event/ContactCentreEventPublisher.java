package uk.gov.ons.ctp.integration.contactcentresvc.event;

import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;

/**
 * Service responsible for the publication of contact centre requests to the Response Management
 * System.
 */
public interface ContactCentreEventPublisher {

  /**
   * Publishes an fulfilment event to the outbound message exchange.
   *
   * @param fulfilmentRequestedEvent contains details on the fulfilment.
   */
  void sendFulfilmentEvent(FulfilmentRequestedEvent fulfilmentRequestedEvent);

  /**
   * Publishes an refusal event to the outbound message queue.
   *
   * @param respondentRefusalEvent contains details on the refusal.
   */
  void sendRefusalEvent(RespondentRefusalEvent respondentRefusalEvent);
}
