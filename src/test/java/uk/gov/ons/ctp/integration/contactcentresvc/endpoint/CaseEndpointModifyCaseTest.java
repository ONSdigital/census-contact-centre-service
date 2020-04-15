package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.putJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.contactcentresvc.CaseServiceFixture.A_RESPONSE_DATE_TIME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.SimpleDateFormat;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Test the PUT endpoint to modify case details. */
@RunWith(MockitoJUnitRunner.class)
public final class CaseEndpointModifyCaseTest {

  @Mock private CaseService caseService;

  @InjectMocks private CaseEndpoint caseEndpoint;

  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  private ObjectNode json;
  private ResponseDTO responseDTO;
  private SimpleDateFormat dateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
  private String caseId;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();

    loadTestJson();
    ModifyCaseRequestDTO dto = mapper.convertValue(json, ModifyCaseRequestDTO.class);
    this.caseId = dto.getCaseId().toString();
    createValidResponse(dto);
  }

  @SneakyThrows
  private void loadTestJson() {
    this.json = FixtureHelper.loadClassObjectNode();
  }

  @SneakyThrows
  private void createValidResponse(ModifyCaseRequestDTO dto) {
    this.responseDTO =
        ResponseDTO.builder().id(caseId).dateTime(dateFormat.parse(A_RESPONSE_DATE_TIME)).build();
  }

  @SneakyThrows
  private ResultActions doPut() {
    return mockMvc.perform(putJson("/cases/" + caseId, json.toString()));
  }

  @SneakyThrows
  private void doPutExpectingRejection() {
    ResultActions actions = doPut();
    actions.andExpect(status().isBadRequest());
    verify(caseService, never()).modifyCase(any());
  }

  @Test
  public void shouldModifyCase() throws Exception {
    when(caseService.modifyCase(any())).thenReturn(responseDTO);
    ResultActions actions = doPut();
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(caseId)));
    actions.andExpect(jsonPath("$.dateTime", is(A_RESPONSE_DATE_TIME)));
    verify(caseService).modifyCase(any());
  }

  @Test
  public void shouldRejectBadPathCaseId() {
    caseId = "123456789";
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadBodyCaseId() {
    json.put("caseId", "foo");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMismatchingCaseId() {
    json.put("caseId", UUID.randomUUID().toString());
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadDate() {
    json.put("dateTime", "2019:12:25 12:34:56");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadStatus() {
    json.put("status", "FOO");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadRegion() {
    json.put("region", "X");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadEstabType() {
    json.put("estabType", "FOO");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingRegion() {
    json.remove("region");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingStatus() {
    json.remove("status");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingCaseId() {
    json.remove("caseId");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingAddressLine1() {
    json.remove("addressLine1");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingTownName() {
    json.remove("townName");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingEstabType() {
    json.remove("estabType");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingPostcode() {
    json.remove("postcode");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingDateTime() {
    json.remove("dateTime");
    doPutExpectingRejection();
  }
}
