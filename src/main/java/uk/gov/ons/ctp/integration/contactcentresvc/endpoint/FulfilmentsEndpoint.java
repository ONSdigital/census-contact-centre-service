package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressServiceImpl;

/** The REST endpoint controller for ContactCentreSvc Fulfilments endpoints */
@RestController
@RequestMapping(produces = "application/json")
public final class FulfilmentsEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentsEndpoint.class);

  private MapperFacade mapperFacade;

  /** Contructor for ContactCentreDataEndpoint */
  @Autowired
  public FulfilmentsEndpoint(
      final AddressServiceImpl addressservice,
      final @Qualifier("CCSvcBeanMapper") MapperFacade mapperFacade) {
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/fulfilments", method = RequestMethod.GET)
  public ResponseEntity<List<FulfilmentDTO>> getFulfilments() {
    List<FulfilmentDTO> fulfilments = new ArrayList<FulfilmentDTO>();
    fulfilments.add(new FulfilmentDTO("ABC", "English Postal Fulfilment", "Post"));

    return ResponseEntity.ok(fulfilments);
  }
}
