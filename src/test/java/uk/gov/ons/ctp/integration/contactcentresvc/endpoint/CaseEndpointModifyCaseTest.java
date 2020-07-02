package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.putJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ModifyCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Test the PUT endpoint to modify case details. */
@RunWith(MockitoJUnitRunner.class)
public final class CaseEndpointModifyCaseTest {

  @Mock private CaseService caseService;

  @InjectMocks private CaseEndpoint caseEndpoint;

  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  private ObjectNode json;
  private CaseDTO responseDTO;
  private String caseId;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();

    this.json = FixtureHelper.loadClassObjectNode();
    ModifyCaseRequestDTO dto = mapper.convertValue(json, ModifyCaseRequestDTO.class);
    this.caseId = dto.getCaseId().toString();
    createValidResponse(dto);
  }

  private void createValidResponse(ModifyCaseRequestDTO dto) {
    this.responseDTO = CaseDTO.builder().id(dto.getCaseId()).build();
  }

  private ResultActions doPut() throws Exception {
    return mockMvc.perform(putJson("/cases/" + caseId, json.toString()));
  }

  private void doPutExpectingRejection() throws Exception {
    ResultActions actions = doPut();
    actions.andExpect(status().isBadRequest());
    verify(caseService, never()).modifyCase(any());
  }

  private ResultActions doPutExpectingOk() throws Exception {
    when(caseService.modifyCase(any())).thenReturn(responseDTO);
    ResultActions actions = doPut();
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(caseId)));
    verify(caseService).modifyCase(any());
    return actions;
  }

  @Test
  public void shouldModifyCase() throws Exception {
    doPutExpectingOk();
  }

  @Test
  public void shouldAcceptMaxAddressLines() throws Exception {
    json.put("addressLine1", StringUtils.repeat('x', 60));
    json.put("addressLine2", StringUtils.repeat('y', 60));
    json.put("addressLine3", StringUtils.repeat('z', 60));
    doPutExpectingOk();
  }

  @Test
  public void shouldAcceptMissingAddressLine2() throws Exception {
    json.remove("addressLine2");
    doPutExpectingOk();
  }

  @Test
  public void shouldAcceptMissingAddressLine3() throws Exception {
    json.remove("addressLine3");
    doPutExpectingOk();
  }

  @Test
  public void shouldAcceptMissingCeOrgName() throws Exception {
    json.remove("ceOrgName");
    doPutExpectingOk();
  }

  @Test
  public void shouldAcceptMissingCeUsualResidents() throws Exception {
    json.remove("ceUsualResidents");
    doPutExpectingOk();
  }

  @Test
  public void shouldRejectBadPathCaseId() throws Exception {
    caseId = "123456789";
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadBodyCaseId() throws Exception {
    json.put("caseId", "foo");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMismatchingCaseId() throws Exception {
    json.put("caseId", UUID.randomUUID().toString());
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadDate() throws Exception {
    json.put("dateTime", "2019:12:25 12:34:56");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadEstabType() throws Exception {
    json.put("estabType", "FOO");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectBadCaseType() throws Exception {
    json.put("caseType", "FOO");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingCaseId() throws Exception {
    json.remove("caseId");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingAddressLine1() throws Exception {
    json.remove("addressLine1");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectTooLongAddressLine1() throws Exception {
    json.put("addressLine1", StringUtils.repeat('x', 61));
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectTooLongAddressLine2() throws Exception {
    json.put("addressLine2", StringUtils.repeat('x', 61));
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectTooLongAddressLine3() throws Exception {
    json.put("addressLine3", StringUtils.repeat('x', 61));
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectTooLongCeOrgName() throws Exception {
    json.put("ceOrgName", StringUtils.repeat('x', 61));
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingCaseType() throws Exception {
    json.remove("caseType");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingEstabType() throws Exception {
    json.remove("estabType");
    doPutExpectingRejection();
  }

  @Test
  public void shouldRejectMissingDateTime() throws Exception {
    json.remove("dateTime");
    doPutExpectingRejection();
  }
}
