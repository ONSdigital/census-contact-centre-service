package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
public final class CaseEndpointUnresolvedFulfilmentPostTest {

  private static final String TITLE = "title";
  private static final String FORENAME = "forename";
  private static final String SURNAME = "surname";
  private static final String ADDRESS_LINE_1 = "addressLine1";
  private static final String ADDRESS_LINE_2 = "addressLine2";
  private static final String ADDRESS_LINE_3 = "addressLine3";
  private static final String ADDRESS_LINE_4 = "addressLine4";
  private static final String TOWN_NAME = "townName";
  private static final String REGION = "region";
  private static final String POSTCODE = "postcode";
  private static final String PRODUCT_CODE = "productCode";
  private static final String DATE_TIME = "dateTime";

  @Mock private EventServiceImpl eventSvc;

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
  public void postUnresolvedFulfilmentGoodRequest() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    assertOk(json);
  }

  @Test
  public void postUnresolvedFulfilmentTitleNull() throws Exception {
    assertBadRequest(TITLE, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentTitleBlank() throws Exception {
    assertBadRequest(TITLE, "");
  }

  @Test
  public void postUnresolvedFulfilmentTitleTooLong() throws Exception {
    assertBadRequest(TITLE, "Mrrrrrrrrrrrr");
  }

  @Test
  public void postUnresolvedFulfilmentForenameNull() throws Exception {
    assertBadRequest(FORENAME, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentForenameBlank() throws Exception {
    assertBadRequest(FORENAME, "");
  }

  @Test
  public void postUnresolvedFulfilmentForenameTooLong() throws Exception {
    assertBadRequest(FORENAME, "Phillllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
  }

  @Test
  public void postUnresolvedFulfilmentSurnameNull() throws Exception {
    assertBadRequest(SURNAME, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentSurnameBlank() throws Exception {
    assertBadRequest(SURNAME, "");
  }

  @Test
  public void postUnresolvedFulfilmentSurnameTooLong() throws Exception {
    assertBadRequest(SURNAME, "Whilessssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine1Null() throws Exception {
    assertBadRequest(ADDRESS_LINE_1, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine1Blank() throws Exception {
    assertBadRequest(ADDRESS_LINE_1, "");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine1TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_1, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine2Null() throws Exception {
    assertOk(ADDRESS_LINE_2, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine2Blank() throws Exception {
    assertOk(ADDRESS_LINE_2, "");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine2TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_2, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine3Null() throws Exception {
    assertOk(ADDRESS_LINE_3, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine3Blank() throws Exception {
    assertOk(ADDRESS_LINE_3, "");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine3TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_3, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine4Null() throws Exception {
    assertOk(ADDRESS_LINE_4, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine4Blank() throws Exception {
    assertOk(ADDRESS_LINE_4, "");
  }

  @Test
  public void postUnresolvedFulfilmentAddressLine4TooLong() throws Exception {
    assertBadRequest(
        ADDRESS_LINE_4, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void postUnresolvedFulfilmentTownNameNull() throws Exception {
    assertBadRequest(TOWN_NAME, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentTownNameBlank() throws Exception {
    assertBadRequest(TOWN_NAME, "");
  }

  @Test
  public void postUnresolvedFulfilmentTownNameTooLong() throws Exception {
    assertBadRequest(TOWN_NAME, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
  }

  @Test
  public void postUnresolvedFulfilmentRegionNull() throws Exception {
    assertOk(REGION, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentRegionBlank() throws Exception {
    assertOk(REGION, "");
  }

  @Test
  public void postUnresolvedFulfilmentRegionBad() throws Exception {
    assertBadRequest(REGION, "X");
  }

  @Test
  public void postUnresolvedFulfilmentPostcodeNull() throws Exception {
    assertBadRequest(POSTCODE, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentPostcodeBlank() throws Exception {
    assertBadRequest(POSTCODE, "");
  }

  @Test
  public void postUnresolvedFulfilmentPostcodeBad() throws Exception {
    assertBadRequest(POSTCODE, "SO100 100HJ");
  }

  @Test
  public void postUnresolvedFulfilmentProductCodeNull() throws Exception {
    assertBadRequest(PRODUCT_CODE, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentProductCodeBlank() throws Exception {
    assertBadRequest(PRODUCT_CODE, "");
  }

  @Test
  public void postUnresolvedFulfilmentProductCodeTooLong() throws Exception {
    assertBadRequest(PRODUCT_CODE, "EN12345");
  }

  @Test
  public void postUnresolvedFulfilmentDateTimeNull() throws Exception {
    assertBadRequest(DATE_TIME, (String) null);
  }

  @Test
  public void postUnresolvedFulfilmentDateTimeBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, "");
    assertBadRequest(json);
  }

  @Test
  public void postUnresolvedFulfilmentDateTimeTooLong() throws Exception {
    assertBadRequest(DATE_TIME, "2007:12:03T10-15-30");
  }

  private void assertOk(String field, String value) throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(field, value);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/unresolved/fulfilment/post", json.toString()));
    actions.andExpect(status().isOk());
  }

  private void assertBadRequest(String field, String value) throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(field, value);
    ResultActions actions =
        mockMvc.perform(postJson("/cases/unresolved/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  private void assertOk(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/unresolved/fulfilment/post", json.toString()));
    actions.andExpect(status().isOk());
  }

  private void assertBadRequest(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/unresolved/fulfilment/post", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}
