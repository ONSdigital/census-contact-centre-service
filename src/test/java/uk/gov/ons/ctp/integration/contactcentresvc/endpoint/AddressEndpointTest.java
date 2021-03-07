package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.google.common.collect.Lists;
import java.util.ArrayList;
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
import uk.gov.ons.ctp.common.domain.EstabType;
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

  private static final String COUNTRY_CODE = "E";
  private static final String ADDRESS_TYPE = "HH";
  private static final String ESTAB_TYPE = EstabType.ROYAL_HOUSEHOLD.name();
  private static final String ESTAB_DESCRIPTION = EstabType.ROYAL_HOUSEHOLD.getCode();

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
  public void validateAddressQueryResponseJson() throws Exception {
    assertOk("/addresses?input=Parks");
  }

  @Test
  public void rejectAddressQueryMissingInput() throws Exception {
    mockMvc
        .perform(get("/addresses"))
        .andExpect(content().string(containsString("field 'input'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectAddressQueryWithEmptyInput() throws Exception {
    mockMvc
        .perform(get("/addresses?input="))
        .andExpect(content().string(containsString("field 'input'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectAddressQueryWithOffsetBelowMin() throws Exception {
    mockMvc
        .perform(get("/addresses?input=Harbour").param("offset", "-1"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void acceptAddressQueryWithValidAddressQueries() throws Exception {
    ArrayList<String> addressQueries = new ArrayList<>();
    addressQueries.add("WOOOOOW");
    addressQueries.add("   WOOOOOW   ");
    addressQueries.add("W   O   W");
    addressQueries.add("'W   O   W,");
    addressQueries.add("  $   O   $  ");
    addressQueries.add("  $  / O |  $  ");

    for (String i : addressQueries) {
      assertOk("/addresses?input=" + i);
    }
  }

  @Test
  public void rejectAddressQueryWithLessThan5ValidCharacters() throws Exception {
    ArrayList<String> addressQueries = new ArrayList<>();
    addressQueries.add("WO''''OW");
    addressQueries.add("X  ,  '");
    addressQueries.add("   WOW   ");
    addressQueries.add("W,,,O,,,W");
    addressQueries.add("'W','OW,");
    addressQueries.add("$O$  ");
    addressQueries.add("  $/O$");
    addressQueries.add("a");
    addressQueries.add("aa");
    addressQueries.add("aaa");
    addressQueries.add("aaaa");
    addressQueries.add("a a");
    addressQueries.add("aa a");

    for (String i : addressQueries) {
      mockMvc
          .perform(get("/addresses?input=" + i))
          .andExpect(
              content().string(containsString("Address query requires 5 or more characters, ")))
          .andExpect(
              content()
                  .string(
                      containsString(
                          "not including single quotes, commas or leading/trailing whitespace")));
    }
  }

  @Test
  public void rejectAddressQueryWithOffsetAboveMax() throws Exception {
    mockMvc
        .perform(get("/addresses?input=Harbour").param("offset", "251"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 250")));
  }

  @Test
  public void rejectAddressQueryWithNonNumericOffset() throws Exception {
    mockMvc
        .perform(get("/addresses?input=Harbour").param("offset", "non-numeric-value"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")));
  }

  @Test
  public void rejectAddressQueryWithLimitBelowMin() throws Exception {
    mockMvc
        .perform(get("/addresses?input=Harbour").param("limit", "-1"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectAddressQueryWithLimitAboveMax() throws Exception {
    mockMvc
        .perform(get("/addresses?input=Harbour").param("limit", "101"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 100")));
  }

  @Test
  public void rejectAddressQueryWithNonNumericLimit() throws Exception {
    mockMvc
        .perform(get("/addresses?input=Harbour").param("limit", "x"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")));
  }

  @Test
  public void validatePostcodeQueryResponseJson() throws Exception {
    assertOk("/addresses/postcode?postcode=EX2 8EB");
  }

  @Test
  public void rejectPostcodeQueryMissingPostcode() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode"))
        .andExpect(content().string(containsString("field 'postcode'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectPostcodeQueryWithEmptyPostcode() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode="))
        .andExpect(content().string(containsString("field 'postcode'")))
        .andExpect(content().string(containsString("must not be blank")));
  }

  @Test
  public void rejectPostcodeQueryWithOffsetBelowMin() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode=EX24LU").param("offset", "-1"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectPostcodeQueryWithOffsetAboveMax() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode=EX24LU").param("offset", "5001"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 5000")));
  }

  @Test
  public void rejectPostcodeQueryWithNonNumericOffset() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode=EX24LU").param("offset", "non-numeric-value"))
        .andExpect(content().string(containsString("on field 'offset': rejected value")));
  }

  @Test
  public void rejectPostcodeQueryWithLimitBelowMin() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode=EX24LU").param("limit", "-1"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be greater than or equal to 0")));
  }

  @Test
  public void rejectPostcodeQueryWithLimitAboveMax() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode=EX24LU").param("limit", "5001"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")))
        .andExpect(content().string(containsString("must be less than or equal to 5000")));
  }

  @Test
  public void rejectPostcodeQueryWithNonNumericLimit() throws Exception {
    mockMvc
        .perform(get("/addresses/postcode?postcode=EX24LU").param("limit", "x"))
        .andExpect(content().string(containsString("on field 'limit': rejected value")));
  }

  private void assertOk(String url) throws Exception {
    AddressDTO address1 = new AddressDTO();
    address1.setUprn(UPRN1);
    address1.setFormattedAddress(FORMATTED_ADDRESS1);
    address1.setWelshFormattedAddress(WELSH_FORMATTED_ADDRESS1);
    address1.setRegion(COUNTRY_CODE);
    address1.setAddressType(ADDRESS_TYPE);
    address1.setEstabType(ESTAB_TYPE);
    address1.setEstabDescription(ESTAB_DESCRIPTION);

    AddressDTO address2 = new AddressDTO();
    address2.setUprn(UPRN2);
    address2.setFormattedAddress(FORMATTED_ADDRESS2);
    address2.setWelshFormattedAddress(WELSH_FORMATTED_ADDRESS2);
    address2.setRegion(COUNTRY_CODE);
    address2.setAddressType(ADDRESS_TYPE);
    address2.setEstabType(ESTAB_TYPE);
    address2.setEstabDescription(ESTAB_DESCRIPTION);

    AddressQueryResponseDTO addresses = new AddressQueryResponseDTO();
    addresses.setDataVersion(DATA_VERSION);
    addresses.setAddresses(Lists.newArrayList(address1, address2));
    addresses.setTotal(2);

    Mockito.when(addressService.addressQuery(any())).thenReturn(addresses);

    ResultActions actions = mockMvc.perform(get("/addresses?input=Parks"));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.dataVersion", is(DATA_VERSION)));
    actions.andExpect(jsonPath("$.addresses[0].uprn", is(UPRN1)));
    actions.andExpect(jsonPath("$.addresses[0].formattedAddress", is(FORMATTED_ADDRESS1)));
    actions.andExpect(
        jsonPath("$.addresses[0].welshFormattedAddress", is(WELSH_FORMATTED_ADDRESS1)));
    actions.andExpect(jsonPath("$.addresses[0].region", is(COUNTRY_CODE)));
    actions.andExpect(jsonPath("$.addresses[0].addressType", is(ADDRESS_TYPE)));
    actions.andExpect(jsonPath("$.addresses[0].estabType", is(ESTAB_TYPE)));
    actions.andExpect(jsonPath("$.addresses[0].estabDescription", is(ESTAB_DESCRIPTION)));
    actions.andExpect(jsonPath("$.addresses[1].uprn", is(UPRN2)));
    actions.andExpect(jsonPath("$.addresses[1].formattedAddress", is(FORMATTED_ADDRESS2)));
    actions.andExpect(
        jsonPath("$.addresses[1].welshFormattedAddress", is(WELSH_FORMATTED_ADDRESS2)));
    actions.andExpect(jsonPath("$.addresses[1].region", is(COUNTRY_CODE)));
    actions.andExpect(jsonPath("$.addresses[1].addressType", is(ADDRESS_TYPE)));
    actions.andExpect(jsonPath("$.addresses[1].estabType", is(ESTAB_TYPE)));
    actions.andExpect(jsonPath("$.addresses[1].estabDescription", is(ESTAB_DESCRIPTION)));
    actions.andExpect(jsonPath("$.total", is(2)));
  }
}
