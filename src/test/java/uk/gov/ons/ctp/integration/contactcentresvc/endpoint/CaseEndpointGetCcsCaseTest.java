package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/**
 * Contact Centre Data Endpoint Unit tests. This class tests the get ccs case endpoints, covering
 * get ccs case by postcode.
 */
public final class CaseEndpointGetCcsCaseTest {

  private static final String CASE_UUID_STRING = "dca05c61-8b95-46af-8f73-36f0dc2cbf5e";
  private static final String CASE_REF = "123456";
  private static final String CASE_TYPE = "R1";
  private static final String CASE_CREATED_DATE_TIME = "2019-01-29T14:14:27.512Z";
  private static final String ADDRESS_LINE_1 = "Smiths Renovations";
  private static final String ADDRESS_LINE_2 = "Rock House";
  private static final String ADDRESS_LINE_3 = "Cowick Lane";
  private static final String TOWN = "Exeter";
  private static final String REGION = "E";
  private static final String POSTCODE = "GW12 AAA";
  private static final String ORG_NAME = "Tiddlywink Athletic Club";

  private static final String EVENT_CATEGORY = "REFUSAL";
  private static final String EVENT_DESCRIPTION = "Event for testcase";
  private static final String EVENT_DATE_TIME = "2017-02-11T16:32:11.863Z";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  private MockMvc mockMvc;

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
  public void getCcsCaseByPostcode_PostcodeInList() throws Exception {
    List<CaseDTO> testCases = new ArrayList<>();
    testCases.add(createResponseCaseDTO());
    testCases.add(createResponseCaseDTO());
    Mockito.when(caseService.getCCSCaseByPostcode(eq(POSTCODE))).thenReturn(testCases);

    ResultActions actions = mockMvc.perform(getJson("/cases/ccs/postcode/" + POSTCODE));
    actions.andExpect(status().isOk());

    verifyStructureOfMultiResultsActions(actions);
  }

  @Test
  public void getCcsCaseByPostcode_PostcodeInListButNotInRM() throws Exception {
    ResponseStatusException ex =
        new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Not Found", new HttpClientErrorException(HttpStatus.NOT_FOUND));
    Mockito.when(caseService.getCCSCaseByPostcode(eq("GW12 AAB"))).thenThrow(ex);
    ResultActions actions = mockMvc.perform(getJson("/cases/ccs/postcode/GW12 AAB"));
    actions.andExpect(status().isNotFound());
  }

  @Test
  public void getCcsCaseByPostcode_PostcodeNotInList() throws Exception {
    ResponseStatusException ex =
        new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            new HttpClientErrorException(HttpStatus.BAD_REQUEST));
    Mockito.when(caseService.getCCSCaseByPostcode(eq("GW12 AAC"))).thenThrow(ex);
    ResultActions actions = mockMvc.perform(getJson("/cases/ccs/postcode/GW12 AAC"));
    actions.andExpect(status().isBadRequest());
  }

  private CaseDTO createResponseCaseDTO() throws ParseException {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    CaseEventDTO caseEventDTO1 =
        CaseEventDTO.builder()
            .description(EVENT_DESCRIPTION)
            .category(EVENT_CATEGORY)
            .createdDateTime(formatter.parse(EVENT_DATE_TIME))
            .build();

    CaseDTO fakeCaseDTO =
        CaseDTO.builder()
            .id(UUID.fromString(CASE_UUID_STRING))
            .caseRef(CASE_REF)
            .caseType(CASE_TYPE)
            .createdDateTime(formatter.parse(CASE_CREATED_DATE_TIME))
            .addressLine1(ADDRESS_LINE_1)
            .addressLine2(ADDRESS_LINE_2)
            .addressLine3(ADDRESS_LINE_3)
            .townName(TOWN)
            .region(REGION)
            .postcode(POSTCODE)
            .ceOrgName(ORG_NAME)
            .caseEvents(Arrays.asList(caseEventDTO1))
            .build();

    return fakeCaseDTO;
  }

  private void verifyStructureOfMultiResultsActions(ResultActions actions) throws Exception {
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
    actions.andExpect(jsonPath("$[0].ceOrgName", is(ORG_NAME)));

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
    actions.andExpect(jsonPath("$[1].ceOrgName", is(ORG_NAME)));

    actions.andExpect(jsonPath("$[1].caseEvents[0].category", is(EVENT_CATEGORY)));
    actions.andExpect(jsonPath("$[1].caseEvents[0].description", is(EVENT_DESCRIPTION)));
    actions.andExpect(jsonPath("$[1].caseEvents[0].createdDateTime", is(EVENT_DATE_TIME)));
  }
}
