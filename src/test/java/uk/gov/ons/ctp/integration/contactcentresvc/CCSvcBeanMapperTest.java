package uk.gov.ons.ctp.integration.contactcentresvc;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;

public class CCSvcBeanMapperTest {

  private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Test
  public void testCaseContainerDTO_CaseDTO() {
    CaseContainerDTO source = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    CaseDTO destination = mapperFacade.map(source, CaseDTO.class);
    assertEquals(source.getId(), destination.getId());
    assertEquals(source.getCaseRef(), destination.getCaseRef());
    assertEquals(source.getAddressType(), destination.getAddressType());
    assertEquals(null, destination.getEstabType());
    assertEquals(source.getEstabType(), destination.getEstabDescription());
    assertEquals(source.getAddressLine1(), destination.getAddressLine1());
    assertEquals(source.getAddressLine2(), destination.getAddressLine2());
    assertEquals(source.getAddressLine3(), destination.getAddressLine3());
    assertEquals(source.getTownName(), destination.getTownName());
    assertEquals(source.getPostcode(), destination.getPostcode());
    assertEquals(source.getOrganisationName(), destination.getCeOrgName());
    assertEquals(source.getRegion().substring(0, 1), destination.getRegion());
    assertEquals(source.getUprn(), String.valueOf(destination.getUprn().getValue()));
    assertEquals(source.getEstabUprn(), String.valueOf(destination.getEstabUprn().getValue()));
    assertEquals(source.getCreatedDateTime(), destination.getCreatedDateTime());
    assertEquals(source.getLastUpdated(), destination.getLastUpdated());
    assertEquals(source.isHandDelivery(), destination.isHandDelivery());
    for (int i = 0; i < source.getCaseEvents().size(); i++) {
      EventDTO sourceEvent = source.getCaseEvents().get(i);
      CaseEventDTO destinationEvent = destination.getCaseEvents().get(i);
      assertEquals(sourceEvent.getDescription(), destinationEvent.getDescription());
      assertEquals(sourceEvent.getEventType(), destinationEvent.getCategory());
      assertEquals(sourceEvent.getCreatedDateTime(), destinationEvent.getCreatedDateTime());
    }
  }

  @Test
  public void AddressIndexAddressCompositeDTO_CollectionCaseNewAddress() {
    AddressIndexAddressCompositeDTO source =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    CollectionCaseNewAddress destination = mapperFacade.map(source, CollectionCaseNewAddress.class);
    Address destAddr = destination.getAddress();
    assertEquals(source.getUprn(), destAddr.getUprn());
    assertEquals(source.getAddressLine1(), destAddr.getAddressLine1());
    assertEquals(source.getAddressLine2(), destAddr.getAddressLine2());
    assertEquals(source.getAddressLine3(), destAddr.getAddressLine3());
    assertEquals(source.getTownName(), destAddr.getTownName());
    assertEquals(source.getPostcode(), destAddr.getPostcode());
    assertEquals(source.getCensusAddressType(), destAddr.getAddressType());
    assertEquals(source.getCensusEstabType(), destAddr.getEstabType());
    assertEquals(source.getCountryCode(), destAddr.getRegion());
  }

  @Test
  public void AddressIndexAddressCompositeDTO_CachedCase() {
    AddressIndexAddressCompositeDTO source =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    CachedCase destination = mapperFacade.map(source, CachedCase.class);
    assertEquals(source.getUprn(), destination.getUprn());
    assertEquals(source.getFormattedAddress(), destination.getFormattedAddress());
    assertEquals(source.getAddressLine1(), destination.getAddressLine1());
    assertEquals(source.getAddressLine2(), destination.getAddressLine2());
    assertEquals(source.getAddressLine3(), destination.getAddressLine3());
    assertEquals(source.getTownName(), destination.getTownName());
    assertEquals(source.getPostcode(), destination.getPostcode());
    assertEquals(source.getCensusAddressType(), destination.getAddressType());
    assertEquals(source.getCensusEstabType(), destination.getEstabType());
    assertEquals(source.getCountryCode(), destination.getRegion());
  }

  @Test
  public void CachedCase_CaseDTO() {
    CachedCase source = FixtureHelper.loadClassFixtures(CachedCase[].class).get(0);
    CaseDTO destination = mapperFacade.map(source, CaseDTO.class);
    assertEquals(source.getId(), destination.getId().toString());
    assertEquals(source.getUprn(), String.valueOf(destination.getUprn().getValue()));
    assertEquals(source.getCreatedDateTime(), destination.getCreatedDateTime());
    assertEquals(source.getAddressLine1(), destination.getAddressLine1());
    assertEquals(source.getAddressLine2(), destination.getAddressLine2());
    assertEquals(source.getAddressLine3(), destination.getAddressLine3());
    assertEquals(source.getTownName(), destination.getTownName());
    assertEquals(source.getPostcode(), destination.getPostcode());
    assertEquals(source.getAddressType(), destination.getAddressType());
    assertEquals(source.getCaseType().name(), destination.getCaseType());
    assertEquals(null, destination.getEstabType());
    assertEquals(source.getEstabType(), destination.getEstabDescription());
    assertEquals(source.getRegion(), destination.getRegion());
  }

  @Test
  public void testCaseContainerDTO_Address() {
    CaseContainerDTO source = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Address destination = mapperFacade.map(source, Address.class);
    assertEquals(source.getAddressLine1(), destination.getAddressLine1());
    assertEquals(source.getAddressLine2(), destination.getAddressLine2());
    assertEquals(source.getAddressLine3(), destination.getAddressLine3());
    assertEquals(source.getTownName(), destination.getTownName());
    assertEquals(source.getPostcode(), destination.getPostcode());
    assertEquals(source.getRegion(), destination.getRegion());
    assertEquals(source.getUprn(), destination.getUprn());
    assertEquals(source.getLatitude(), destination.getLatitude());
    assertEquals(source.getLongitude(), destination.getLongitude());
    assertEquals(source.getEstabUprn(), destination.getEstabUprn());
    assertEquals(source.getAddressType(), destination.getAddressType());
    assertEquals(source.getAddressLevel(), destination.getAddressLevel());
    assertEquals(source.getEstabType(), destination.getEstabType());
  }
}
