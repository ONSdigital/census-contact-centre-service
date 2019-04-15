package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

  @Autowired ProductReference productReference;
  
  @Override
  public List<Product> getProducts(String caseType, String region) throws CTPException {
    Product example = new Product();
    example.setCaseType(caseType);
    example.setRegions(Arrays.asList(region));
    return productReference.searchProducts(example);
  }


}
