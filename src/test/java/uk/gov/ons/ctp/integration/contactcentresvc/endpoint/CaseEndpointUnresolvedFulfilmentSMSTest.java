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
public final class CaseEndpointUnresolvedFulfilmentSMSTest {

  private static final String TEL_NO = "telNo";
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
  public void smsUnresolvedFulfilmentGoodRequest() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentTelNoNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TEL_NO, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentTelNoBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TEL_NO, "");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentTelNoTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TEL_NO, "07968583119119119119119");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine1Null() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_1, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine1Blank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_1, "");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine1TooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_1, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine2Null() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_2, (String) null);
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine2Blank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_2, "");
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine2TooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_2, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine3Null() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_3, (String) null);
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine3Blank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_3, "");
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine3TooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_3, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine4Null() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_4, (String) null);
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine4Blank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_4, "");
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentAddressLine4TooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(ADDRESS_LINE_4, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentTownNameNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TOWN_NAME, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentTownNameBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TOWN_NAME, "");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentTownNameTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TOWN_NAME, "Addressssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentRegionNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(REGION, (String) null);
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentRegionBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(REGION, "");
    assertOk(json);
  }

  @Test
  public void smsUnresolvedFulfilmentRegionBad() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(REGION, "X");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentPostcodeNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(POSTCODE, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentPostcodeBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(POSTCODE, "");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentPostcodeBad() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(POSTCODE, "SO100 100HJ");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentProductCodeNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(PRODUCT_CODE, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentProductCodeBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(PRODUCT_CODE, "");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentProductCodeTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(PRODUCT_CODE, "EN12345");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentDateTimeNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentDateTimeBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, "");
    assertBadRequest(json);
  }

  @Test
  public void smsUnresolvedFulfilmentDateTimeTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, "2007:12:03T10-15-30");
    assertBadRequest(json);
  }

  private void assertOk(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/unresolved/fulfilment/sms", json.toString()));
    actions.andExpect(status().isOk());
  }

  private void assertBadRequest(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/unresolved/fulfilment/sms", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}