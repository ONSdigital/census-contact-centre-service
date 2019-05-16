package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.ons.ctp.common.event.model.*;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class CaseServiceImplTest {

  private static final String REQUEST_DATE_TIME = "2017-02-11T16:32:11.863";

  @Mock ProductReference productReference;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks CaseService target = new CaseServiceImpl();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void caseServiceGood() throws Exception {

    // The mocked productReference will return this product
    Product returnedProduct =
        Product.builder()
            .caseType(Product.CaseType.H)
            .description("foobar")
            .fulfilmentCode("ABC123")
            .language("eng")
            .deliveryChannel(Product.DeliveryChannel.POST)
            .regions(new ArrayList<Product.Region>(List.of(Product.Region.E, Product.Region.W)))
            .requestChannels(
                new ArrayList<Product.RequestChannel>(
                    List.of(Product.RequestChannel.CC, Product.RequestChannel.FIELD)))
            .build();
    Mockito.when(productReference.searchProducts(any()))
        .thenReturn(new ArrayList<Product>(List.of(returnedProduct)));

    UUID caseIdFixture = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    PostalFulfilmentRequestDTO requestBodyDTOFixture = new PostalFulfilmentRequestDTO();

    requestBodyDTOFixture.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    requestBodyDTOFixture.setTitle("Mr");
    requestBodyDTOFixture.setForename("Mickey");
    requestBodyDTOFixture.setSurname("Mouse");
    requestBodyDTOFixture.setProductCode("P_OR_H1");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    requestBodyDTOFixture.setDateTime(LocalDateTime.parse(REQUEST_DATE_TIME, formatter));


    // call the unit under test
    ResponseDTO responseDTO = target.fulfilmentRequestByPost(caseIdFixture, requestBodyDTOFixture);

    // here you need to construct an Event to publish...
    //TODO: rename these fields to be called fixtures if we need to use them
    
    FulfilmentRequestedEvent fulfilmentRequestedEvent = new FulfilmentRequestedEvent();

    FulfilmentPayload fulfilmentPayload = fulfilmentRequestedEvent.getPayload();

    FulfilmentRequest fulfilmentRequest = fulfilmentPayload.getFulfilmentRequest();

    fulfilmentRequest.setFulfilmentCode("XXXXXX-XXXXXX");
    fulfilmentRequest.setCaseId("bbd55984-0dbf-4499-bfa7-0aa4228700e9");

    Address address = fulfilmentRequest.getAddress();
    address.setAddressLine1("1 main street");
    address.setAddressLine2("upper upperingham");
    address.setAddressLine3("");
    address.setTownName("upton");
    address.setPostcode("UP103UP");
    address.setRegion("E");
    address.setLatitude("50.863849");
    address.setLongitude("-1.229710");
    address.setUprn("XXXXXXXXXXXXX");
    address.setArid("XXXXX");
    address.setAddressType("CE");
    address.setEstabType("XXX");

    Contact contact = fulfilmentRequest.getContact();
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setEmail("me@example.com");
    contact.setTelNo("+447890000000");

    Header header = new Header();
    header.setType("FULFILMENT_REQUESTED");
    header.setSource("CONTACT_CENTRE_API");
    header.setChannel("CC");
    //    header.setDateTime(DateTimeUtil.getCurrentDateTimeInJsonFormat());
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    fulfilmentRequestedEvent.setEvent(header);

//    // fulfilmentService should call the productReference with this example Product
////    Product expectedExample =
////        Product.builder()
////            .caseType(Product.CaseType.H)
////            .regions(new ArrayList<Product.Region>(List.of(Product.Region.E)))
////            .requestChannels(
////                new ArrayList<Product.RequestChannel>(List.of(Product.RequestChannel.CC)))
////            .build();
////
////    // verify that the unit under test called the expected productReference and with the
////    // expectedExample
////    ArgumentCaptor<Product> argument = ArgumentCaptor.forClass(Product.class);
////    verify(productReference).searchProducts(argument.capture());
////    assertEquals(expectedExample, argument.getValue());
////
////    // now check that the returned fulfilment DTOs are correctly populated by the mapper the unit
////    // under test used
////    assertTrue(fulfilments.size() == 1);
////    FulfilmentDTO fulfilment = fulfilments.get(0);
////
////    assertEquals(fulfilment.getCaseType().name(), CaseType.H.name());
////    assertEquals(fulfilment.getDescription(), "foobar");
////    assertEquals(
////        fulfilment.getDeliveryChannel().name(),
////        uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel.POST.name());
////    assertEquals(fulfilment.getFulfilmentCode(), "ABC123");
////    assertEquals(fulfilment.getLanguage(), "eng");
////    assertTrue(
////        fulfilment
////            .getRegions()
////            .contains(Region.E));
  }

}
