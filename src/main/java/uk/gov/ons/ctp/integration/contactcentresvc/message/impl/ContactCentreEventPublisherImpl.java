package uk.gov.ons.ctp.integration.contactcentresvc.message.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.integration.contactcentresvc.message.ContactCentreEventPublisher;

/**
 * Implementation for publication of contact centre asynchronous requests to the Response Management
 * System.
 */
@MessageEndpoint
public class ContactCentreEventPublisherImpl implements ContactCentreEventPublisher {

  @Autowired private RabbitTemplate rabbitTemplate;

  /** Send Event */
  @Override
  public void sendEvent(String event) {
    rabbitTemplate.convertAndSend(event);
  }
}
