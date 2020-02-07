package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostcodeQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.VersionResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

/** The REST endpoint controller for ContactCentreSvc Details */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class AddressEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(AddressEndpoint.class);

  private AddressService addressService;

  /**
   * Constructor for ContactCentreDataEndpoint
   *
   * @param addressService is the object that this endpoint can call for address and postcode
   *     searches.
   */
  @Autowired
  public AddressEndpoint(final AddressService addressService) {
    this.addressService = addressService;
  }

  /**
   * This GET endpoint returns the addresses for an address search. If no matches are found then it
   * returns with 0 addresses, otherwise it returns with 1 or more addresses.
   *
   * @param addressQueryRequest is a DTO specify details on the address to search for.
   * @return an object listing the addresses matching the address search string.
   */
  @RequestMapping(value = "/addresses", method = RequestMethod.GET)
  public AddressQueryResponseDTO getAddressesBySearchQuery(
      @Valid AddressQueryRequestDTO addressQueryRequest) {
    log.with("requestParams", addressQueryRequest).info("Entering GET getAddressesBySearchQuery");
    return addressService.addressQuery(addressQueryRequest);
  }

  /**
   * This GET endpoint returns the addresses for the specified postcode.
   *
   * @param postcodeQueryRequest is a DTO specify details on the postcode to search for.
   * @return an object listing the addresses for the postcode.
   */
  @RequestMapping(value = "/addresses/postcode", method = RequestMethod.GET)
  public AddressQueryResponseDTO getAddressesByPostcode(
      @Valid PostcodeQueryRequestDTO postcodeQueryRequest) {
    log.with("requestParams", postcodeQueryRequest).info("Entering GET getAddressesByPostcode");
    return addressService.postcodeQuery(postcodeQueryRequest);
  }

  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   */
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public VersionResponseDTO getVersion() {
    log.info("Entering GET getVersion");
    VersionResponseDTO fakeVersion =
        VersionResponseDTO.builder().apiVersion("4.0.2").dataVersion("60").build();
    return fakeVersion;
  }
}
