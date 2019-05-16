package uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.UUID;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseDetailsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

/** This class is responsible for communications with the Address Index service. */
@Service
@Validated
public class CaseServiceClientServiceImpl {
  private static final Logger log = LoggerFactory.getLogger(CaseServiceClientServiceImpl.class);

  @Autowired private AppConfig appConfig;

  @Inject
  @Qualifier("caseServiceClient")
  private RestClient caseServiceClient;

  public CaseDetailsDTO getCaseById(UUID caseId, Boolean listCaseEvents) {
    log.debug("getCaseById. Calling Case Service to find case details by ID: " + caseId);

    // Build map for query params
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("caseevents", Boolean.toString(listCaseEvents));

    // Ask Case Service to find case details
    String path = appConfig.getCaseServiceSettings().getCaseByIdQueryPath();
    CaseDetailsDTO caseDetails =
        caseServiceClient.getResource(
            path, CaseDetailsDTO.class, null, queryParams, caseId.toString());

    log.debug("getCaseById. Found details for case: " + caseId);

    return caseDetails;
  }
}
