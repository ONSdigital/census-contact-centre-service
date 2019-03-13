package uk.gov.ons.ctp.integration.contactcentresvc;

import java.util.HashMap;
// import com.godaddy.logging.LoggingConfigs;
// import java.time.Clock;
// import javax.annotation.PostConstruct;
// import net.sourceforge.cobertura.CoverageIgnore;
// import org.redisson.Redisson;
// import org.redisson.api.RedissonClient;
// import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.rest.RestClientConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

/** The 'main' entry point for the ContactCentre Svc SpringBoot Application. */
// @ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
// @EntityScan("uk.gov.ons.ctp.integration")
@SpringBootApplication
public class ContactCentreSvcApplication {

  private AppConfig appConfig;

  // Table to convert from AddressIndex response status values to values that can be returned to the
  // invoker of this service
  private static final HashMap<HttpStatus, HttpStatus> httpErrorMapping;
  static {
    httpErrorMapping = new HashMap<HttpStatus, HttpStatus>();
    httpErrorMapping.put(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.GATEWAY_TIMEOUT, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.REQUEST_TIMEOUT, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /** Constructor for ContactCentreSvcApplication */
  @Autowired
  public ContactCentreSvcApplication(final AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Bean
  @Qualifier("addressIndexClient")
  public RestClient addressIndexClient() {
    RestClientConfig clientConfig = appConfig.getAddressIndexSettings().getRestClientConfig();
    RestClient restHelper = new RestClient(clientConfig, httpErrorMapping);
    return restHelper;
  }

  /**
   * The main entry point for this applicaion.
   *
   * @param args runtime command line args
   */
  public static void main(final String[] args) {

    SpringApplication.run(ContactCentreSvcApplication.class, args);
  }

  /**
   * The restTemplate bean injected in REST client classes
   *
   * @return the restTemplate used in REST calls
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
