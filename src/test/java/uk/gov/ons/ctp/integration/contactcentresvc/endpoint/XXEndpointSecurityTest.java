package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-xx")
public class XXEndpointSecurityTest extends EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @MockBean CaseService caseService;

  @Test
  public void xxForbiddenGetUACForCase() throws IllegalStateException, IOException {
    testGetUACForCase(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenAccessCasesByUPRN() throws IllegalStateException, IOException {
    testAccessCasesByUPRN(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetAddresses() throws IllegalStateException, IOException {
    testGetAddresses(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetAddressesPostcode() throws IllegalStateException, IOException {
    testGetAddressesPostcode(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenPostRefusal() throws IllegalStateException, IOException {
    testPostRefusal(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenPutCase() throws IllegalStateException, IOException {
    testPutCase(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetCCSCaseByPostcode() throws IllegalStateException, IOException {
    testGetCCSCaseByPostcode(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetCaseByCaseId() throws IllegalStateException, IOException {
    testGetCaseByCaseId(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetCaseByCaseRef() throws IllegalStateException, IOException {
    testGetCaseByCaseRef(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetCaseLaunch() throws IllegalStateException, IOException {
    testGetCaseLaunch(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenGetFulfilfments() throws IllegalStateException, IOException {
    testGetFulfilfments(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenPostFulfilfmentPost() throws IllegalStateException, IOException {
    testPostFulfilmentPost(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxForbiddenPostFulfilfmentSMS() throws IllegalStateException, IOException {
    testPostFulfilmentSMS(HttpStatus.FORBIDDEN);
  }

  @Test
  public void xxOkPostCase() throws IllegalStateException, IOException {
    testPostCase(HttpStatus.FORBIDDEN);
  }
}
