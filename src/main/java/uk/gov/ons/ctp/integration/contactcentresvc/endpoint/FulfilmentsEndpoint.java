package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentsRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.FulfilmentsService;

/** The REST controller for ContactCentreSvc Fulfilments end points */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class FulfilmentsEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentsEndpoint.class);

  private FulfilmentsService fulfilmentsService;

  /**
   * Constructor for ContactCentre Fulfilment endpoint
   *
   * @param fulfilmentsService is a service layer object that will do processing on behalf of this
   *     endpoint.
   */
  @Autowired
  public FulfilmentsEndpoint(final FulfilmentsService fulfilmentsService) {
    this.fulfilmentsService = fulfilmentsService;
  }

  /**
   * the GET end point to retrieve fulfilment ie product codes for case type and region
   *
   * @param requestDTO holds the case type and region, to be used in the search of available
   *     fulfilments.
   * @return the list of fulfilments
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/fulfilments", method = RequestMethod.GET)
  public ResponseEntity<List<FulfilmentDTO>> getFulfilments(@Valid FulfilmentsRequestDTO requestDTO)
      throws CTPException {
    log.with("requestParams", requestDTO).info("Entering GET getFulfilments");
    List<FulfilmentDTO> fulfilments =
        fulfilmentsService.getFulfilments(
            requestDTO.getCaseType(),
            requestDTO.getRegion(),
            requestDTO.getDeliveryChannel(),
            requestDTO.getIndividual(),
            requestDTO.getProductGroup());
    return ResponseEntity.ok(fulfilments);
  }
}
