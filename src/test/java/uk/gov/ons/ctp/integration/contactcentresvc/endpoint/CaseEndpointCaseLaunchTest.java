package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Contact Centre Data Endpoint Unit tests */
public class CaseEndpointCaseLaunchTest {

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  @Autowired private MockMvc mockMvc;

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
  public void getLaunchURL_ValidInvocation() throws Exception {
    String fakeResponse = "{\"url\": \"https://www.google.co.uk/search?q=FAKE\"}";
    Mockito.when(caseService.getLaunchURLForCaseId(any(), any())).thenReturn(fakeResponse);

    ResultActions actions =
        mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=12345&individual=false"));
    actions.andExpect(status().isOk());

    // Check that the url is as expected. Note that MockMvc (or some component in the chain) escapes
    // all double quotes
    String responseUrl = actions.andReturn().getResponse().getContentAsString();
    String expectedUrl = "\"{\\\"url\\\": \\\"https://www.google.co.uk/search?q=FAKE\\\"}\"";
    assertEquals(expectedUrl, responseUrl);
  }

  @Test
  public void getLaunchURL_BadCaseId() throws Exception {
    ResultActions actions =
        mockMvc.perform(getJson("/cases/123456789/launch?agentId=12345&individual=true"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getLaunchURL_GoodCaseIdMissingAgentTest() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?individual=true"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getLaunchURL_GoodCaseIdBadAgent() throws Exception {
    ResultActions actions =
        mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=ABC45&individual=true"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getLaunchURL_GoodCaseIdGoodAgentMissingIndividual() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=123"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getLaunchURL_GoodCaseIdGoodAgentBadIndividual() throws Exception {
    ResultActions actions =
        mockMvc.perform(getJson("/cases/" + uuid + "/launch?agentId=123&individual=x"));
    actions.andExpect(status().isBadRequest());
  }
}
