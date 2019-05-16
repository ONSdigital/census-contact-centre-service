package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseDetailsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CategoryDTO.CategoryName;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;

/**
 * This class is a stub which has a minimal implementation of the case service endpoints. 
 * The idea is that it produces a response which is just enough to allow CC development to proceed.
 */
@RestController
@RequestMapping(value = "/cases-rm-fake", produces = "application/json")
public final class CaseServiceFakeStub implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  @RequestMapping(value = "/info", method = RequestMethod.GET)
  public ResponseEntity<String> info() throws CTPException {

    return ResponseEntity.ok("pmb");
  }

  @RequestMapping(value = "/uprn/{uprn}", method = RequestMethod.GET)
  public ResponseEntity<List<CaseDTO>> getCaseByUPRN(
      @PathVariable(value = "uprn") final UniquePropertyReferenceNumber uprn,
      @Valid CaseRequestDTO requestParamsDTO)
      throws CTPException {
    log.with("uprn", uprn)
        .with("caseEvents", requestParamsDTO.getCaseEvents())
        .debug("Entering getCaseByUPRN");

    List<CaseDTO> results = null;

    return ResponseEntity.ok(results);
  }

  /**
   * the GET endpoint to find a Case by UUID
   *
   * @param caseId to find by
   * @param caseevents flag used to return or not CaseEvents
   * @param iac flag used to return or not the iac
   * @return the case found
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{caseId}", method = RequestMethod.GET)
  public ResponseEntity<CaseDetailsDTO> findCaseById(
      @PathVariable("caseId") final UUID caseId,
      @RequestParam(value = "caseevents", required = false) boolean caseevents,
      @RequestParam(value = "iac", required = false) boolean iac)
      throws CTPException {
    log.with("case_id", caseId).debug("Entering findCaseById");

    ResponseDTO r1 = new ResponseDTO();
    r1.setInboundChannel("ONLINE");
    r1.setDateTime(new Date());

    List<ResponseDTO> responses = new ArrayList<>();
    responses.add(r1);

    CaseEventDTO e1 = new CaseEventDTO();
    e1.setDescription("Initial creation of case");
    e1.setCategory(CategoryName.CASE_CREATED);
    e1.setCreatedDateTime(new Date());

    CaseEventDTO e2 = new CaseEventDTO();
    e2.setDescription("Create Household Visit");
    e2.setCategory(CategoryName.ACTION_CREATED);
    e2.setCreatedDateTime(new Date());

    List<CaseEventDTO> caseEvents = new ArrayList<>();
    caseEvents.add(e1);
    caseEvents.add(e2);

    CaseDetailsDTO caseDetails = new CaseDetailsDTO();
    caseDetails.setId(UUID.randomUUID());
    caseDetails.setCaseRef("1000000000000001");
    caseDetails.setSampleUnitType("H");
    caseDetails.setCreatedDateTime(new Date());
    caseDetails.setResponses(responses);
    caseDetails.setCaseEvents(caseEvents);

    return ResponseEntity.ok(caseDetails);
  }
}
