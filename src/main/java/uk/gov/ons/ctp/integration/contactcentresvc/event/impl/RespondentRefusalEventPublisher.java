package uk.gov.ons.ctp.integration.contactcentresvc.event.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.event.ContactCentreEventPublisher;

/** Implementation for publication of case refusal events. */
@MessageEndpoint
public class RespondentRefusalEventPublisher implements ContactCentreEventPublisher {

  @Qualifier("respondentRefusalRabbitTemplate")
  @Autowired
  private RabbitTemplate respondentRefusalRabbitTemplate;

  /** Send Event */
  @Override
  public void sendEvent(GenericEvent event) {
    respondentRefusalRabbitTemplate.convertAndSend(event);
  }
}
