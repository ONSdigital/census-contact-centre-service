package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

/** Contact Centre Data Endpoint Unit tests */
public final class CaseEndpointCaseByUPRNTest {

  @InjectMocks private CaseEndpoint caseEndpoint;

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
  public void getCaseByUprn_GoodUPRN() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012"));
    actions.andExpect(status().isOk());
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
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/123456789012?caseEvents=1"));
    actions.andExpect(status().isOk());
  }

  @Test
  public void getCaseByUprn_CaseEventsDuff() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/uprn/12345678901234?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }
}
