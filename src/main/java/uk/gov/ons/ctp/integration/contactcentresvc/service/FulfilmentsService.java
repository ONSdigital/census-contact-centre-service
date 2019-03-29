package uk.gov.ons.ctp.integration.contactcentresvc.service;

import com.google.common.collect.Lists;
import java.util.List;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentDTO.Method;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.FulfilmentsRequestDTO;

public interface FulfilmentsService {

  public default List<FulfilmentDTO> getFulfilments(FulfilmentsRequestDTO requestDTO) {
    FulfilmentDTO fulfilment1 =
        FulfilmentDTO.builder()
            .productCode("p1")
            .language("English")
            .description("FAKE fulfilment1")
            .method(Method.EMAIL)
            .build();

    FulfilmentDTO fulfilment2 =
        FulfilmentDTO.builder()
            .productCode("p2")
            .language("English")
            .description("FAKE fulfilment2")
            .method(Method.POST)
            .build();

    FulfilmentDTO fulfilment3 =
        FulfilmentDTO.builder()
            .productCode("p3")
            .language("Welsh")
            .description("FAKE fulfilment3")
            .method(Method.SMS)
            .build();

    List<FulfilmentDTO> fakeResults = Lists.newArrayList(fulfilment1, fulfilment2, fulfilment3);

    return fakeResults;
  }
}
