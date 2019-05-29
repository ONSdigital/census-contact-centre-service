package uk.gov.ons.ctp.integration.contactcentresvc.client.caseService;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.client.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

public class CaseContainerDTOCaseMappingTest {
  private CCSvcBeanMapper mapper = new CCSvcBeanMapper();

  @Test
  public void regionTest() throws Exception {

    CaseContainerDTO caseContainerDTO = new CaseContainerDTO();

    CaseDTO caseDTO = mapper.map(caseContainerDTO, CaseDTO.class);
    assertEquals(null, caseDTO.getRegion());

    caseContainerDTO.setRegion("E12345678");
    caseDTO = mapper.map(caseContainerDTO, CaseDTO.class);
    assertEquals("E", caseDTO.getRegion());

    caseContainerDTO.setRegion("E");
    caseDTO = mapper.map(caseContainerDTO, CaseDTO.class);
    assertEquals("E", caseDTO.getRegion());

    caseContainerDTO.setRegion("");
    caseDTO = mapper.map(caseContainerDTO, CaseDTO.class);
    assertEquals("", caseDTO.getRegion());
  }
}
