package uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

/** This class is responsible for communications with the Case Service. */
@Service
@Validated
public class CaseServiceClientServiceImpl {
  private static final Logger log = LoggerFactory.getLogger(CaseServiceClientServiceImpl.class);

  @Autowired private AppConfig appConfig;

  @Inject
  @Qualifier("caseServiceClient")
  private RestClient caseServiceClient;

  public CaseContainerDTO getCaseById(UUID caseId, Boolean listCaseEvents) {
    log.with("caseId", caseId)
        .debug("getCaseById() calling Case Service to find case details by ID");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByIdQueryPath();
    CaseContainerDTO caseDetails =
        caseServiceClient.getResource(
            path, CaseContainerDTO.class, null, queryParams, caseId.toString());
    log.with("caseId", caseId).debug("getCaseById() found case details for case ID");

    return caseDetails;
  }

  public List<CaseContainerDTO> getCaseByUprn(Long uprn, Boolean listCaseEvents) {
    log.with("uprn", uprn)
        .debug("getCaseByUprn() calling Case Service to find case details by Uprn");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByUprnQueryPath();
    List<CaseContainerDTO> cases =
        caseServiceClient.getResources(
            path, CaseContainerDTO[].class, null, queryParams, Long.toString(uprn));

    log.with("uprn", uprn).debug("getCaseByUprn() found case details by Uprn");

    return cases;
  }

  public CaseContainerDTO getCaseByCaseRef(Long caseReference, Boolean listCaseEvents) {
    log.with("caseReference", caseReference)
        .debug(
            "getCaseByCaseReference() calling Case Service to find case details by case reference");

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByCaseReferenceQueryPath();
    CaseContainerDTO caseDetails =
        caseServiceClient.getResource(
            path, CaseContainerDTO.class, null, queryParams, caseReference);

    log.with("caseReference", caseReference)
        .debug("getCaseByCaseReference() found case details by case reference");

    return caseDetails;
  }
}
