package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AppointmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSUnresolvedFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.editor.UniquePropertyReferenceNumberEditor;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** The REST controller for ContactCentreSvc find cases end points */
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  private CaseService caseService;

  /**
   * Constructor for ContactCentreDataEndpoint
   *
   * @param caseService is a service layer object that we be doing the processing on behalf of this
   *     endpoint.
   */
  @Autowired
  public CaseEndpoint(final CaseService caseService) {
    this.caseService = caseService;
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(
        UniquePropertyReferenceNumber.class, new UniquePropertyReferenceNumberEditor());
  }

  /**
   * the GET end point to get a Case by caseId
   *
   * @param caseId the id of the case
   * @param requestParamsDTO contains request params
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseById(
      @PathVariable("caseId") final UUID caseId, @Valid CaseRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("case_id", caseId)
        .with("caseEvents", requestParamsDTO.getCaseEvents())
        .info("Entering GET getCaseById");

    CaseDTO result = caseService.getCaseById(caseId, requestParamsDTO);

    return ResponseEntity.ok(result);
  }

  /**
   * the GET end point to get a Case by UPRN
   *
   * @param uprn the UPRN
   * @param requestParamsDTO contains request params
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/uprn/{uprn}", method = RequestMethod.GET)
  public ResponseEntity<List<CaseDTO>> getCaseByUPRN(
      @PathVariable(value = "uprn") final UniquePropertyReferenceNumber uprn,
      @Valid CaseRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("uprn", uprn)
        .with("caseEvents", requestParamsDTO.getCaseEvents())
        .info("Entering GET getCaseByUPRN");

    List<CaseDTO> results = caseService.getCaseByUPRN(uprn, requestParamsDTO);

    return ResponseEntity.ok(results);
  }

  /**
   * the GET end point to get a Case by Case Ref
   *
   * @param ref the CaseRef
   * @param requestParamsDTO contains request params
   * @return the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/ref/{ref}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseByCaseReference(
      @PathVariable(value = "ref") final long ref, @Valid CaseRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("ref", ref)
        .with("caseEvents", requestParamsDTO.getCaseEvents())
        .info("Entering GET getCaseByCaseReference");

    CaseDTO result = caseService.getCaseByCaseReference(ref, requestParamsDTO);

    return ResponseEntity.ok(result);
  }

  /**
   * the GET end point to get an EQ Launch URL for a case
   *
   * @param caseId the id of the case
   * @param requestParamsDTO contains request params
   * @return the URL to launch the questionnaire for the case
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/launch", method = RequestMethod.GET)
  public ResponseEntity<String> getLaunchURLForCaseId(
      @PathVariable(value = "caseId") final UUID caseId, @Valid LaunchRequestDTO requestParamsDTO)
      throws CTPException {
    // INFO because we need to log agent-id
    log.with("caseId", caseId)
        .with("agentId", requestParamsDTO.getAgentId())
        .info("Entering GET getLaunchURLForCaseId");

    String launchURL = caseService.getLaunchURLForCaseId(caseId, requestParamsDTO);

    return ResponseEntity.ok(launchURL);
  }

  /**
   * the POST end point to request a postal fulfilment for a case
   *
   * @param caseId the id of the case
   * @param requestBodyDTO contains request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/fulfilment/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentRequestByPost(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("caseId", caseId).info("Entering POST fulfilmentRequestByPost");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with(caseId).warn("The caseid in the URL does not match the caseid in the request body");
      throw new CTPException(
          Fault.BAD_REQUEST, "The caseid in the URL does not match the caseid in the request body");
    }

    ResponseDTO response = caseService.fulfilmentRequestByPost(requestBodyDTO);

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to request an SMS fulfilment for a case
   *
   * @param caseId the id of the case
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/fulfilment/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentRequestBySMS(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("caseId", caseId).info("Entering POST fulfilmentRequestBySMS");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with(caseId).warn("The caseid in the URL does not match the caseid in the request body");
      throw new CTPException(
          Fault.BAD_REQUEST, "The caseid in the URL does not match the caseid in the request body");
    }

    ResponseDTO response = caseService.fulfilmentRequestBySMS(requestBodyDTO);

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to request a postal fulfilment for an unresolved case
   *
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/unresolved/fulfilment/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentUnresolvedRequestByPost(
      @Valid @RequestBody PostalUnresolvedFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    log.info("Entering POST fulfilmentUnresolvedRequestByPost");

    ResponseDTO response = caseService.fulfilmentUnresolvedRequestByPost(requestBodyDTO);

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to request an SMS fulfilment for an unresolved case
   *
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/unresolved/fulfilment/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentUnresolvedRequestBySMS(
      @Valid @RequestBody SMSUnresolvedFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    log.info("Entering POST fulfilmentUnresolvedRequestBySMS");

    ResponseDTO response = caseService.fulfilmentUnresolvedRequestBySMS(requestBodyDTO);

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to request an appointment
   *
   * @param caseId is the case to schedule an appointment for.
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/appointment", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> makeAppointment(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody AppointmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("caseId", caseId).info("Entering POST makeAppointment");

    ResponseDTO response = caseService.makeAppointment(caseId, requestBodyDTO);

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to report a refusal - caseId may be "unknown"
   *
   * @param caseId is the case to log the refusal against. May contain 'unknown'.
   * @param requestBodyDTO the request body.
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/refusal", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> reportRefusal(
      @PathVariable(value = "caseId") final String caseId,
      @Valid @RequestBody RefusalRequestDTO requestBodyDTO)
      throws CTPException {

    log.with("caseId", caseId).info("Entering POST reportRefusal");

    UUID caseIdUUID = null;
    if (StringUtils.isBlank(caseId)) {
      throw new CTPException(Fault.BAD_REQUEST, "caseId must be a valid UUID or \"UNKNOWN\"");
    } else if (!caseId.equals(requestBodyDTO.getCaseId())) {
      throw new CTPException(Fault.BAD_REQUEST, "caseId in path and body must be identical");
    } else if (!caseId.toUpperCase().equals("UNKNOWN")) {
      try {
        caseIdUUID = UUID.fromString(caseId);
      } catch (IllegalArgumentException e) {
        throw new CTPException(Fault.BAD_REQUEST, "caseId must be a valid UUID");
      }
    }
    ResponseDTO response = caseService.reportRefusal(caseIdUUID, requestBodyDTO);

    log.with("caseId", caseId).debug("Exiting reportRefusal");

    return ResponseEntity.ok(response);
  }
}
