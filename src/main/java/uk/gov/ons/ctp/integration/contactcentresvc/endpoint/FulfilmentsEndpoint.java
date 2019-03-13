package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressServiceImpl;

/** The REST controller for ContactCentreSvc Fulfilments end points */
@RestController
@RequestMapping(produces = "application/json")
public final class FulfilmentsEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentsEndpoint.class);

  private MapperFacade mapperFacade;

  /** Contructor for ContactCentreDataEndpoint */
  @Autowired
  public FulfilmentsEndpoint(
      final AddressServiceImpl addressservice,
      final MapperFacade mapperFacade) {
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET end point to retrieve fulfilment ie product codes for case type and region
   * 
   * @param caseType the case type (optional)
   * @param region the region (optional)
   * @return the list of fulfilments
   * @throws CTPException something went wrong
   */
  public ResponseEntity<List<FulfilmentDTO>> getFulfilments(
      @RequestParam(value = "case-type", required = false) String caseType,
      @RequestParam(value = "region", required = false) String region)
      throws CTPException {
    log.with("case-type", caseType).with("region", region).debug("Entering getFulfilments");
    List<FulfilmentDTO> fulfilments = new ArrayList<FulfilmentDTO>();
    fulfilments.add(new FulfilmentDTO("ABC", "English Postal Fulfilment", "Post"));
    return ResponseEntity.ok(null);
  } 
}
