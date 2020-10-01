package uk.gov.ons.ctp.integration.contactcentresvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

@Component
public class CCSPostcodesBean {
  private static final Logger log = LoggerFactory.getLogger(CCSPostcodesBean.class);

  @Autowired
  private AppConfig appConfig;

  private Set<String> ccsPostcodes;

  public boolean isInCCSPostcodes(String postcode) {
    return ccsPostcodes.contains(postcode);
  }

  @PostConstruct
  private void init() {
    this.ccsPostcodes = new HashSet<>();
    String strPostcodePath = appConfig.getCcsPostcodes().getCcsPostcodePath();

    Path postcodeFilePath = Paths.get(strPostcodePath);
    List<String> postcodes;
    try {
      postcodes = Files.readAllLines(postcodeFilePath);
      for (String postcode : postcodes) {
        postcode = postcode.trim();
        ccsPostcodes.add(postcode);
      }
    } catch (IOException e) {
      log.with(strPostcodePath).error("APPLICATION IS MISCONFIGURED - unable to read in postcodes from file."
          + " Using postcodes from application.yml instead.", e);
      ccsPostcodes = appConfig.getCcsPostcodes().getCcsDefaultPostcodes();
    }
  }
}
