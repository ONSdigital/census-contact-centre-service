package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.util.Arrays;
import java.util.List;
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
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.FulfilmentsService;

/** Contact Centre Data Endpoint Unit tests */
public final class FulfilmentsEndpointTest {

  private static final String PARAM_CASE_TYPE = "caseType";
  private static final String PARAM_REGION = "region";
  private static final String PARAM_INDIVIDUAL = "individual";

  private static final String FULFILMENT_CODE_1 = "P1";
  private static final String DESCRIPTION_1 = "First fulfilment";

  private static final String FULFILMENT_CODE_2 = "P2";
  private static final String DESCRIPTION_2 = "Another fulfilment";

  @Mock private FulfilmentsService fulfilmentService;

  @InjectMocks private FulfilmentsEndpoint fulfilmentsEndpoint;

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
        MockMvcBuilders.standaloneSetup(fulfilmentsEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void fulfilmentsGoodRequestNoParams() throws Exception {
    List<FulfilmentDTO> testCaseDTO = createResponseFulfilmentDTO();
    Mockito.when(fulfilmentService.getFulfilments(any(), any(), any(), any(), any()))
        .thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/fulfilments"));
    actions.andExpect(status().isOk());

    verifyStructureOfFulfilmentDTO(actions);
  }

  @Test
  public void fulfilmentsGoodRequestAllParams() throws Exception {
    List<FulfilmentDTO> testCaseDTO = createResponseFulfilmentDTO();
    Mockito.when(fulfilmentService.getFulfilments(any(), any(), any(), any(), any()))
        .thenReturn(testCaseDTO);

    ResultActions actions =
        mockMvc.perform(
            getJson("/fulfilments").param(PARAM_REGION, "E").param(PARAM_INDIVIDUAL, "true"));
    actions.andExpect(status().isOk());

    verifyStructureOfFulfilmentDTO(actions);
  }

  @Test
  public void fulfilmentsGoodRequestBadCaseType() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/fulfilments").param(PARAM_CASE_TYPE, "XX"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentsGoodRequestBadRegion() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/fulfilments").param(PARAM_REGION, "FR"));
    actions.andExpect(status().isBadRequest());
  }

  private List<FulfilmentDTO> createResponseFulfilmentDTO() {
    FulfilmentDTO fulfilmentsDTO1 =
        FulfilmentDTO.builder()
            .fulfilmentCode(FULFILMENT_CODE_1)
            .description(DESCRIPTION_1)
            .deliveryChannel(DeliveryChannel.SMS)
            .build();

    FulfilmentDTO fulfilmentsDTO2 =
        FulfilmentDTO.builder()
            .fulfilmentCode(FULFILMENT_CODE_2)
            .description(DESCRIPTION_2)
            .deliveryChannel(DeliveryChannel.POST)
            .build();

    List<FulfilmentDTO> fulfilments = Arrays.asList(fulfilmentsDTO1, fulfilmentsDTO2);
    return fulfilments;
  }

  private void verifyStructureOfFulfilmentDTO(ResultActions actions) throws Exception {
    actions.andExpect(jsonPath("$.[0].fulfilmentCode", is(FULFILMENT_CODE_1)));
    actions.andExpect(jsonPath("$.[0].description", is(DESCRIPTION_1)));
    actions.andExpect(jsonPath("$.[0].deliveryChannel", is(DeliveryChannel.SMS.toString())));

    actions.andExpect(jsonPath("$.[1].fulfilmentCode", is(FULFILMENT_CODE_2)));
    actions.andExpect(jsonPath("$.[1].description", is(DESCRIPTION_2)));
    actions.andExpect(jsonPath("$.[1].deliveryChannel", is(DeliveryChannel.POST.toString())));
  }
}
