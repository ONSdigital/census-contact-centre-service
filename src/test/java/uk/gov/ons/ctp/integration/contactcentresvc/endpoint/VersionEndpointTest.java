package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.hamcrest.Matchers.matchesRegex;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

/** Contact Centre Data endpoint Unit test. */
@RunWith(MockitoJUnitRunner.class)
public final class VersionEndpointTest {
  private static final String SWAGGER_HEADER = "info:\n  version: \"5.10.7-oas3\"";

  @Mock private ResourceLoader resourceLoader;
  @Mock private Resource resource;
  @InjectMocks private VersionEndpoint versionEndpoint;

  private MockMvc mockMvc;

  @Before
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(versionEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void validRequestRespondsWithVersionNumber() throws Exception {
    InputStream is = new ByteArrayInputStream(SWAGGER_HEADER.getBytes());

    when(resourceLoader.getResource(anyString())).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(is);
    versionEndpoint.readSwaggerVersion();

    mockMvc
        .perform(get("/version"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiVersion", matchesRegex("^5\\.10\\.7$")));
  }
}
