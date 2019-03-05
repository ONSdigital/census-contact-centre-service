package uk.gov.ons.ctp.integration.contactcentresvc;

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
import org.springframework.web.client.RestTemplate;

import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

/** The 'main' entry point for the ContactCentre Svc SpringBoot Application. */
// @ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
// @EntityScan("uk.gov.ons.ctp.integration")
@SpringBootApplication
public class ContactCentreSvcApplication {

  private AppConfig appConfig;

  /** Constructor for ContactCentreSvcApplication */
  @Autowired
  public ContactCentreSvcApplication(final AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Bean
  @Qualifier("addressIndexClient")
  public RestClient addressIndexClient() {
	  RestClient restHelper = new RestClient(appConfig.getAddressIndexSettings().getRestClientConfig());
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
