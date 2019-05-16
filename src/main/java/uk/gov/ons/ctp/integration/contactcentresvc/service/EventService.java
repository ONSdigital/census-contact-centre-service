package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.GenericEvent;

/** Service responsible for creation of events */
public interface EventService {

  /** Create and publish event */
  void createEvent(GenericEvent request) throws CTPException;
}
