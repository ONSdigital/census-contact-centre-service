package uk.gov.ons.ctp.integration.contactcentresvc;

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
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

    factory
        .classMap(CaseContainerDTO.class, CaseDTO.class)
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
