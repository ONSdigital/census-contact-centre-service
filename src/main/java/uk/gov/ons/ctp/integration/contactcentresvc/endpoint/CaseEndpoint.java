package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.EventService;

/** The REST endpoint controller for Case as a resource. */
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint {

  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  @Autowired private EventService<PostalFulfilmentRequestDTO> eventSvc;

  /** Request a postal fulfilment request by Case UUID */
  @RequestMapping(
      value = "/{caseId}/fulfilment/post",
      method = RequestMethod.POST,
      consumes = "application/json")
  public ResponseEntity<String> caseFulfilmentPost(
      @PathVariable("caseId") final String caseId,
      @RequestBody final PostalFulfilmentRequestDTO postalFulfilmentRequestDTO)
      throws CTPException {
    log.debug("Entering caseFulfilmentPost");
    eventSvc.createEvent(postalFulfilmentRequestDTO);
    return new ResponseEntity<>("Request received", HttpStatus.OK);
  }
}
