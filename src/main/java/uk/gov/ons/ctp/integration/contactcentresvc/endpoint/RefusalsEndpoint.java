package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.AddressServiceImpl;

/** The REST controller for ContactCentreSvc Refusals end points */
@RestController
@RequestMapping(produces = "application/json")
public final class RefusalsEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(RefusalsEndpoint.class);

  private MapperFacade mapperFacade;

  /** Contructor for ContactCentreDataEndpoint */
  @Autowired
  public RefusalsEndpoint(
      final AddressServiceImpl addressservice,
      final MapperFacade mapperFacade) {
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET end point to retrieve refusal codes for case type
   * 
   * @param caseType the case type (optional)
   * @return the list of refusal codes
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/refusals", method = RequestMethod.GET)
  public ResponseEntity<List<RefusalDTO>> getRefusals(
      @RequestParam(value = "case-type", required = false) String caseType)
      throws CTPException {
    log.with("case-type", caseType).debug("Entering getRefusals");
    return ResponseEntity.ok(null);
  } 
}
