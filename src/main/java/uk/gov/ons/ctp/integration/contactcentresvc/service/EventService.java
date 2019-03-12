package uk.gov.ons.ctp.integration.contactcentresvc.service;

import uk.gov.ons.ctp.common.error.CTPException;

/** Service responsible for creation of events */
public interface EventService<T> {

  /** Create and publish event */
  void createEvent(T request) throws CTPException;
}
