package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.utility.Constants;

@Service
@Validated()
public class CaseServiceImpl implements CaseService {
  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  private MapperFacade caseDTOMapper;

  @Autowired private CaseServiceClientServiceImpl caseServiceClient;

  private static class DateMapper extends CustomConverter<String, Date> {
    private SimpleDateFormat caseDateTimeFormatter =
        new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);

    @Override
    public Date convert(
        String dateAsText, Type<? extends Date> destinationType, MappingContext mappingContext) {
      try {
        return caseDateTimeFormatter.parse(dateAsText);
      } catch (ParseException e) {
        throw new RuntimeException("Failed to parse date: " + dateAsText);
      }
    }
  }

  public CaseServiceImpl() {

    MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

    mapperFactory.classMap(CaseContainerDTO.class, CaseDTO.class).byDefault().register();

    ConverterFactory converterFactory = mapperFactory.getConverterFactory();
    DateMapper dateTimeMapper = new DateMapper();
    converterFactory.registerConverter(dateTimeMapper);

    caseDTOMapper = mapperFactory.getMapperFacade();
  }

  @Override
  public CaseDTO getCaseById(final UUID caseId, CaseRequestDTO requestParamsDTO) {
    log.debug("Fetching case details by caseId: {}", caseId);

    // Get the case details from the case service
    Boolean getCaseEvents = requestParamsDTO.getCaseEvents();
    CaseContainerDTO caseDetails = caseServiceClient.getCaseById(caseId, getCaseEvents);

    // Only return Household cases
    boolean isHouseholdCase =
        caseDetails.getCaseType().equals(Constants.CASE_SERVICE_UNIT_TYPE_HOUSEHOLD);
    if (!isHouseholdCase) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case is a non-household case");
    }

    // Convert from Case service to Contact Centre DTOs
    CaseDTO caseServiceResponse = caseDTOMapper.map(caseDetails, CaseDTO.class);

    // Make sure that we don't return any events if the caller doesn't want them
    if (!getCaseEvents) {
      caseServiceResponse.setCaseEvents(null);
    }

    log.debug("Returning case details for caseId: {}", caseId);

    return caseServiceResponse;
  }
}
