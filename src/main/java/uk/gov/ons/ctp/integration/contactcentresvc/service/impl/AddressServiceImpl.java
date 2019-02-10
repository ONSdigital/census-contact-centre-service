package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

/**
 * A ContactCentreDataService implementation which encapsulates all business logic for getting
 * Addresses
 */
@Service
public class AddressServiceImpl implements AddressService {
  private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);
}
