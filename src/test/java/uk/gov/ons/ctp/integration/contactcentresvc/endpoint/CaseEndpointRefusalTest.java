package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.SimpleDateFormat;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Contact Centre Data Endpoint Unit tests */
public final class CaseEndpointRefusalTest {

  private static final String CASE_ID = "caseId";
  private static final String TITLE = "title";
  private static final String NOTES = "notes";
  private static final String TEL_NO = "telNo";
  private static final String FORENAME = "forename";
  private static final String SURNAME = "surname";
  private static final String ADDRESS_LINE_1 = "addressLine1";
  private static final String ADDRESS_LINE_2 = "addressLine2";
  private static final String ADDRESS_LINE_3 = "addressLine3";
  private static final String ADDRESS_LINE_4 = "addressLine4";
  private static final String TOWN_NAME = "townName";
  private static final String REGION = "region";
  private static final String POSTCODE = "postcode";
  private static final String UPRN = "uprn";
  private static final String DATE_TIME = "dateTime";

  private static final String RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705Z";

  @Mock private CaseService caseService;

  @InjectMocks private CaseEndpoint caseEndpoint;

  private MockMvc mockMvc;

  // UUID_STR must match the UUID in the test fixture
  private static final String UUID_STR = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

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
  public void refusalGoodRequest() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    assertOk(json);
  }

  @Test
  public void refusalGoodBodyCaseIdMismatch() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(CASE_ID, "c43533d0-7f2f-42d9-90d2-8204edf4812e");
    assertBadRequest(json);
  }

  @Test
  public void refusalGoodBodyCaseIdBothUnknown() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(CASE_ID, "unknown");
    ResultActions actions = mockMvc.perform(postJson("/cases/unknown/refusal", json.toString()));
    actions.andExpect(status().isOk());
  }

  @Test
  public void refusalBlankUUID() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    String uuid = "  ";
    json.put(CASE_ID, uuid);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void refusalGoodBodyCaseIdBothFred() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(CASE_ID, "fred");
    ResultActions actions = mockMvc.perform(postJson("/cases/fred/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void refusalTelNoNull() throws Exception {
    assertOk(TEL_NO, (String) null);
  }

  @Test
  public void refusalTelNoBlank() throws Exception {
    assertOk(TEL_NO, "");
  }

  @Test
  public void refusalTelNoTooLong() throws Exception {
    assertBadRequest(TEL_NO, "07968583119119119119119");
  }

  @Test
  public void refusalTitleNull() throws Exception {
    assertOk(TITLE, (String) null);
  }

  @Test
  public void refusalTitleBlank() throws Exception {
    assertOk(TITLE, "");
  }

  @Test
  public void refusalTitleTooLong() throws Exception {
    assertBadRequest(TITLE, "Mrrrrrrrrrrrr");
  }

  @Test
  public void refusalNotesNull() throws Exception {
    assertOk(NOTES, (String) null);
  }

  @Test
  public void refusalNotesBlank() throws Exception {
    assertOk(NOTES, "");
  }

  @Test
  public void refusalNotesTooLong() throws Exception {
    assertBadRequest(NOTES, StringUtils.repeat("A", 513));
  }

  @Test
  public void refusalUPRNNull() throws Exception {
    assertOk(UPRN, (String) null);
  }

  @Test
  public void refusalUPRNBlank() throws Exception {
    assertOk(UPRN, "");
  }

  @Test
  public void refusalUPRNTooLong() throws Exception {
    assertBadRequest(UPRN, "1234567890123");
  }

  @Test
  public void refusalForenameNull() throws Exception {
    assertOk(FORENAME, (String) null);
  }

  @Test
  public void refusalForenameBlank() throws Exception {
    assertOk(FORENAME, "");
  }

  @Test
  public void refusalForenameTooLong() throws Exception {
    assertBadRequest(FORENAME, "Phillllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
  }

  @Test
  public void refusalSurnameNull() throws Exception {
    assertOk(SURNAME, (String) null);
  }

  @Test
  public void refusalSurnameBlank() throws Exception {
    assertOk(SURNAME, "");
  }

  @Test
  public void refusalSurnameTooLong() throws Exception {
    assertBadRequest(SURNAME, "Whilessssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void refusalAddressLine1Null() throws Exception {
    assertOk(ADDRESS_LINE_1, (String) null);
  }

  @Test
  public void refusalAddressLine1Blank() throws Exception {
    assertOk(ADDRESS_LINE_1, "");
  }

  @Test
  public void refusalAddressLine1TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_1, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void refusalAddressLine2Null() throws Exception {
    assertOk(ADDRESS_LINE_2, (String) null);
  }

  @Test
  public void refusalAddressLine2Blank() throws Exception {
    assertOk(ADDRESS_LINE_2, "");
  }

  @Test
  public void refusalAddressLine2TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_2, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void refusalAddressLine3Null() throws Exception {
    assertOk(ADDRESS_LINE_3, (String) null);
  }

  @Test
  public void refusalAddressLine3Blank() throws Exception {
    assertOk(ADDRESS_LINE_3, "");
  }

  @Test
  public void refusalAddressLine3TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_3, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void refusalAddressLine4Null() throws Exception {
    assertOk(ADDRESS_LINE_4, (String) null);
  }

  @Test
  public void refusalAddressLine4Blank() throws Exception {
    assertOk(ADDRESS_LINE_4, "");
  }

  @Test
  public void refusalAddressLine4TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_4, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void refusalTownNameNull() throws Exception {
    assertOk(TOWN_NAME, (String) null);
  }

  @Test
  public void refusalTownNameBlank() throws Exception {
    assertOk(TOWN_NAME, "");
  }

  @Test
  public void refusalTownNameTooLong() throws Exception {
    assertBadRequest(TOWN_NAME, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void refusalRegionNull() throws Exception {
    assertOk(REGION, (String) null);
  }

  @Test
  public void refusalRegionBlank() throws Exception {
    assertOk(REGION, "");
  }

  @Ignore // TODO until region validation in place
  @Test
  public void refusalRegionBad() throws Exception {
    assertBadRequest(REGION, "X");
  }

  @Test
  public void refusalPostcodeNull() throws Exception {
    assertOk(POSTCODE, (String) null);
  }

  @Test
  public void refusalPostcodeBlank() throws Exception {
    assertOk(POSTCODE, "");
  }

  @Test
  public void refusalPostcodeBad() throws Exception {
    assertBadRequest(POSTCODE, "SO100 100HJ");
  }

  @Test
  public void refusalDateTimeNull() throws Exception {
    assertBadRequest(DATE_TIME, (String) null);
  }

  @Test
  public void refusalDateTimeBlank() throws Exception {
    assertBadRequest(DATE_TIME, "");
  }

  @Test
  public void refusalDateTimeTooLong() throws Exception {
    assertBadRequest(DATE_TIME, "2007:12:03T10-15-30");
  }

  private void assertOk(String field, String value) throws Exception {
    UUID uuid = UUID.randomUUID();
    SimpleDateFormat dateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    ResponseDTO responseDTO =
        ResponseDTO.builder()
            .id(uuid.toString())
            .dateTime(dateFormat.parse(RESPONSE_DATE_TIME))
            .build();
    Mockito.when(caseService.reportRefusal(any(), any())).thenReturn(responseDTO);

    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(field, value);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(uuid.toString())));
    actions.andExpect(jsonPath("$.dateTime", is(RESPONSE_DATE_TIME)));
  }

  private void assertBadRequest(String field, String value) throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(field, value);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  private void assertOk(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isOk());
  }

  private void assertBadRequest(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + UUID_STR + "/refusal", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}
