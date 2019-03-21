package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.EventServiceImpl;

/** Contact Centre Data Endpoint Unit tests */
public final class CaseEndpointFulfilmentPostTest {

  @Mock private EventServiceImpl eventSvc;

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
  public void postFulfilmentByCaseById_GoodId() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/fulfilment/post", json.toString()));
    actions.andExpect(status().isOk());
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
    json.put("dateTime", "2019:12:25 12:34:56");
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postFulfilmentByCaseById_BadCaseId() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put("caseId", "foo");
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}
