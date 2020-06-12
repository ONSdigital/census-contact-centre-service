package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.VersionResponseDTO;

/** The REST endpoint controller for ContactCentreSvc Version Details */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class VersionEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(VersionEndpoint.class);

  private ResourceLoader resourceLoader;

  @Autowired
  public VersionEndpoint(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   */
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public VersionResponseDTO getVersion() {
    log.info("Entering GET getVersion");
    VersionResponseDTO fakeVersion =
        VersionResponseDTO.builder().apiVersion(swaggerVersion()).build();
    return fakeVersion;
  }

  @SuppressWarnings("unchecked")
  private String swaggerVersion() {
    String ver = "UNKNOWN";
    Resource resource = resourceLoader.getResource("classpath:swagger-current.yml");

    Yaml yaml = new Yaml();
    try {
      Map<String, Object> obj = yaml.load(resource.getInputStream());
      Map<String, Object> info = (Map<String, Object>) obj.get("info");
      ver = info.get("version").toString();
      ver = ver.replace("-oas3", "");
    } catch (IOException e) {
      log.error("Cannot determine Swagger version", e);
    }
    return ver;
  }
}
