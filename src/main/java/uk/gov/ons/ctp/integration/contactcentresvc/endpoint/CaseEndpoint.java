package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AppointmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSUnresolvedFulfilmentRequestDTO;

/** The REST controller for ContactCentreSvc find cases end points */
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
//@Validated
public class CaseEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  @Autowired private EventService<PostalFulfilmentRequestDTO> eventSvc;

  private MapperFacade mapperFacade;

  /** Constructor for ContactCentreDataEndpoint */
  @Autowired
  public CaseEndpoint(final MapperFacade mapperFacade) {
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET end point to get a Case by caseId
   * 
   * @param caseId the id of the case
   * @param caseEvents true if case events to be returned with each case
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseById(@PathVariable("case-id") final UUID caseId,
      @RequestParam(value = "case-events", required = false) boolean caseEvents)
      throws CTPException {
    log.with("case_id", caseId).debug("Entering getCaseById");

    return ResponseEntity.ok(null);
  }

  /**
   * the GET end point to get a Case by UPRN
   * 
   * @param uprn the UPRN
   * @param caseEvents true if case events to be returned with each case
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/uprn/{uprn}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseByUPRN(
      @PathVariable(value = "uprn") final long uprn,
      @RequestParam(value = "case-events", required = false) boolean caseEvents)
      throws CTPException {
    log.with("uprn", uprn).debug("Entering getCaseByUPRN");

    return ResponseEntity.ok(null);
  } 
  
  /**
   * the GET end point to get a Case by Case Ref
   * 
   * @param ref the CaseRef
   * @param caseEvents true if case events to be returned with each case
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/ref/{ref}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseByCaseReference(
      @PathVariable(value = "ref") final long ref,
      @RequestParam(value = "case-events", required = false) boolean caseEvents)
      throws CTPException {
    log.with("ref", ref).debug("Entering getCaseByCaseReference");

    return ResponseEntity.ok(null);
  } 
  
  /**
   * the GET end point to get an EQ Launch URL for a case
   * 
   * @param caseId the id of the case
   * @param agentId the id of the call centre initiating the launch
   * @return the URL to launch the questionnaire for the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/launch", method = RequestMethod.GET)
  public ResponseEntity<String> getLaunchURLForCaseId(
      @PathVariable(value = "case-id") final UUID caseId,
      @RequestParam(value = "agent-id", required = true) @Pattern(regexp="\\d{1,5}") String agentId)
      throws CTPException {
    // INFO because we need to log agent-id
    log.with("case-id", caseId).with("agent-id", agentId).info("Entering getLaunchURLForCaseId");
    return ResponseEntity.ok(null);
  } 
  
  /**
   * the POST end point to request a postal fulfilment for a case
   * 
   * @param caseId the id of the case
   * @param requestDTO the request data
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/fulfilment/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO>  fulfilmentRequestByPost(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody PostalFulfilmentRequestDTO requestDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering makeFulfilmentRequestByPost");
    eventSvc.createEvent(requestDTO);
    return ResponseEntity.ok(null);
  }

  /**
   * the POST end point to request an SMS fulfilment for a case
   * 
   * @param caseId the id of the case
   * @param requestDTO the request data
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/fulfilment/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO>  fulfilmentRequestBySMS(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody SMSFulfilmentRequestDTO requestDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering fulfilmentRequestBySMS");
    return ResponseEntity.ok(null);
  }
  /**
   * the POST end point to request a postal fulfilment for an unresolved case
   * 
   * @param requestDTO the request data
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/unresolved/fulfilment/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO>  fulfilmentUnresolvedRequestByPost(
      @Valid @RequestBody PostalUnresolvedFulfilmentRequestDTO requestDTO)
      throws CTPException {
    log.debug("Entering fulfilmentUnresolvedRequestByPost");
    return ResponseEntity.ok(null);
  }

  /**
   * the POST end point to request an SMS fulfilment for an unresolved case
   * 
   * @param requestDTO the request data
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/unresolved/fulfilment/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO>  fulfilmentUnresolvedRequestBySMS(
      @Valid @RequestBody SMSUnresolvedFulfilmentRequestDTO requestDTO)
      throws CTPException {
    log.debug("Entering fulfilmentUnresolvedRequestBySMS");
    return ResponseEntity.ok(null);
  }

  /**
   * the POST end point to request an appointment
   * 
   * @param requestDTO the request data
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/appointment", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<UUID>  makeAppointment(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody AppointmentRequestDTO requestDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering makeAppointment");
    return ResponseEntity.ok(null);
  }

  /**
   * the POST end point to report a refusal
   * 
   * @param requestDTO the request data
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/appointment", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO>  reportRefusal(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody RefusalRequestDTO requestDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering makeAppointment");
    return ResponseEntity.ok(null);
  }
}
