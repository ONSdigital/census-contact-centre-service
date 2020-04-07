package uk.gov.ons.ctp.integration.contactcentresvc.client.caseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import uk.gov.ons.ctp.common.model.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

public class CaseContainerDTOCaseMappingTest {
  private static final String A_UPRN = "334999999999";
  private CCSvcBeanMapper mapper = new CCSvcBeanMapper();
  private CaseContainerDTO caseContainerDTO = new CaseContainerDTO();
  private CaseDTO caseDTO;

  private void map() {
    caseDTO = mapper.map(caseContainerDTO, CaseDTO.class);
  }

  @Test
  public void regionTest() {
    map();
    assertEquals(null, caseDTO.getRegion());

    caseContainerDTO.setRegion("E12345678");
    map();
    assertEquals("E", caseDTO.getRegion());

    caseContainerDTO.setRegion("E");
    map();
    assertEquals("E", caseDTO.getRegion());

    caseContainerDTO.setRegion("");
    map();
    assertEquals("", caseDTO.getRegion());
  }

  @Test
  public void shouldMapSecureEstablishment() {
    caseContainerDTO.setSecureEstablishment(true);
    map();
    assertTrue(caseDTO.isSecureEstablishment());
  }

  @Test
  public void shouldMapNonSecureEstablishment() {
    caseContainerDTO.setSecureEstablishment(false);
    map();
    assertFalse(caseDTO.isSecureEstablishment());
  }

  @Test
  public void shouldAcceptNullEstablishmentUprn() {
    map();
    assertNull(caseDTO.getEstabUprn());
  }

  @Test
  public void shouldAcceptEstablishmentUprn() {
    caseContainerDTO.setEstabUprn(A_UPRN);
    map();
    assertEquals(new UniquePropertyReferenceNumber(A_UPRN), caseDTO.getEstabUprn());
  }
}
