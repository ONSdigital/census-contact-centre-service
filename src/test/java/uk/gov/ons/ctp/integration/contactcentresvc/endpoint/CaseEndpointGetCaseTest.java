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
import java.util.Arrays;
import java.util.UUID;
import org.assertj.core.util.Lists;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseResponseDTO;
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
  private static final String ADDRESS_LINE_4 = "";
  private static final String TOWN = "Exeter";
  private static final String REGION = "E";
  private static final String POSTCODE = "EX2 9HY";

  private static final String RESPONSE1_DATE_TIME = "2016-11-09T11:44:44.797";
  private static final String RESPONSE1_INBOUND_CHANNEL = "ONLINE";

  private static final String EVENT_UUID_STRING = "1d014993-d3f2-40f9-b00a-5e6c9729ee89";
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
    CaseDTO testCaseDTO = createResponseCaseDTO();
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(caseService.getCaseByUPRN(eq(expectedUprn), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012"));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);
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
    CaseDTO testCaseDTO = createResponseCaseDTO();
    UniquePropertyReferenceNumber expectedUprn = new UniquePropertyReferenceNumber(123456789012L);
    Mockito.when(caseService.getCaseByUPRN(eq(expectedUprn), any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012?caseEvents=1"));
    actions.andExpect(status().isOk());

    verifyStructureOfResultsActions(actions);
  }

  @Test
  public void getCaseByUprn_CaseEventsDuff() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/12345678901234?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }

  private CaseDTO createResponseCaseDTO() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    CaseResponseDTO caseResponseDTO1 =
        CaseResponseDTO.builder()
            .dateTime(RESPONSE1_DATE_TIME)
            .inboundChannel(RESPONSE1_INBOUND_CHANNEL)
            .build();

    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .id(UUID.fromString(EVENT_UUID_STRING))
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
            .addressLine4(ADDRESS_LINE_4)
            .town(TOWN)
            .region(REGION)
            .postcode(POSTCODE)
            .responses(Lists.newArrayList(caseResponseDTO1))
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
    actions.andExpect(jsonPath("$.addressLine4", is(ADDRESS_LINE_4)));
    actions.andExpect(jsonPath("$.town", is(TOWN)));
    actions.andExpect(jsonPath("$.region", is(REGION)));
    actions.andExpect(jsonPath("$.postcode", is(POSTCODE)));

    actions.andExpect(jsonPath("$.responses[0].dateTime", is(RESPONSE1_DATE_TIME)));
    actions.andExpect(jsonPath("$.responses[0].inboundChannel", is(RESPONSE1_INBOUND_CHANNEL)));

    actions.andExpect(jsonPath("$.caseEvents[0].id", is(EVENT_UUID_STRING)));
    actions.andExpect(jsonPath("$.caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$.caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$.caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));
  }
}
