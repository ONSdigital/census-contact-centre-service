package uk.gov.ons.ctp.integration.contactcentresvc;

import com.godaddy.logging.LoggingConfigs;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.SpringRabbitEventSender;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.rest.RestClientConfig;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;
import uk.gov.ons.ctp.integration.eqlaunch.service.impl.EqLaunchServiceImpl;

/** The 'main' entry point for the ContactCentre Svc SpringBoot Application. */
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration", "uk.gov.ons.ctp.common"})
@ImportResource("springintegration/main.xml")
@EnableCaching
public class ContactCentreSvcApplication {

  private AppConfig appConfig;

  @Value("${queueconfig.event-exchange}")
  private String eventExchange;

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

  /**
   * Constructor for ContactCentreSvcApplication
   *
   * @param appConfig contains the configuration for the current deployment.
   */
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

  @Bean
  @Qualifier("caseServiceClient")
  public CaseServiceClientServiceImpl caseServiceClient() {
    RestClientConfig clientConfig = appConfig.getCaseServiceSettings().getRestClientConfig();
    RestClient restHelper = new RestClient(clientConfig, httpErrorMapping, defaultHttpStatus);
    CaseServiceClientServiceImpl csClientServiceImpl = new CaseServiceClientServiceImpl(restHelper);
    return csClientServiceImpl;
  }

  /**
   * The main entry point for this application.
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
   * Bean used to publish asynchronous event messages
   *
   * @param connectionFactory RabbitMQ connection settings and strategies
   * @return the event publisher
   */
  @Bean
  public EventPublisher eventPublisher(
      final ConnectionFactory connectionFactory, final FirestoreEventPersistence eventPersistence) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange("events");
    template.setChannelTransacted(true);

    EventSender sender = new SpringRabbitEventSender(template);
    return new EventPublisher(sender, eventPersistence);
  }

  /**
   * Bean to allow CC service to call the eqlauncher.
   *
   * @return a EqLauncherServer instance.
   */
  @Bean
  public EqLaunchService eqLaunchService() {
    return new EqLaunchServiceImpl();
  }

  @Value("#{new Boolean('${logging.useJson}')}")
  private boolean useJsonLogging;

  @PostConstruct
  public void initJsonLogging() {
    if (useJsonLogging) {
      LoggingConfigs.setCurrent(LoggingConfigs.getCurrent().useJson());
    }
  }
}
