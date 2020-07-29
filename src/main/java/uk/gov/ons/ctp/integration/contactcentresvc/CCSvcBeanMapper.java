package uk.gov.ons.ctp.integration.contactcentresvc;

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.util.StringToUPRNConverter;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.cloud.CachedCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;

/** The bean mapper that maps to/from DTOs and JPA entity types. */
@Component
public class CCSvcBeanMapper extends ConfigurableMapper {

  /**
   * Setup the mapper for all of our beans. Only fields having non identical names need mapping if
   * we also use byDefault() following.
   *
   * @param factory the factory to which we add our mappings
   */
  protected final void configure(final MapperFactory factory) {
    ConverterFactory converterFactory = factory.getConverterFactory();
    converterFactory.registerConverter("regionConverter", new RegionConverter());
    converterFactory.registerConverter(new StringToUUIDConverter());
    converterFactory.registerConverter(new StringToUPRNConverter());

    factory
        .classMap(CaseContainerDTO.class, CaseDTO.class)
        .field("estabType", "estabDescription")
        .field("organisationName", "ceOrgName")
        .byDefault()
        .fieldMap("region", "region")
        .converter("regionConverter")
        .add()
        .register();

    factory
        .classMap(EventDTO.class, CaseEventDTO.class)
        .field("eventType", "category")
        .byDefault()
        .register();

    factory
        .classMap(AddressIndexAddressCompositeDTO.class, CollectionCaseNewAddress.class)
        .field("uprn", "address.uprn")
        .field("addressLine1", "address.addressLine1")
        .field("addressLine2", "address.addressLine2")
        .field("addressLine3", "address.addressLine3")
        .field("townName", "address.townName")
        .field("postcode", "address.postcode")
        .field("censusAddressType", "address.addressType")
        .field("censusEstabType", "address.estabType")
        .field("countryCode", "address.region")
        .field("organisationName", "organisationName")
        .register();

    factory
        .classMap(AddressIndexAddressCompositeDTO.class, CachedCase.class)
        .field("censusAddressType", "addressType")
        .field("censusEstabType", "estabType")
        .field("countryCode", "region")
        .field("organisationName", "ceOrgName")
        .byDefault()
        .register();

    factory
        .classMap(CachedCase.class, CaseDTO.class)
        .field("estabType", "estabDescription")
        .byDefault()
        .register();

    factory
        .classMap(CachedCase.class, CaseContainerDTO.class)
        .field("ceOrgName", "organisationName")
        .byDefault()
        .register();

    factory.classMap(CaseContainerDTO.class, Address.class).byDefault().register();
    factory.classMap(CaseContainerDTO.class, AddressCompact.class).byDefault().register();
  }

  private class RegionConverter extends BidirectionalConverter<String, String> {
    public String convertTo(String src, Type<String> dstType, MappingContext context) {
      return convert(src);
    }

    public String convertFrom(String src, Type<String> dstType, MappingContext context) {
      return convert(src);
    }

    private String convert(String src) {
      return StringUtils.isEmpty(src) ? src : src.substring(0, 1).toUpperCase();
    }
  }
}
