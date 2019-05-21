package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.message.model.Event;

/** Service responsible for creation of events */
public interface EventService {

  /**
   * Create and publish event.
   *
   * @param request holds the data to be published.
   * @throws CTPException if something goes wrong.
   */
  void createEvent(Event request) throws CTPException;
}
