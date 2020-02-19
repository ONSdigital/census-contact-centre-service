package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Region;
import uk.gov.ons.ctp.integration.contactcentresvc.service.FulfilmentsService;

public class FulfilmentServiceImplTest {

  @Mock ProductReference productReference;

  @Spy private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @InjectMocks FulfilmentsService fulfilmentService = new FulfilmentsServiceImpl();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void fulfilmentServiceGood() throws Exception {
    // The mocked productReference will return this product
    Product returnedProduct =
        Product.builder()
            .caseTypes(new ArrayList<Product.CaseType>(List.of(Product.CaseType.HH)))
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

    // call the unit under test
    List<FulfilmentDTO> fulfilments =
        fulfilmentService.getFulfilments(CaseType.HH, Region.E, DeliveryChannel.POST);

    // fulfilmentService should call the productReference with this example Product
    Product expectedExample =
        Product.builder()
            .caseTypes(new ArrayList<Product.CaseType>(List.of(Product.CaseType.HH)))
            .regions(new ArrayList<Product.Region>(List.of(Product.Region.E)))
            .requestChannels(
                new ArrayList<Product.RequestChannel>(List.of(Product.RequestChannel.CC)))
            .deliveryChannel(Product.DeliveryChannel.POST)
            .build();

    // verify that the unit under test called the expected productReference and with the
    // expectedExample
    ArgumentCaptor<Product> argument = ArgumentCaptor.forClass(Product.class);
    verify(productReference).searchProducts(argument.capture());
    assertEquals(expectedExample, argument.getValue());

    // now check that the returned fulfilment DTOs are correctly populated by the mapper the unit
    // under test used
    assertTrue(fulfilments.size() == 1);
    FulfilmentDTO fulfilment = fulfilments.get(0);

    assertEquals(fulfilment.getCaseTypes().get(0).name(), CaseType.HH.name());
    assertEquals(fulfilment.getDescription(), "foobar");
    assertEquals(
        fulfilment.getDeliveryChannel().name(),
        uk.gov.ons.ctp.integration.contactcentresvc.representation.DeliveryChannel.POST.name());
    assertEquals(fulfilment.getFulfilmentCode(), "ABC123");
    assertEquals(fulfilment.getLanguage(), "eng");
    assertTrue(
        fulfilment
            .getRegions()
            .contains(uk.gov.ons.ctp.integration.contactcentresvc.representation.Region.E));
  }

  @Test
  public void fulfilmentServiceEmpty() throws Exception {
    // The mocked productReference will return no products
    Mockito.when(productReference.searchProducts(any())).thenReturn(new ArrayList<Product>());

    // call the unit under test
    List<FulfilmentDTO> fulfilments =
        fulfilmentService.getFulfilments(CaseType.HH, Region.E, DeliveryChannel.POST);

    // now check that no dtos were returned
    assertTrue(fulfilments.size() == 0);
  }
}
