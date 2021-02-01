package uk.gov.ons.ctp.integration.contactcentresvc;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.godaddy.logging.LoggingConfigs;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import java.time.Duration;
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
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.ctp.common.cloud.CloudRetryListener;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.SpringRabbitEventSender;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.rest.RestClientConfig;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.MessagingConfig.PublishConfig;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;
import uk.gov.ons.ctp.integration.eqlaunch.service.impl.EqLaunchServiceImpl;

/** The 'main' entry point for the ContactCentre Svc SpringBoot Application. */
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration", "uk.gov.ons.ctp.common"})
@EnableCaching
public class ContactCentreSvcApplication {
  private static final Logger log = LoggerFactory.getLogger(ContactCentreSvcApplication.class);

  private AppConfig appConfig;

  @Value("${queueconfig.event-exchange}")
  private String eventExchange;

  @Value("${management.metrics.export.stackdriver.project-id}")
  private String stackdriverProjectId;

  @Value("${management.metrics.export.stackdriver.enabled}")
  private boolean stackdriverEnabled;

  @Value("${management.metrics.export.stackdriver.step}")
  private String stackdriverStep;

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
  public RestClient addressIndexClient() throws CTPException {
    RestClientConfig clientConfig = appConfig.getAddressIndexSettings().getRestClientConfig();
    RestClient restHelper = new RestClient(clientConfig, httpErrorMapping, defaultHttpStatus);
    return restHelper;
  }

  @Bean
  @Qualifier("caseServiceClient")
  public CaseServiceClientServiceImpl caseServiceClient() throws CTPException {
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

  @Bean
  public RabbitTemplate rabbitTemplate(
      final ConnectionFactory connectionFactory, RetryTemplate sendRetryTemplate) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange("events");
    template.setChannelTransacted(true);
    template.setRetryTemplate(sendRetryTemplate);
    return template;
  }

  @Bean
  public RetryTemplate sendRetryTemplate(RetryListener sendRetryListener) {
    RetryTemplate template = new RetryTemplate();
    template.registerListener(sendRetryListener);
    PublishConfig publishConfig = appConfig.getMessaging().getPublish();
    template.setRetryPolicy(new SimpleRetryPolicy(publishConfig.getMaxAttempts()));
    return template;
  }

  @Bean
  public RetryListener sendRetryListener() {
    return new CloudRetryListener() {
      @Override
      public <T, E extends Throwable> boolean open(
          RetryContext context, RetryCallback<T, E> callback) {
        context.setAttribute(RetryContext.NAME, "publish-event");
        return true;
      }
    };
  }

  /**
   * Bean used to publish asynchronous event messages
   *
   * @param rabbitTemplate rabbit template
   * @param eventPersistence event persistence object
   * @param circuitBreakerFactory circuit breaker factory
   * @return event publisher bean
   */
  @Bean
  public EventPublisher eventPublisher(
      final RabbitTemplate rabbitTemplate,
      final FirestoreEventPersistence eventPersistence,
      final Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    EventSender sender = new SpringRabbitEventSender(rabbitTemplate);
    CircuitBreaker circuitBreaker = circuitBreakerFactory.create("eventSendCircuitBreaker");
    return EventPublisher.createWithEventPersistence(sender, eventPersistence, circuitBreaker);
  }

  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomiser() {
    CustomCircuitBreakerConfig config = appConfig.getCircuitBreaker();
    log.info("Circuit breaker configuration: {}", config);
    return config.defaultCircuitBreakerCustomiser();
  }

  /**
   * Bean to allow CC service to call the eqlauncher.
   *
   * @return a EqLauncherServer instance.
   */
  @Bean
  public EqLaunchService eqLaunchService() throws CTPException {
    return new EqLaunchServiceImpl(appConfig.getKeystore());
  }

  @Value("#{new Boolean('${logging.useJson}')}")
  private boolean useJsonLogging;

  @PostConstruct
  public void initJsonLogging() {
    if (useJsonLogging) {
      LoggingConfigs.setCurrent(LoggingConfigs.getCurrent().useJson());
    }
  }

  @Bean
  StackdriverConfig stackdriverConfig() {
    return new StackdriverConfig() {
      @Override
      public Duration step() {
        return Duration.parse(stackdriverStep);
      }

      @Override
      public boolean enabled() {
        return stackdriverEnabled;
      }

      @Override
      public String projectId() {
        return stackdriverProjectId;
      }

      @Override
      public String get(String key) {
        return null;
      }
    };
  }

  @Bean
  public MeterFilter meterFilter() {
    return new MeterFilter() {
      @Override
      public MeterFilterReply accept(Meter.Id id) {
        // RM use this to remove Rabbit clutter from the metrics as they have alternate means of
        // monitoring it
        // We will probable want to experiment with removing this to see what value we get from
        // rabbit metrics
        // a) once we have Grafana setup, and b) once we try out micrometer in a perf environment
        if (id.getName().startsWith("rabbitmq")) {
          return MeterFilterReply.DENY;
        }
        return MeterFilterReply.NEUTRAL;
      }
    };
  }

  @Bean
  StackdriverMeterRegistry meterRegistry(StackdriverConfig stackdriverConfig) {

    StackdriverMeterRegistry.builder(stackdriverConfig).build();
    return StackdriverMeterRegistry.builder(stackdriverConfig).build();
  }
}
