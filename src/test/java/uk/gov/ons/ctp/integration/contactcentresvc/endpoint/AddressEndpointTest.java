package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.google.common.collect.Lists;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.AddressQueryResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;

/** Contact Centre Data endpoint Unit tests */
public final class AddressEndpointTest {

  private static final String DATA_VERSION = "39";

  private static final String UPRN1 = "100041133017";
  private static final String FORMATTED_ADDRESS1 = "13 Smiths Court, Exeter, EX2 8EB";
  private static final String WELSH_FORMATTED_ADDRESS1 = "13w Smiths Court, Exeter, EX2 8EB";

  private static final String UPRN2 = "100041133019";
  private static final String FORMATTED_ADDRESS2 = "15 Smiths Court, Exeter, EX2 8EB";
  private static final String WELSH_FORMATTED_ADDRESS2 = "15w Smiths Court, Exeter, EX2 8EB";

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
  public void getContactCentreDataFromEndpoint() throws Exception {
    ResultActions actions = mockMvc.perform(getJson("/contactcentre/data"));
    actions.andExpect(status().isOk());
  }

  @Test
  public void validateAddressQueryResponseJson() throws Exception {
    assertOk("/contactcentre/addresses?input=Park");
  }

  @Test
  public void rejectAddressQueryMissingInput() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses"))
        .andExpect(content().string(containsString("field 'input'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectAddressQueryWithEmptyInput() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses?input="))
        .andExpect(content().string(containsString("field 'input'")))
        .andExpect(content().string(containsString("must not be blank")));
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

  @Test
  public void validatePostcodeQueryResponseJson() throws Exception {
    assertOk("/contactcentre/addresses/postcode?postcode=EX2 8EB");
  }

  @Test
  public void rejectPostcodeQueryMissingPostcode() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode"))
        .andExpect(content().string(containsString("field 'postcode'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectPostcodeQueryWithEmptyPostcode() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode?postcode="))
        .andExpect(content().string(containsString("field 'postcode'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectPostcodeQueryWithOffsetBelowMin() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode?postcode=EX24LU").param("offset", "-1"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectPostcodeQueryWithOffsetAboveMax() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode?postcode=EX24LU").param("offset", "251"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 250")));
  }

  @Test
  public void rejectPostcodeQueryWithNonNumericOffset() throws Exception {
    mockMvc
        .perform(
            get("/contactcentre/addresses/postcode?postcode=EX24LU")
                .param("offset", "non-numeric-value"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")));
  }

  @Test
  public void rejectPostcodeQueryWithLimitBelowMin() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode?postcode=EX24LU").param("limit", "-1"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectPostcodeQueryWithLimitAboveMax() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode?postcode=EX24LU").param("limit", "101"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 100")));
  }

  @Test
  public void rejectPostcodeQueryWithNonNumericLimit() throws Exception {
    mockMvc
        .perform(get("/contactcentre/addresses/postcode?postcode=EX24LU").param("limit", "x"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")));
  }

  private void assertOk(String url) throws Exception {
    AddressDTO address1 = new AddressDTO();
    address1.setUprn(UPRN1);
    address1.setFormattedAddress(FORMATTED_ADDRESS1);
    address1.setWelshFormattedAddress(WELSH_FORMATTED_ADDRESS1);

    AddressDTO address2 = new AddressDTO();
    address2.setUprn(UPRN2);
    address2.setFormattedAddress(FORMATTED_ADDRESS2);
    address2.setWelshFormattedAddress(WELSH_FORMATTED_ADDRESS2);

    AddressQueryResponseDTO addresses = new AddressQueryResponseDTO();
    addresses.setDataVersion(DATA_VERSION);
    addresses.setAddresses(Lists.newArrayList(address1, address2));
    addresses.setTotal(2);

    Mockito.when(addressService.addressQuery(any())).thenReturn(addresses);

    ResultActions actions = mockMvc.perform(get("/contactcentre/addresses?input=Park"));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.dataVersion", is(DATA_VERSION)));
    actions.andExpect(jsonPath("$.addresses[0].uprn", is(UPRN1)));
    actions.andExpect(jsonPath("$.addresses[0].formattedAddress", is(FORMATTED_ADDRESS1)));
    actions.andExpect(
        jsonPath("$.addresses[0].welshFormattedAddress", is(WELSH_FORMATTED_ADDRESS1)));
    actions.andExpect(jsonPath("$.addresses[1].uprn", is(UPRN2)));
    actions.andExpect(jsonPath("$.addresses[1].formattedAddress", is(FORMATTED_ADDRESS2)));
    actions.andExpect(
        jsonPath("$.addresses[1].welshFormattedAddress", is(WELSH_FORMATTED_ADDRESS2)));
    actions.andExpect(jsonPath("$.total", is(2)));
  }
}
