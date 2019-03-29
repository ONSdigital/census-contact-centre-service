package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Contact Centre Data Endpoint Unit tests */
public final class CaseEndpointAppointmentTest {

  private static final String APPOINTMENT_TYPE = "appointmentType";
  private static final String TEL_NO = "telNo";
  private static final String TITLE = "title";
  private static final String FORENAME = "forename";
  private static final String SURNAME = "surname";
  private static final String DATE_TIME = "dateTime";

  private static final String RESPONSE_DATE_TIME = "2019-03-28T11:56:40.705340";
  
  @Mock private CaseService caseService;

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
  public void appointmentBadId() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    ResultActions actions =
        mockMvc.perform(postJson("/cases/123456789/appointment", json.toString()));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void appointmentGoodRequest() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    assertOk(json);
  }

  @Test
  public void appointmentTelNoNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TEL_NO, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void appointmentTelNoBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TEL_NO, "");
    assertBadRequest(json);
  }

  @Test
  public void appointmentTelNoTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TEL_NO, "07968583119119119119119");
    assertBadRequest(json);
  }

  @Test
  public void appointmentTypeNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(APPOINTMENT_TYPE, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void appointmentTypeBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(APPOINTMENT_TYPE, "");
    assertBadRequest(json);
  }

  @Test
  public void appointmentTypeBad() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(APPOINTMENT_TYPE, "PERSONAL");
    assertBadRequest(json);
  }

  @Test
  public void appointmentTitleNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TITLE, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void appointmentTitleBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TITLE, "");
    assertBadRequest(json);
  }

  @Test
  public void appointmentTitleTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(TITLE, "Mrrrrrrrrrrrr");
    assertBadRequest(json);
  }

  @Test
  public void appointmentForenameNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(FORENAME, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void appointmentForenameBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(FORENAME, "");
    assertBadRequest(json);
  }

  @Test
  public void appointmentForenameTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(FORENAME, "Phillllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
    assertBadRequest(json);
  }

  @Test
  public void appointmentSurnameNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(SURNAME, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void appointmentSurnameBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(SURNAME, "");
    assertBadRequest(json);
  }

  @Test
  public void appointmentSurnameTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(SURNAME, "Whilessssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    assertBadRequest(json);
  }

  @Test
  public void appointmentUnresolvedFulfilmentDateTimeNull() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, (String) null);
    assertBadRequest(json);
  }

  @Test
  public void appointmentUnresolvedFulfilmentDateTimeBlank() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, "");
    assertBadRequest(json);
  }

  @Test
  public void appointmentUnresolvedFulfilmentDateTimeTooLong() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode();
    json.put(DATE_TIME, "2007:12:03T10-15-30");
    assertBadRequest(json);
  }

  private void assertOk(ObjectNode json) throws Exception {
    ResponseDTO responseDTO = ResponseDTO.builder()
        .id(uuid.toString())
        .dateTime(RESPONSE_DATE_TIME)
        .build();
    Mockito.when(caseService.makeAppointment(any(), any())).thenReturn(responseDTO);
    
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/appointment", json.toString()));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(uuid.toString())));
    actions.andExpect(jsonPath("$.dateTime", is(RESPONSE_DATE_TIME)));
  }

  private void assertBadRequest(ObjectNode json) throws Exception {
    ResultActions actions =
        mockMvc.perform(postJson("/cases/" + uuid + "/appointment", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}
