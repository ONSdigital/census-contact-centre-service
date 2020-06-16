package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.NewCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

public final class CaseEndpointPostNewCaseTest {

  @Mock private CaseService caseService;

  @InjectMocks private CaseEndpoint caseEndpoint;

  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  /**
   * Set up of tests.
   *
   * @throws Exception if anything goes wrong.
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
  public void postNewCase() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(0);

    UUID newId = UUID.randomUUID();
    CaseDTO responseDTO = CaseDTO.builder().id(newId).build();
    Mockito.when(caseService.createCaseForNewAddress(any())).thenReturn(responseDTO);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isOk());
    actions.andExpect(jsonPath("$.id", is(newId.toString())));
  }

  @Test
  public void postNewCase_essentialFieldsOnly() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isOk());
  }

  @Test
  public void postNewCase_missingRegion() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setRegion(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_missingAddressLine1() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setAddressLine1(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_missingTownName() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setTownName(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_missingPostcode() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setPostcode(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_missingDateTime() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setDateTime(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_missingCaseType() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setCaseType(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_missingEstabType() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setEstabType(null);

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_badPostcode() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    newCaseRequestDTO.setPostcode("XYZ 123");

    String jsonString = mapper.writeValueAsString(newCaseRequestDTO);
    ResultActions actions = mockMvc.perform(postJson("/cases", jsonString));
    actions.andExpect(status().isBadRequest());
  }

  @Test
  public void postNewCase_BadDate() throws Exception {
    NewCaseRequestDTO newCaseRequestDTO =
        FixtureHelper.loadClassFixtures(NewCaseRequestDTO[].class).get(1);
    ObjectNode json = mapper.convertValue(newCaseRequestDTO, ObjectNode.class);
    json.put("dateTime", "2019:12:25 12:34:56");

    ResultActions actions = mockMvc.perform(postJson("/cases", json.toString()));
    actions.andExpect(status().isBadRequest());
  }
}
