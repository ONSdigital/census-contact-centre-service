package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import uk.gov.ons.ctp.common.event.model.*;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

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

    // execution - call the first unit under test
    ResponseDTO responseDTO = target.fulfilmentRequestByPost(caseIdFixture, requestBodyDTOFixture);

    // verify the value of the id in the ResponseDTO
    assertEquals(responseDTO.getId(), caseIdFixture.toString());

    // execution - call the second unit under test
    String fulfilmentCodeFixture = requestBodyDTOFixture.getProductCode();
    Product.DeliveryChannel deliveryChannelFixture = Product.DeliveryChannel.POST;

    FulfilmentRequestedEvent fulfilmentRequestedEvent =
        target.searchProductsAndConstructEvent(fulfilmentCodeFixture, deliveryChannelFixture);

    // here you need to construct an Event to compare with the one returned...
    FulfilmentRequestedEvent fulfilmentRequestedEventFixture = new FulfilmentRequestedEvent();

    FulfilmentPayload fulfilmentPayloadFixture = fulfilmentRequestedEventFixture.getPayload();

    FulfilmentRequest fulfilmentRequestFixture = fulfilmentPayloadFixture.getFulfilmentRequest();

    fulfilmentRequestFixture.setFulfilmentCode("XXXXXX-XXXXXX");
    fulfilmentRequestFixture.setCaseId("bbd55984-0dbf-4499-bfa7-0aa4228700e9");

    Address addressFixture = fulfilmentRequestFixture.getAddress();
    addressFixture.setAddressLine1("1 main street");
    addressFixture.setAddressLine2("upper upperingham");
    addressFixture.setAddressLine3("");
    addressFixture.setTownName("upton");
    addressFixture.setPostcode("UP103UP");
    addressFixture.setRegion("E");
    addressFixture.setLatitude("50.863849");
    addressFixture.setLongitude("-1.229710");
    addressFixture.setUprn("XXXXXXXXXXXXX");
    addressFixture.setArid("XXXXX");
    addressFixture.setAddressType("CE");
    addressFixture.setEstabType("XXX");

    Contact contactFixture = fulfilmentRequestFixture.getContact();
    contactFixture.setTitle("Ms");
    contactFixture.setForename("jo");
    contactFixture.setSurname("smith");
    contactFixture.setEmail("me@example.com");
    contactFixture.setTelNo("+447890000000");

    Header headerFixture = new Header();
    headerFixture.setType("FULFILMENT_REQUESTED");
    headerFixture.setSource("CONTACT_CENTRE_API");
    headerFixture.setChannel("CC");
    //    headerFixture.setDateTime(DateTimeUtil.getCurrentDateTimeInJsonFormat());
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    fulfilmentRequestedEventFixture.setEvent(headerFixture);

    // verify the values of the fields within the fulfilmentRequestedEvent header
    assertEquals(headerFixture, fulfilmentRequestedEvent.getEvent());

    // verify the values of the fields within the fulfilmentRequestedEvent payload
    assertEquals(fulfilmentPayloadFixture, fulfilmentRequestedEvent.getPayload());
  }
}
