package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.EventServiceImpl;

/** Contact Centre Data Endpoint Unit tests */
public final class RefusalsEndpointTest {

  private static final String CASE_TYPE = "caseType";

  @Mock private EventServiceImpl eventSvc;

  @InjectMocks private RefusalsEndpoint refusalsEndpoint;

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
        MockMvcBuilders.standaloneSetup(refusalsEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void fulfilmentsGoodRequestNoParams() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/refusals"));
    actions.andExpect(status().isOk());
  }

  @Test
  public void fulfilmentsGoodRequestGoodCaseType() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/refusals").param(CASE_TYPE, "HI"));
    actions.andExpect(status().isOk());
  }

  @Test
  public void fulfilmentsGoodRequestBadCaseType() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/refusals").param(CASE_TYPE, "HIDEHI"));
    actions.andExpect(status().isBadRequest());
  }
}
