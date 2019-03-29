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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO.Method;
import uk.gov.ons.ctp.integration.contactcentresvc.service.FulfilmentsService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.EventServiceImpl;

/** Contact Centre Data Endpoint Unit tests */
public final class FulfilmentsEndpointTest {

  private static final String CASE_TYPE = "caseType";
  private static final String REGION = "region";

  private static final String PRODUCT_CODE_1 = "P1";
  private static final String LANGUAGE_1 = "English";
  private static final String DESCRIPTION_1 = "First fulfilment";

  private static final String PRODUCT_CODE_2 = "P2";
  private static final String LANGUAGE_2 = "Welsh";
  private static final String DESCRIPTION_2 = "Another fulfilment";

  @Mock private EventServiceImpl eventSvc;

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
    Mockito.when(fulfilmentService.getFulfilments(any())).thenReturn(testCaseDTO);

    ResultActions actions = mockMvc.perform(getJson("/fulfilments"));
    actions.andExpect(status().isOk());

    verifyStructureOfFulfilmentDTO(actions);
  }

  @Test
  public void fulfilmentsGoodRequestAllParams() throws Exception {
    List<FulfilmentDTO> testCaseDTO = createResponseFulfilmentDTO();
    Mockito.when(fulfilmentService.getFulfilments(any())).thenReturn(testCaseDTO);

    ResultActions actions =
        mockMvc.perform(getJson("/fulfilments").param(CASE_TYPE, "HI").param(REGION, "E"));
    actions.andExpect(status().isOk());

    verifyStructureOfFulfilmentDTO(actions);
  }

  @Test
  public void fulfilmentsGoodRequestBadCaseType() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/fulfilments").param(CASE_TYPE, "HIDEHI"));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentsGoodRequestBadRegion() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/fulfilments").param(REGION, "FR"));
    actions.andExpect(status().isBadRequest());
  }

  private List<FulfilmentDTO> createResponseFulfilmentDTO() {
    FulfilmentDTO fulfilmentsDTO1 =
        FulfilmentDTO.builder()
            .productCode(PRODUCT_CODE_1)
            .language(LANGUAGE_1)
            .description(DESCRIPTION_1)
            .method(Method.EMAIL)
            .build();

    FulfilmentDTO fulfilmentsDTO2 =
        FulfilmentDTO.builder()
            .productCode(PRODUCT_CODE_2)
            .language(LANGUAGE_2)
            .description(DESCRIPTION_2)
            .method(Method.POST)
            .build();

    List<FulfilmentDTO> fulfilments = Arrays.asList(fulfilmentsDTO1, fulfilmentsDTO2);
    return fulfilments;
  }

  private void verifyStructureOfFulfilmentDTO(ResultActions actions) throws Exception {
    actions.andExpect(jsonPath("$.[0].productCode", is(PRODUCT_CODE_1)));
    actions.andExpect(jsonPath("$.[0].language", is(LANGUAGE_1)));
    actions.andExpect(jsonPath("$.[0].description", is(DESCRIPTION_1)));
    actions.andExpect(jsonPath("$.[0].method", is(Method.EMAIL.toString())));

    actions.andExpect(jsonPath("$.[1].productCode", is(PRODUCT_CODE_2)));
    actions.andExpect(jsonPath("$.[1].language", is(LANGUAGE_2)));
    actions.andExpect(jsonPath("$.[1].description", is(DESCRIPTION_2)));
    actions.andExpect(jsonPath("$.[1].method", is(Method.POST.toString())));
  }
}
