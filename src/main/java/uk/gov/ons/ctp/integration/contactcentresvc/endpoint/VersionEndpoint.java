package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.PostConstruct;
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
  private String version = "UNKNOWN";

  @Autowired
  public VersionEndpoint(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @PostConstruct
  @SuppressWarnings("unchecked")
  void readSwaggerVersion() {
    Resource resource = resourceLoader.getResource("classpath:swagger-current.yml");

    Yaml yaml = new Yaml();
    try (InputStream is = resource.getInputStream()) {
      Map<String, Object> yamlMap = yaml.load(is);
      Map<String, Object> info = (Map<String, Object>) yamlMap.get("info");
      version = info.get("version").toString();
      version = version.replace("-oas3", "");
    } catch (IOException e) {
      log.error("Cannot determine Swagger version", e);
    }
    log.info("Swagger version used: {}", version);
  }

  /**
   * the GET endpoint to get contact centre Details
   *
   * @return the contact centre details found
   */
  @RequestMapping(value = "/version", method = RequestMethod.GET)
  public VersionResponseDTO getVersion() {
    log.info("Entering GET getVersion");
    VersionResponseDTO fakeVersion = VersionResponseDTO.builder().apiVersion(version).build();
    return fakeVersion;
  }
}
