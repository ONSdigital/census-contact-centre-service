package uk.gov.ons.ctp.integration.contactcentresvc.event.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequestedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentRefusalEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.event.ContactCentreEventPublisher;

/** Implementation for publication of events. */
@MessageEndpoint
public class ContactCentreEventPublisherImpl implements ContactCentreEventPublisher {

  private static final String ROUTING_KEY_FULFILMENT = "event.fulfilment.request";
  private static final String ROUTING_KEY_REFUSAL = "event.respondent.refusal";

  @Autowired private RabbitTemplate outboundRabbitTemplate;

  @Override
  public void sendFulfilmentEvent(FulfilmentRequestedEvent fulfilmentRequestedEvent) {
    outboundRabbitTemplate.convertAndSend(ROUTING_KEY_FULFILMENT, fulfilmentRequestedEvent);
  }

  @Override
  public void sendRefusalEvent(RespondentRefusalEvent respondentRefusalEvent) {
    outboundRabbitTemplate.convertAndSend(ROUTING_KEY_REFUSAL, respondentRefusalEvent);
  }
}
