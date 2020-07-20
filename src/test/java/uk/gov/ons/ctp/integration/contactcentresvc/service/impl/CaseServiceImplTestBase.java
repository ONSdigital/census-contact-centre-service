package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import ma.glasnost.orika.MapperFacade;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.EventPayload;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.CaseServiceClientServiceImpl;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.CaseDataRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.service.AddressService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;
import uk.gov.ons.ctp.integration.eqlaunch.service.impl.EqLaunchServiceImpl;

public abstract class CaseServiceImplTestBase {
  @Spy AppConfig appConfig = new AppConfig();

  @Mock ProductReference productReference;

  @Mock CaseServiceClientServiceImpl caseServiceClient;

  @Mock EqLaunchService eqLaunchService = new EqLaunchServiceImpl();

  @Mock EventPublisher eventPublisher;

  @Spy MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Mock CaseDataRepository dataRepo;

  @Mock AddressService addressSvc;

  static final List<DeliveryChannel> ALL_DELIVERY_CHANNELS =
      List.of(DeliveryChannel.POST, DeliveryChannel.SMS);

  @InjectMocks CaseService target = new CaseServiceImpl();

  void verifyTimeInExpectedRange(long minAllowed, long maxAllowed, Date dateTime) {
    long actualInMillis = dateTime.getTime();
    assertTrue(actualInMillis + " not after " + minAllowed, actualInMillis >= minAllowed);
    assertTrue(actualInMillis + " not before " + maxAllowed, actualInMillis <= maxAllowed);
  }

  long asMillis(String datetime) throws ParseException {
    SimpleDateFormat dateParser = new SimpleDateFormat(DateTimeUtil.DATE_FORMAT_IN_JSON);
    return dateParser.parse(datetime).getTime();
  }

  <T extends EventPayload> T verifyEventSent(EventType expectedEventType, Class<T> payloadClazz) {
    ArgumentCaptor<T> payloadCaptor = ArgumentCaptor.forClass(payloadClazz);
    verify(eventPublisher)
        .sendEvent(
            eq(expectedEventType),
            eq(Source.CONTACT_CENTRE_API),
            eq(Channel.CC),
            payloadCaptor.capture());

    return payloadCaptor.getValue();
  }

  void verifyEventNotSent() {
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
  }

  void verifyEventNotSent(EventType type) {
    verify(eventPublisher, never()).sendEvent(eq(type), any(), any(), any());
  }
}
