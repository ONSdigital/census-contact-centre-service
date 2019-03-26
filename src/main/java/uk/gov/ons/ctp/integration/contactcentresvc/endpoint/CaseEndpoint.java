package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.Valid;
import ma.glasnost.orika.MapperFacade;
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
import uk.gov.ons.ctp.integration.contactcentresvc.service.EventService;

/** The REST controller for ContactCentreSvc find cases end points */
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  @Autowired private EventService eventSvcImpl;

  private CaseService caseService;

  private MapperFacade mapperFacade;

  /** Constructor for ContactCentreDataEndpoint */
  @Autowired
  public CaseEndpoint(final CaseService caseService, final MapperFacade mapperFacade) {
    this.caseService = caseService;
    this.mapperFacade = mapperFacade;
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
  @RequestMapping(value = "/{case-id}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseById(
      @PathVariable("case-id") final UUID caseId, @Valid CaseRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("case_id", caseId)
        .with("caseEvents", requestParamsDTO.getCaseEvents())
        .debug("Entering getCaseById");

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
  public ResponseEntity<CaseDTO> getCaseByUPRN(
      @PathVariable(value = "uprn") final UniquePropertyReferenceNumber uprn,
      @Valid CaseRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("uprn", uprn)
        .with("caseEvents", requestParamsDTO.getCaseEvents())
        .debug("Entering getCaseByUPRN");

    CaseDTO result = caseService.getCaseByUPRN(uprn, requestParamsDTO);

    return ResponseEntity.ok(result);
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
        .debug("Entering getCaseByCaseReference");

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
  @RequestMapping(value = "/{case-id}/launch", method = RequestMethod.GET)
  public ResponseEntity<String> getLaunchURLForCaseId(
      @PathVariable(value = "case-id") final UUID caseId, @Valid LaunchRequestDTO requestParamsDTO)
      throws CTPException {
    // INFO because we need to log agent-id
    log.with("case-id", caseId)
        .with("agent-id", requestParamsDTO.getAgentId())
        .info("Entering getLaunchURLForCaseId");

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
  @RequestMapping(value = "/{case-id}/fulfilment/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentRequestByPost(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering makeFulfilmentRequestByPost");

    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return ResponseEntity.ok(fakeResponse);
  }

  /**
   * the POST end point to request an SMS fulfilment for a case
   *
   * @param caseId the id of the case
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/fulfilment/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> fulfilmentRequestBySMS(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering fulfilmentRequestBySMS");

    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return ResponseEntity.ok(fakeResponse);
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
    log.debug("Entering fulfilmentUnresolvedRequestByPost");

    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return ResponseEntity.ok(fakeResponse);
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
    log.debug("Entering fulfilmentUnresolvedRequestBySMS");

    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return ResponseEntity.ok(fakeResponse);
  }

  /**
   * the POST end point to request an appointment
   *
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/appointment", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<UUID> makeAppointment(
      @PathVariable(value = "case-id") final UUID caseId,
      @Valid @RequestBody AppointmentRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("case-id", caseId).debug("Entering makeAppointment");

    UUID fakeUUIDResult = createSemiRandomFakeUUID();

    return ResponseEntity.ok(fakeUUIDResult);
  }

  /**
   * the POST end point to report a refusal - caseId may be "unknown"
   *
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{case-id}/refusal", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> reportRefusal(
      @PathVariable(value = "case-id") final String caseId,
      @Valid @RequestBody RefusalRequestDTO requestBodyDTO)
      throws CTPException {

    if (caseId != null
        && requestBodyDTO.getCaseId() != null
        && !caseId.equals(requestBodyDTO.getCaseId())) {
      throw new CTPException(Fault.BAD_REQUEST, "caseId in path and body must be identical");
    }
    UUID caseIdUUID = null;
    if (StringUtils.isBlank(caseId)) {
      throw new CTPException(Fault.BAD_REQUEST, "caseId must be a valid UUID or \"UNKNOWN\"");
    } else if (!caseId.toUpperCase().equals("UNKNOWN")) {
      try {
        caseIdUUID = UUID.fromString(caseId);
      } catch (IllegalArgumentException e) {
        throw new CTPException(Fault.BAD_REQUEST, "caseId must be a valid UUID");
      }
    }
    // caseIdUUID to be used as caseId from here on in - may be null if was "UNKNOWN"

    // TODO Region validation

    log.with("case-id", caseId).debug("Entering makeAppointment");
    
    ResponseDTO fakeResponse =
        ResponseDTO.builder()
            .id(createSemiRandomFakeUUID().toString())
            .dateTime(LocalDateTime.now().toString())
            .build();

    return ResponseEntity.ok(fakeResponse);
  }

  private UUID createSemiRandomFakeUUID() {
    String randomUUID = UUID.randomUUID().toString();
    String firstPart = randomUUID.substring(0, 9);
    String lastPart = randomUUID.substring(23);
    String semiRandomUUID = firstPart + "aaaa-bbbb-cccc" + lastPart;

    return UUID.fromString(semiRandomUUID);
  }
}
