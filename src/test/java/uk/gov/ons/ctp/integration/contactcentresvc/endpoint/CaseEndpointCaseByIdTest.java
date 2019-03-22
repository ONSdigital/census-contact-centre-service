package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.util.UUID;
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
public final class CaseEndpointCaseByIdTest {

  @InjectMocks private CaseEndpoint caseEndpoint;

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
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid));
    actions.andExpect(status().isOk());
  }

  @Test
  public void getCaseById_BadId() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/123456789"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void getCaseById_CaseEventsTrue() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "?caseEvents=1"));
    actions.andExpect(status().isOk());
  }

  @Test
  public void getCaseById_CaseEventsDuff() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/cases/" + uuid + "?caseEvents=maybe"));
    actions.andExpect(status().isBadRequest());
  }
}
