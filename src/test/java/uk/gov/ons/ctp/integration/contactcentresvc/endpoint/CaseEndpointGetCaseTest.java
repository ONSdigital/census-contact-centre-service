package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Contact Centre Data Endpoint Unit tests. This class tests the get case endpoints, covering gets
 * based on uuid, ref and uprn.
 */
public final class CaseEndpointGetCaseTest {

  private static final String CASE_UUID_STRING = "dca05c61-8b95-46af-8f73-36f0dc2cbf5e";
  private static final String CASE_REF = "123456";
  private static final String CASE_TYPE = "R1";
  private static final String CASE_CREATED_DATE_TIME = "2019-01-29T14:14:27.512";
  private static final String ADDRESS_LINE_1 = "Smiths Renovations";
  private static final String ADDRESS_LINE_2 = "Rock House";
  private static final String ADDRESS_LINE_3 = "Cowick Lane";
  private static final String TOWN = "Exeter";
  private static final String REGION = "E";
  private static final String POSTCODE = "EX2 9HY";

  private static final String RESPONSE1_DATE_TIME = "2016-11-09T11:44:44.797";
  private static final String RESPONSE1_INBOUND_CHANNEL = "ONLINE";

  private static final String EVENT_CATEGORY = "REFUSAL";
  private static final String EVENT_DESCRIPTION = "Event for testcase";
  private static final String EVENT_DATE_TIME = "2017-02-11T16:32:11.863";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  private MockMvc mockMvc;

  private UUID uuid = UUID.randomUUID();

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void getCaseById_GoodId() throws Exception {
    CaseDTO testCaseDTO = createResponseCaseDTO();
    Mockito.when(caseService.getCaseById(eq(uuid), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);
  }

  @Test
  public void getCaseById_BadId() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/123456789"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseById_CaseEventsTrue() throws Exception {
    CaseDTO testCaseDTO = createResponseCaseDTO();
    Mockito.when(caseService.getCaseById(eq(uuid), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "?caseEvents=1"));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);
  }

  @Test
  public void getCaseById_CaseEventsDuff() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseByRef_GoodRef() throws Exception {
    CaseDTO testCaseDTO = createResponseCaseDTO();
    Mockito.when(caseService.getCaseByCaseReference(eq(123456L), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/ref/123456"));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);
  }

  @Test
  public void getCaseByRef_BadRef() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/ref/avg"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseByUprn_GoodUPRN() throws Exception {
    List<CaseDTO> testCases = new ArrayList<>();
    testCases.add(createResponseCaseDTO());
    testCases.add(createResponseCaseDTO());
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(caseService.getCaseByUPRN(eq(expectedUprn), any())).thenReturn(testCases);

    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012"));
    actions.andExpect(status().isOk());

    verifyStructureOfMultiResultsActions(actions);
  }

  @Test
  public void getCaseByUprn_UPRNTooLong() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012345"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseByUprn_BadUPRN() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/A12345678901234"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseByUprn_CaseEventsTrue() throws Exception {
    List<CaseDTO> testCases = new ArrayList<>();
    testCases.add(createResponseCaseDTO());
    testCases.add(createResponseCaseDTO());
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(caseService.getCaseByUPRN(eq(expectedUprn), any())).thenReturn(testCases);

    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012?caseEvents=1"));
    actions.andExpect(status().isOk());

    verifyStructureOfMultiResultsActions(actions);
  }

  @Test
  public void getCaseByUprn_CaseEventsDuff() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/12345678901234?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }

  private CaseDTO createResponseCaseDTO() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .description(EVENT_DESCRIPTION)
            .category(EVENT_CATEGORY)
            .createdDateTime(LocalDateTime.parse(EVENT_DATE_TIME, formatter))
            .build();

    CaseDTO fakeCaseDTO =
        CaseDTO.builder()
            .id(UUID.fromString(CASE_UUID_STRING))
            .caseRef(CASE_REF)
            .caseType(CASE_TYPE)
            .createdDateTime(LocalDateTime.parse(CASE_CREATED_DATE_TIME, formatter))
            .addressLine1(ADDRESS_LINE_1)
            .addressLine2(ADDRESS_LINE_2)
            .addressLine3(ADDRESS_LINE_3)
            .townName(TOWN)
            .region(REGION)
            .postcode(POSTCODE)
            .caseEvents(Arrays.asList(caseEventDTO1))
            .build();

    return fakeCaseDTO;
  }

  private void verifyStructureOfResultsActions(ResultActions actions) throws Exception {
    actions.andExpect(jsonPath("$.id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$.caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$.caseType", is(CASE_TYPE)));
    actions.andExpect(jsonPath("$.createdDateTime", is(CASE_CREATED_DATE_TIME)));
    actions.andExpect(jsonPath("$.addressLine1", is(ADDRESS_LINE_1)));
    actions.andExpect(jsonPath("$.addressLine2", is(ADDRESS_LINE_2)));
    actions.andExpect(jsonPath("$.addressLine3", is(ADDRESS_LINE_3)));
    actions.andExpect(jsonPath("$.townName", is(TOWN)));
    actions.andExpect(jsonPath("$.region", is(REGION)));
    actions.andExpect(jsonPath("$.postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$.caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$.caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$.caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));
  }

  private void verifyStructureOfMultiResultsActions(ResultActions actions) throws Exception {
    // This is not ideal - obvious duplication here - want to find a neater way of making the same
    // assertions repeatedly
    actions.andExpect(jsonPath("$[0].id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$[0].caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$[0].caseType", is(CASE_TYPE)));
    actions.andExpect(jsonPath("$[0].createdDateTime", is(CASE_CREATED_DATE_TIME)));
    actions.andExpect(jsonPath("$[0].addressLine1", is(ADDRESS_LINE_1)));
    actions.andExpect(jsonPath("$[0].addressLine2", is(ADDRESS_LINE_2)));
    actions.andExpect(jsonPath("$[0].addressLine3", is(ADDRESS_LINE_3)));
    actions.andExpect(jsonPath("$[0].townName", is(TOWN)));
    actions.andExpect(jsonPath("$[0].region", is(REGION)));
    actions.andExpect(jsonPath("$[0].postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$[0].caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$[0].caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$[0].caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));

    actions.andExpect(jsonPath("$[1].id", is(CASE_UUID_STRING)));
    actions.andExpect(jsonPath("$[1].caseRef", is(CASE_REF)));
    actions.andExpect(jsonPath("$[1].caseType", is(CASE_TYPE)));
    actions.andExpect(jsonPath("$[1].createdDateTime", is(CASE_CREATED_DATE_TIME)));
    actions.andExpect(jsonPath("$[1].addressLine1", is(ADDRESS_LINE_1)));
    actions.andExpect(jsonPath("$[1].addressLine2", is(ADDRESS_LINE_2)));
    actions.andExpect(jsonPath("$[1].addressLine3", is(ADDRESS_LINE_3)));
    actions.andExpect(jsonPath("$[1].townName", is(TOWN)));
    actions.andExpect(jsonPath("$[1].region", is(REGION)));
    actions.andExpect(jsonPath("$[1].postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$[1].caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$[1].caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$[1].caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));
  }
}
