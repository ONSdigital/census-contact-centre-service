package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.validation.Valid;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressServiceImpl;

/** The REST endpoint controller for ContactCentreSvc Details */
@RestController
@RequestMapping(value = "/contactcentre", produces = "application/json")
public final class AddressEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(AddressEndpoint.class);

  private AddressServiceImpl addressService;
  private MapperFacade mapperFacade;

  /** Constructor for ContactCentreDataEndpoint */
  @Autowired
  public AddressEndpoint(
      final AddressServiceImpl addressservice,
      final @Qualifier("CCSvcBeanMapper") MapperFacade mapperFacade) {
    this.addressService = addressservice;
    this.mapperFacade = mapperFacade;
  }

  /**
   * This GET endpoint returns the addresses for an address search. Depending on the success of the
   * search it will return 0, 1 or more addresses.
   *
   * @return an object listing the addresses matching the address search string.
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/addresses", method = RequestMethod.GET)
  public AddressQueryResponseDTO getAddressesBySearchQuery(
      @Valid AddressQueryRequestDTO addressQueryRequest) {
    return addressService.addressQuery(addressQueryRequest);
  }

  /**
   * This GET endpoint returns the addresses for the specified postcode.
   *
   * @return an object listing the addresses for the postcode.
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/addresses/postcode", method = RequestMethod.GET)
  public AddressQueryResponseDTO getAddressesByPostcode(
      @Valid PostcodeQueryRequestDTO postcodeQueryRequest) {
    return addressService.postcodeQuery(postcodeQueryRequest);
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
