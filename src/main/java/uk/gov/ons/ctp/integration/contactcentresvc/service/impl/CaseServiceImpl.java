package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@Service
@Validated()
@Configuration
public class CaseServiceImpl implements CaseService {
  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private AppConfig appConfig;

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  private MapperFacade caseDTOMapper = new CCSvcBeanMapper();

  @Value("${spring.security.user.name}")
  String whitelistedEventNames;

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by caseId: {}", caseId);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = caseServiceClient.getCaseById(caseId, getCaseEvents);

    // Only return Household cases
    if (!caseIsHouseholdOrCommunal(caseDetails.getCaseType())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Case is a not a household or communal case");
    }

    // Convert from Case service to Contact Centre DTOs
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);

    filterCaseEvents(caseServiceResponse, getCaseEvents);

    log.debug("Returning case details for caseId: {}", caseId);

    return caseServiceResponse;
  }

  @Override
  public List<CaseDTO> getCaseByUPRN(
      UniquePropertyReferenceNumber uprn, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by UPRN: {}", uprn);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    List<CaseContainerDTO> caseDetails =
        caseServiceClient.getCaseByUprn(uprn.getValue(), getCaseEvents);

    // Only return Household cases
    List<CaseContainerDTO> householdCases =
        caseDetails
            .parallelStream()
            .filter(c -> caseIsHouseholdOrCommunal(c.getCaseType()))
            .collect(Collectors.toList());

    // Convert from Case service to Contact Centre DTOs
    List<CaseDTO> caseServiceResponse = caseDTOMapper.mapAsList(householdCases, CaseDTO.class);

    // Clean up the events before returning them
    caseServiceResponse.stream().forEach(c -> filterCaseEvents(c, getCaseEvents));

    log.debug(
        "Returning case details for UPRN: {}. Result set size: {}",
        uprn,
        caseServiceResponse.size());

    return caseServiceResponse;
  }

  @Override
  public CaseDTO getCaseByCaseReference(final long caseRef, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by case reference: {}", caseRef);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = caseServiceClient.getCaseByCaseRef(caseRef, getCaseEvents);

    // Only return Household cases
    if (!caseIsHouseholdOrCommunal(caseDetails.getCaseType())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Case is a not a household or communal case");
    }

    // Convert from Case service to Contact Centre DTOs
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);

    filterCaseEvents(caseServiceResponse, getCaseEvents);

    log.debug("Returning case details for case reference: {}", caseRef);

    return caseServiceResponse;
  }

  private void filterCaseEvents(CaseDTO caseDTO, Boolean getCaseEvents) {
    if (getCaseEvents) {
      // Only return whitelisted events
      Set<String> whitelistedEventCategories =
          appConfig.getCaseServiceSettings().getWhitelistedEventCategories();
      List<CaseEventDTO> filteredEvents =
          caseDTO
              .getCaseEvents()
              .stream()
              .filter(e -> whitelistedEventCategories.contains(e.getCategory()))
              .collect(Collectors.toList());
      caseDTO.setCaseEvents(filteredEvents);
    } else {
      // Caller doesn't want any event data
      caseDTO.setCaseEvents(null);
    }
  }

  private boolean caseIsHouseholdOrCommunal(String caseTypeString) {
    return caseTypeString.equals(CaseType.HH.name()) || caseTypeString.equals(CaseType.CE.name());
  }
}
