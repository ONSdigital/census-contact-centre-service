package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressServiceImpl;

/** The REST endpoint controller for ContactCentreSvc Details */
@RestController
@RequestMapping(value = "/contactcentre", produces = "application/json")
public final class AddressEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(AddressEndpoint.class);

  public static final String CATEGORY_ACCESS_CODE_AUTHENTICATION_ATTEMPT_NOT_FOUND =
      "Category ACCESS_CODE_AUTHENTICATION_ATTEMPT does not exist";

  private AddressServiceImpl addressService;
  private MapperFacade mapperFacade;

  /** Contructor for ContactCentreDataEndpoint */
  @Autowired
  public AddressEndpoint(
      final AddressServiceImpl addressservice,
      final @Qualifier("RHSvcBeanMapper") MapperFacade mapperFacade) {
    this.addressService = addressservice;
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/data", method = RequestMethod.GET)
  public String getContactCentreData() {
    String helloTeam = "Hello Census Integration Contact Centre!";

    return helloTeam;
  }
}
