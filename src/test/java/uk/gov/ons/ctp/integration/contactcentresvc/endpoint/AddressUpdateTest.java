package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

/** Contact Centre Data endpoint Unit tests */
public final class AddressUpdateTest {

  private static final String RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705340";

  @InjectMocks private AddressEndpoint addressEndpoint;

  @Mock AddressService addressService;

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
        MockMvcBuilders.standaloneSetup(addressEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void postValidAddressUpdate() throws Exception {
    UUID uuid = UUID.randomUUID();
    ResponseDTO responseDTO =
        ResponseDTO.builder().id(uuid.toString()).dateTime(RESPONSE_DATE_TIME).build();
    Mockito.when(addressService.addressChange(any())).thenReturn(responseDTO);

    ObjectNode json = FixtureHelper.loadClassObjectNode();

    ResultActions actions = mockMvc.perform(postJson("/addresses/123", json.toString()));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(uuid.toString())));
    actions.andExpect(jsonPath("$.dateTime", is(RESPONSE_DATE_TIME)));
  }

  @Test
  public void failDueToInvalidCategory() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put("category", "uknown-and-invalid-category");
    ResultActions actions = mockMvc.perform(postJson("/addresses/123", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void failDueToEmptyRequest() throws Exception {
    ResultActions actions = mockMvc.perform(postJson("/addresses/123", ""));
    actions.andExpect(status().isBadRequest());
  }
}
