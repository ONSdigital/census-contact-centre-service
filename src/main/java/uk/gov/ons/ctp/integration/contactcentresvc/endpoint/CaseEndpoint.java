package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.Constants;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.LaunchRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UACResponseDTO;
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
      @PathVariable("caseId") final UUID caseId, @Valid CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("pathParam", caseId)
        .with("requestParams", requestParamsDTO)
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
      @Valid CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("pathParam", uprn)
        .with("requestParams", requestParamsDTO)
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
      @PathVariable(value = "ref") final long ref, @Valid CaseQueryRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("pathParam", ref)
        .with("requestParams", requestParamsDTO)
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
    log.with("pathParam", caseId)
        .with("requestParams", requestParamsDTO)
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

    log.with("pathParam", caseId)
        .with("requestBody", requestBodyDTO)
        .info("Entering POST fulfilmentRequestByPost");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with(caseId)
          .warn(
              "The caseid in the URL does not match the caseid "
                  + "in the fulfilmentRequestByPost request body");
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

    log.with("pathParam", caseId)
        .with("requestBody", requestBodyDTO)
        .info("Entering POST fulfilmentRequestBySMS");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with(caseId)
          .warn(
              "The caseid in the URL does not match the caseid "
                  + "in the fulfilmentRequestBySMS request body");
      throw new CTPException(
          Fault.BAD_REQUEST, "The caseid in the URL does not match the caseid in the request body");
    }

    ResponseDTO response = caseService.fulfilmentRequestBySMS(requestBodyDTO);

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

    log.with("pathParam", caseId)
        .with("requestBody", requestBodyDTO)
        .info("Entering POST reportRefusal");

    UUID caseIdUUID = null;
    if (StringUtils.isBlank(caseId)) {
      log.with("caseId", caseId).warn("reportRefusal caseId must be a valid UUID or \"UNKNOWN\"");
      throw new CTPException(Fault.BAD_REQUEST, "caseId must be a valid UUID or \"UNKNOWN\"");
    } else if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with("caseId", caseId).warn("reportRefusal caseId in path and body must be identical");
      throw new CTPException(
          Fault.BAD_REQUEST, "reportRefusal caseId in path and body must be identical");
    } else if (!caseId.toUpperCase().equals("UNKNOWN")) {
      try {
        caseIdUUID = UUID.fromString(caseId);
      } catch (IllegalArgumentException e) {
        log.with("caseId", caseId).warn("reportRefusal caseId must be a valid UUID");
        throw new CTPException(Fault.BAD_REQUEST, "caseId must be a valid UUID");
      }
    }
    ResponseDTO response = caseService.reportRefusal(caseIdUUID, requestBodyDTO);

    log.with("caseId", caseId).debug("Exiting reportRefusal");

    return ResponseEntity.ok(response);
  }

  /**
   * the POST end point to invalidate an existing case due to address status change.
   *
   * @param caseId case ID
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/invalidate", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<ResponseDTO> invalidateCase(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody InvalidateCaseRequestDTO requestBodyDTO)
      throws CTPException {

    log.with("requestBody", requestBodyDTO).info("Entering POST invalidate");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with(caseId)
          .warn(
              "The caseid in the URL does not match the caseid "
                  + "in the invalidateCase request body");
      throw new CTPException(
          Fault.BAD_REQUEST, "The caseid in the URL does not match the caseid in the request body");
    }
    ResponseDTO response = caseService.invalidateCase(requestBodyDTO);
    return ResponseEntity.ok(response);
  }

  // ---------------------------------------------------------------
  // DUMMY ENDPOINTS FROM HERE
  // ---------------------------------------------------------------

  /**
   * DUMMY ENDPOINT FOR CC
   *
   * <p>the PUT end point to modify an existing case
   *
   * @param caseId case ID
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}", method = RequestMethod.PUT)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<CaseDTO> modifyCase(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody ModifyCaseRequestDTO requestBodyDTO)
      throws CTPException {
    log.with("requestBody", requestBodyDTO).info("Entering PUT modifyCase");
    return ResponseEntity.ok(new CaseDTO());
  }

  /**
   * DUMMY ENDPOINT FOR CC
   *
   * <p>the POST end point to create a new case
   *
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<CaseDTO> newCase(@Valid @RequestBody NewCaseRequestDTO requestBodyDTO)
      throws CTPException {

    log.with("requestBody", requestBodyDTO).info("Entering POST newCase");

    CaseDTO response = new CaseDTO();

    return ResponseEntity.ok(response);
  }

  /**
   * DUMMY ENDPOINT FOR CC
   *
   * <p>the GET end point to request a CCS case by postcode
   *
   * @param postcode postcode
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/ccs/postcode/{postcode}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<CaseDTO> getCCSCaseByPostcode(
      @PathVariable(value = "postcode") @NotBlank @Pattern(regexp = Constants.POSTCODE_RE)
          final String postcode)
      throws CTPException {

    log.with("pathParam", postcode).info("Entering GET getCCSCaseByPostcode");

    CaseDTO response = new CaseDTO();
    return ResponseEntity.ok(response);
  }

  /**
   * DUMMY ENDPOINT FOR AD
   *
   * <p>the GET end point to request a UAC from AD for a given caseid
   *
   * @param caseId the id of the case
   * @param requestBodyDTO the request body
   * @return response entity
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}/uac", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<UACResponseDTO> getUACForCase(
      @PathVariable(value = "caseId") final UUID caseId, @Valid UACRequestDTO requestBodyDTO)
      throws CTPException {

    log.with("pathParam", caseId)
        .with("requestBody", requestBodyDTO)
        .info("Entering GET getUACForCase");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      log.with(caseId)
          .warn(
              "The caseid in the URL does not match the caseid "
                  + "in the getUACForCase request body");
      throw new CTPException(
          Fault.BAD_REQUEST, "The caseid in the URL does not match the caseid in the request body");
    }

    UACResponseDTO response = new UACResponseDTO();

    return ResponseEntity.ok(response);
  }
}
