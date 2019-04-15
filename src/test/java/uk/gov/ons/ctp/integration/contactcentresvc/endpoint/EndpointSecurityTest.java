package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @Before
  public void setUp() throws MalformedURLException {
    restTemplate = new TestRestTemplate("serco_cks", "t1nyr3b3l");
    base = new URL("http://localhost:" + port);
  }

  @Test
  public void whenLoggedUserRequestsInfoPageThenSuccess()
      throws IllegalStateException, IOException {
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("contactcentresvc"));
  }

  @Test
  public void whenAnonymousUserRequestsInfoPageThenSuccess()
      throws IllegalStateException, IOException {
    restTemplate = new TestRestTemplate();
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/info", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().contains("contactcentresvc"));
  }

  @Test
  public void whenUserWithWrongCredentialsRequestCaseThenUnauthorizedPage() throws Exception {

    restTemplate = new TestRestTemplate("user", "wrongpassword");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/uprn/123", String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody().contains("Unauthorized"));
  }

  @Test
  public void whenUserWithCorrectCredentialsRequestCaseThenSuccess() throws Exception {

    restTemplate = new TestRestTemplate("serco_cks", "t1nyr3b3l");
    ResponseEntity<String> response =
        restTemplate.getForEntity(base.toString() + "/cases/uprn/123", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
