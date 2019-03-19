package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** Contact Centre Data endpoint Unit tests */
public final class AddressEndpointTest {
  @InjectMocks private AddressEndpoint contactCentreDataEndpoint;

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
        MockMvcBuilders.standaloneSetup(contactCentreDataEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void getContactCentreDataFromEndpoint() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/contactcentre/data"));
    actions.andExpect(status().isOk());
  }


  @Test
  public void rejectAddressQueryMissingInput() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses"))
        .andExpect(content().string(containsString("field 'input'")))
        .andExpect(
            content().string(containsString("[input]]; default message [must not be null]")));
  }

  @Test
  public void rejectAddressQueryWithEmptyInput() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input="))
        .andExpect(content().string(containsString("field 'input'")))
        .andExpect(content().string(containsString("must match")));
  }

  @Test
  public void rejectAddressQueryWithOffsetBelowMin() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input=Harbour").param("offset", "-1"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectAddressQueryWithOffsetAboveMax() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input=Harbour").param("offset", "251"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 250")));
  }

  @Test
  public void rejectAddressQueryWithNonNumericOffset() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input=Harbour").param("offset", "non-numeric-value"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")));
  }

  @Test
  public void rejectAddressQueryWithLimitBelowMin() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input=Harbour").param("limit", "-1"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectAddressQueryWithLimitAboveMax() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input=Harbour").param("limit", "101"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 100")));
  }

  @Test
  public void rejectAddressQueryWithNonNumericLimit() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input=Harbour").param("limit", "x"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")));
  }
}
