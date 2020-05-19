package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.VersionResponseDTO;

/** The REST endpoint controller for ContactCentreSvc Version Details */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class VersionEndpoint implements CTPEndpoint {
  public static final String CC_API_VERSION = "5.10.0";
  private static final Logger log = LoggerFactory.getLogger(VersionEndpoint.class);

  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   */
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public VersionResponseDTO getVersion() {
    log.info("Entering GET getVersion");
    VersionResponseDTO fakeVersion =
        VersionResponseDTO.builder().apiVersion(CC_API_VERSION).build();
    return fakeVersion;
  }
}
