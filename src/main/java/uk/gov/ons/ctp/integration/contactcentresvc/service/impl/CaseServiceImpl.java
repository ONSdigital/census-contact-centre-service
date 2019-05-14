package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseDetailsDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.utility.Constants;

@Service
@Validated()
public class CaseServiceImpl implements CaseService {
  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by caseId: {}", caseId);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseDetailsDTO caseDetails = caseServiceClient.getCaseById(caseId, getCaseEvents);

    // Only return Household cases
    boolean isHouseholdCase =
        caseDetails.getSampleUnitType().equals(Constants.CASE_SERVICE_UNIT_TYPE_HOUSEHOLD);
    if (!isHouseholdCase) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case is a non-household case");
    }

    CaseDTO caseServiceResponse =
        convertFromCaseServiceDTOToContactCentreDTO(caseDetails, getCaseEvents);

    log.debug("Returning case details for caseId: {}", caseId);

    return caseServiceResponse;
  }

  private CaseDTO convertFromCaseServiceDTOToContactCentreDTO(
      CaseDetailsDTO caseServiceDTO, boolean getCaseEvents) {
    // Convert Case Service response DTO objects to the Contact Centre equivalent DTO
    List<CaseResponseDTO> responses = null;
    List<ResponseDTO> responsesFromCaseService = caseServiceDTO.getResponses();
    if (responsesFromCaseService != null) {
      responses =
          responsesFromCaseService
              .stream()
              .map(
                  originalResponse -> {
                    CaseResponseDTO convertedResponse = new CaseResponseDTO();
                    convertedResponse.setInboundChannel(originalResponse.getInboundChannel());
                    convertedResponse.setDateTime(
                        DateTimeUtil.formatDate(originalResponse.getDateTime()));
                    return convertedResponse;
                  })
              .collect(Collectors.toList());
    }

    // Convert Case Service event DTO objects to the Contact Centre equivalent DTO
    List<CaseEventDTO> events = null;
    List<uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseEventDTO>
        caseEventsFromCaseService = caseServiceDTO.getCaseEvents();
    if (getCaseEvents && caseEventsFromCaseService != null) {
      events =
          caseEventsFromCaseService
              .stream()
              .map(
                  originalEvent -> {
                    CaseEventDTO convertedEvent = new CaseEventDTO();
                    convertedEvent.setDescription(originalEvent.getDescription());
                    convertedEvent.setCategory(originalEvent.getCategory().toString());
                    convertedEvent.setCreatedDateTime(
                        DateTimeUtil.convertDateToLocalDateTime(
                            originalEvent.getCreatedDateTime()));
                    return convertedEvent;
                  })
              .collect(Collectors.toList());
    }

    // Convert top level Case Service case DTO object to Contact Centre equivalent
    CaseDTO caseContactCenterDTO =
        CaseDTO.builder()
            .id(caseServiceDTO.getId())
            .caseRef(caseServiceDTO.getCaseRef())
            .caseType(caseServiceDTO.getSampleUnitType())
            .createdDateTime(
                DateTimeUtil.convertDateToLocalDateTime(caseServiceDTO.getCreatedDateTime()))
            // TODO: Populate address fields when Case service makes them available
            .responses(responses)
            .caseEvents(events)
            .build();

    return caseContactCenterDTO;
  }
}
