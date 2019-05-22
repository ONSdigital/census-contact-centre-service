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
    log.debug("getCaseById. Calling Case Service to find case details by ID: " + caseId);

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByIdQueryPath();
    CaseContainerDTO caseDetails =
        caseServiceClient.getResource(
            path, CaseContainerDTO.class, null, queryParams, caseId.toString());
    log.debug("getCaseById. Found details for case: " + caseId);

    return caseDetails;
  }

  public List<CaseContainerDTO> getCaseByUprn(Long uprn, Boolean listCaseEvents) {
    log.debug("getCaseByUprn. Calling Case Service to find case details by Uprn: " + uprn);

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByUprnQueryPath();
    List<CaseContainerDTO> cases =
        caseServiceClient.getResources(
            path, CaseContainerDTO[].class, null, queryParams, Long.toString(uprn));

    log.debug("getCaseByUprn. Found details for Uprn" + uprn);

    return cases;
  }

  public CaseContainerDTO getCaseByCaseRef(Long caseReference, Boolean listCaseEvents) {
    log.debug(
        "getCaseByCaseReference. Calling Case Service to find case details by case reference: "
            + caseReference);

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseEvents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByCaseReferenceQueryPath();
    CaseContainerDTO caseDetails =
        caseServiceClient.getResource(
            path, CaseContainerDTO.class, null, queryParams, caseReference);

    log.debug("getCaseByCaseReference. Found details for case reference: " + caseReference);

    return caseDetails;
  }
}
