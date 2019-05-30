package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.SimpleDateFormat;
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
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Contact Centre Data Endpoint Unit tests */
public final class CaseEndpointFulfilmentPostTest {

  private static final String RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705Z";

  @Mock private CaseService caseService;

  @InjectMocks private CaseEndpoint caseEndpoint;

  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

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
  public void postFulfilmentByCaseById_GoodId() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    PostalFulfilmentRequestDTO requestData =
        mapper.convertValue(json, PostalFulfilmentRequestDTO.class);

    SimpleDateFormat dateFormat = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    ResponseDTO responseDTO =
        ResponseDTO.builder()
            .id(requestData.getCaseId().toString())
            .dateTime(dateFormat.parse(RESPONSE_DATE_TIME))
            .build();
    Mockito.when(caseService.fulfilmentRequestByPost(any(), any())).thenReturn(responseDTO);

    String jsonString = mapper.writeValueAsString(requestData);
    ResultActions actions =
        mockMvc.perform(
            postJson("/cases/" + requestData.getCaseId() + "/fulfilment/post", jsonString));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(requestData.getCaseId().toString())));
    actions.andExpect(jsonPath("$.dateTime", is(RESPONSE_DATE_TIME)));
  }

  @Test
  public void postFulfilmentByCaseById_BadId() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    ResultActions actions =
        mockMvc.perform(postJson("/cases/123456789/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postFulfilmentByCaseById_BadDate() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    PostalFulfilmentRequestDTO requestData =
        mapper.convertValue(json, PostalFulfilmentRequestDTO.class);
    json.put("dateTime", "2019:12:25 12:34:56");
    ResultActions actions =
        mockMvc.perform(
            postJson("/cases/" + requestData.getCaseId() + "/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postFulfilmentByCaseById_BadCaseId() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    PostalFulfilmentRequestDTO requestData =
        mapper.convertValue(json, PostalFulfilmentRequestDTO.class);
    json.put("caseId", "foo");
    ResultActions actions =
        mockMvc.perform(
            postJson("/cases/" + requestData.getCaseId() + "/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}
