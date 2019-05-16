package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
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
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;

/**
 * This class is a stub which has a minimal implementation of the case service endpoints. The idea
 * is that it produces a response which is just enough to allow CC development to proceed.
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
  public ResponseEntity<CaseContainerDTO> findCaseById(
      @PathVariable("caseId") final UUID caseId,
      @RequestParam(value = "caseevents", required = false) boolean caseevents,
      @RequestParam(value = "iac", required = false) boolean iac)
      throws CTPException {
    log.with("case_id", caseId).debug("Entering findCaseById");

    EventDTO e1 = new EventDTO();
    e1.setId("101");
    e1.setDescription("Initial creation of case");
    e1.setCategory("CASE_CREATED");
    e1.setCreatedDateTime("2019-04-01T07:12:26Z");

    EventDTO e2 = new EventDTO();
    e2.setId("102");
    e2.setDescription("Create Household Visit");
    e2.setCategory("ACTION_CREATED");
    e2.setCreatedDateTime("2019-04-14T12:45:26Z");

    List<EventDTO> caseEvents = new ArrayList<>();
    caseEvents.add(e1);
    caseEvents.add(e2);

    CaseContainerDTO caseDetails = new CaseContainerDTO();
    caseDetails.setId(caseId);
    caseDetails.setArid("2344266233");
    caseDetails.setEstabArid("AABBCC");
    caseDetails.setEstabType("ET");
    caseDetails.setUprn("1235532324343434");
    caseDetails.setCaseRef("x1233");
    caseDetails.setCaseType("H");
    caseDetails.setCreatedDateTime("2019-04-14T12:45:26Z");
    caseDetails.setAddressLine1("Napier House");
    caseDetails.setAddressLine2("11 Park Street");
    caseDetails.setAddressLine3("Parkhead");
    caseDetails.setTownName("Glasgow");
    caseDetails.setPostcode("G1 2AA");
    caseDetails.setOrganisationName("ON");
    caseDetails.setAddressLevel("E");
    caseDetails.setAbpCode("AACC");
    caseDetails.setRegion("E");
    caseDetails.setLatitude("41.40338");
    caseDetails.setLongitude("2.17403");
    caseDetails.setOa("EE22");
    caseDetails.setLsoa("x1");
    caseDetails.setMsoa("x2");
    caseDetails.setLad("H1");
    caseDetails.setCaseEvents(caseEvents);

    return ResponseEntity.ok(caseDetails);
  }
}
