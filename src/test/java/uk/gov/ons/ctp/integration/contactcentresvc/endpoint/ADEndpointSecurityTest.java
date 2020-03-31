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
@ActiveProfiles("test-ad")
public class ADEndpointSecurityTest extends EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @MockBean CaseService caseService;

  @Test
  public void adOkGetUACForCase() throws IllegalStateException, IOException {
    testGetUACForCase(HttpStatus.OK);
  }

  @Test
  public void adOkAccessCasesByUPRN() throws IllegalStateException, IOException {
    testAccessCasesByUPRN(HttpStatus.OK);
  }

  @Test
  public void adOkGetAddresses() throws IllegalStateException, IOException {
    testGetAddresses(HttpStatus.OK);
  }

  @Test
  public void adOkGetAddressesPostcode() throws IllegalStateException, IOException {
    testGetAddressesPostcode(HttpStatus.OK);
  }

  @Test
  public void adForbiddenPostRefusal() throws IllegalStateException, IOException {
    testPostRefusal(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPutCase() throws IllegalStateException, IOException {
    testPutCase(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCCSCaseByPostcode() throws IllegalStateException, IOException {
    testGetCCSCaseByPostcode(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseByCaseId() throws IllegalStateException, IOException {
    testGetCaseByCaseId(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseByCaseRef() throws IllegalStateException, IOException {
    testGetCaseByCaseRef(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseLaunch() throws IllegalStateException, IOException {
    testGetCaseLaunch(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetFulfilfments() throws IllegalStateException, IOException {
    testGetFulfilfments(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostFulfilfmentPost() throws IllegalStateException, IOException {
    testPostFulfilmentPost(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostFulfilfmentSMS() throws IllegalStateException, IOException {
    testPostFulfilmentSMS(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adOkPostCase() throws IllegalStateException, IOException {
    testPostCase(HttpStatus.FORBIDDEN);
  }
}
