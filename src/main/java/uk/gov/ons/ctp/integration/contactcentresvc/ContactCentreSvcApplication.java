package uk.gov.ons.ctp.integration.contactcentresvc;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.rest.RestClientConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

/** The 'main' entry point for the ContactCentre Svc SpringBoot Application. */
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
@ImportResource("springintegration/main.xml")
public class ContactCentreSvcApplication {

  private AppConfig appConfig;

  // Table to convert from AddressIndex response status values to values that can be returned to the
  // invoker of this service
  private static final HashMap<HttpStatus, HttpStatus> httpErrorMapping;

  static {
    httpErrorMapping = new HashMap<HttpStatus, HttpStatus>();
    httpErrorMapping.put(HttpStatus.OK, HttpStatus.OK);
    httpErrorMapping.put(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND);
    httpErrorMapping.put(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.GATEWAY_TIMEOUT, HttpStatus.INTERNAL_SERVER_ERROR);
    httpErrorMapping.put(HttpStatus.REQUEST_TIMEOUT, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // This is the http status to be used for error mapping if a status is not in the mapping table
  HttpStatus defaultHttpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

  /** Constructor for ContactCentreSvcApplication */
  @Autowired
  public ContactCentreSvcApplication(final AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Bean
  @Qualifier("addressIndexClient")
  public RestClient addressIndexClient() {
    RestClientConfig clientConfig = appConfig.getAddressIndexSettings().getRestClientConfig();
    RestClient restHelper = new RestClient(clientConfig, httpErrorMapping, defaultHttpStatus);
    return restHelper;
  }

  /**
   * The main entry point for this application.
   *
   * @param args runtime command line args
   */
  public static void main(final String[] args) {

    SpringApplication.run(ContactCentreSvcApplication.class, args);
  }

  //  @EnableWebSecurity
  //  public static class SecurityConfig extends WebSecurityConfigurerAdapter {
  //
  //    @Value("${spring.security.user.name}")
  //    String username;
  //
  //    @Value("${spring.security.user.password}")
  //    String password;
  //
  //    @Override
  //    protected void configure(HttpSecurity http) throws Exception {
  //      http
  //        .authorizeRequests().antMatchers("/info").permitAll()
  //        .anyRequest().authenticated().and()
  //        .csrf().disable().httpBasic();
  //    }
  //
  //    @Override
  //    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
  //        auth.inMemoryAuthentication()
  //            .passwordEncoder(passwordEncoder())
  //            .withUser("user").password(ENCODED_PASSWORD).roles("USER");
  //    }
  ////    @Bean
  ////    public UserDetailsService userDetailsService() {
  ////        @SuppressWarnings("deprecation")
  ////        User.UserBuilder users = User.withDefaultPasswordEncoder();
  ////        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
  ////        manager.createUser(users.username(username).password(password).roles("USER").build());
  ////        return manager;
  ////
  ////    }
  //  }
  //
  //  @Bean
  //  public PasswordEncoder passwordEncoder() {
  //      return new BCryptPasswordEncoder();
  //  }

  /**
   * The restTemplate bean injected in REST client classes
   *
   * @return the restTemplate used in REST calls
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  /**
   * Custom Object Mapper
   *
   * @return a customer object mapper
   */
  @Bean
  @Primary
  public CustomObjectMapper customObjectMapper() {
    return new CustomObjectMapper();
  }

  /**
   * Bean used to map exceptions for endpoints
   *
   * @return the service client
   */
  @Bean
  public RestExceptionHandler restExceptionHandler() {
    return new RestExceptionHandler();
  }
}
