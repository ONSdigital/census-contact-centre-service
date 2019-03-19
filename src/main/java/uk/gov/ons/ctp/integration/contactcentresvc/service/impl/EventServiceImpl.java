package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.message.ContactCentreEventPublisher;
import uk.gov.ons.ctp.integration.contactcentresvc.service.EventService;

/** Implementation for creation of Event messages */
@Service
public class EventServiceImpl<T> implements EventService<T> {

  @Autowired private CustomObjectMapper objectMapper;

  @Autowired private ContactCentreEventPublisher publisher;

  /** Create and publish event */
  @Override
  public void createEvent(T request) throws CTPException {
    try {
      String message = objectMapper.writeValueAsString(request);
      publisher.sendEvent(message);
    } catch (JsonProcessingException ex) {
      throw new CTPException(CTPException.Fault.BAD_REQUEST, "JSON failed to parse");
    }
  }
}
