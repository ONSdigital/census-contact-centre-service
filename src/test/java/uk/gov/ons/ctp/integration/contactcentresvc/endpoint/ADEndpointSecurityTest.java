package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

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
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-ad")
public class ADEndpointSecurityTest extends EndpointSecurityTest {

  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @MockBean CaseService caseService;
  @MockBean AddressService addressService;
  @MockBean CaseDataRepository caseDataRepository;

  @Test
  public void adOkGetUACForCase() {
    testGetUACForCase(HttpStatus.OK);
  }

  @Test
  public void adOkAccessCasesByUPRN() {
    testAccessCasesByUPRN(HttpStatus.OK);
  }

  @Test
  public void adOkGetAddresses() {
    testGetAddresses(HttpStatus.OK);
  }

  @Test
  public void adOkGetAddressesPostcode() {
    testGetAddressesPostcode(HttpStatus.OK);
  }

  @Test
  public void adForbiddenPostRefusal() {
    testPostRefusal(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostInvalidate() {
    testPostInvalidateCase(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPutCase() {
    testPutCase(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCCSCaseByPostcode() {
    testGetCCSCaseByPostcode(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseByCaseId() {
    testGetCaseByCaseId(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseByCaseRef() {
    testGetCaseByCaseRef(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetCaseLaunch() {
    testGetCaseLaunch(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenGetFulfilfments() {
    testGetFulfilfments(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostFulfilfmentPost() {
    testPostFulfilmentPost(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adForbiddenPostFulfilfmentSMS() {
    testPostFulfilmentSMS(HttpStatus.FORBIDDEN);
  }

  @Test
  public void adOkPostCase() {
    testPostCase(HttpStatus.FORBIDDEN);
  }
}
