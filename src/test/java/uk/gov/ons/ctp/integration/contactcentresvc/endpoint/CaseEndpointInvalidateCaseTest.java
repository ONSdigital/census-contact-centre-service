package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseQueryRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Test the POST endpoint to invalidate case details. */
@RunWith(MockitoJUnitRunner.class)
public final class CaseEndpointInvalidateCaseTest {

  @Mock private CaseService caseService;
  @Mock private CaseQueryRequestDTO caseQueryRequestDTO;
  @Mock private CaseDTO caseToCheck;

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
    InvalidateCaseRequestDTO dto = mapper.convertValue(json, InvalidateCaseRequestDTO.class);
    this.caseId = dto.getCaseId().toString();
    createValidResponse();
  }

  @SneakyThrows
  private void loadTestJson() {
    this.json = FixtureHelper.loadClassObjectNode();
  }

  @SneakyThrows
  private void createValidResponse() {
    this.responseDTO =
        ResponseDTO.builder().id(caseId).dateTime(dateFormat.parse(A_RESPONSE_DATE_TIME)).build();
  }

  @SneakyThrows
  private ResultActions doPost() {
    return mockMvc.perform(postJson("/cases/" + caseId + "/invalidate", json.toString()));
  }

  @SneakyThrows
  private void doPostExpectingRejection() {
    ResultActions actions = doPost();
    actions.andExpect(status().isBadRequest());
    verify(caseService, never()).invalidateCase(any());
  }

  @Test
  public void shouldModifyCase() throws Exception {
    when(caseService.invalidateCase(any())).thenReturn(responseDTO);
    ResultActions actions = doPost();
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(caseId)));
    actions.andExpect(jsonPath("$.dateTime", is(A_RESPONSE_DATE_TIME)));
    verify(caseService).invalidateCase(any());
  }

  @Test
  public void shouldRejectBadPathCaseId() {
    caseId = "123456789";
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectBadBodyCaseId() {
    json.put("caseId", "foo");
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectMismatchingCaseId() {
    json.put("caseId", UUID.randomUUID().toString());
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectBadDate() {
    json.put("dateTime", "2019:12:25 12:34:56");
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectBadStatus() {
    json.put("status", "FOO");
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectMissingStatus() {
    json.remove("status");
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectMissingCaseId() {
    json.remove("caseId");
    doPostExpectingRejection();
  }

  @Test
  public void shouldRejectMissingDateTime() {
    json.remove("dateTime");
    doPostExpectingRejection();
  }

  //  @Test
  //  public void shouldRejectAsCaseIsOfTypeCE() throws Exception {
  //    caseId = "77346443-64ae-422e-9b93-d5250f48a27a";
  //    json.put("caseId", "77346443-64ae-422e-9b93-d5250f48a27a");
  //    json.put("status", "DOES_NOT_EXIST");
  //    ResultActions actions = doPost();
  //    actions.andExpect(status().isBadRequest());
  //    verify(caseService, never()).invalidateCase(any());
  //  }
}
