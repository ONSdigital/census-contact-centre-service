package uk.gov.ons.ctp.integration.contactcentresvc.event.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.event.ContactCentreEventPublisher;

/**
 * Implementation for publication of contact centre asynchronous requests to the Response Management
 * System.
 */
@MessageEndpoint
public class ContactCentreEventPublisherImpl implements ContactCentreEventPublisher {

  @Qualifier("fulfilmentRequestRabbitTemplate")
  @Autowired
  private RabbitTemplate fulfilmentRequestRabbitTemplate;

  /** Send Event */
  @Override
  public void sendEvent(GenericEvent event) {
    fulfilmentRequestRabbitTemplate.convertAndSend(event);
  }
}
