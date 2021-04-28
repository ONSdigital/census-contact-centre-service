package uk.gov.ons.ctp.integration.contactcentresvc;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

@Component
public class CCSPostcodesBean {
  private static final Logger log = LoggerFactory.getLogger(CCSPostcodesBean.class);

  @Autowired private AppConfig appConfig;

  private Set<String> ccsPostcodes;

  public boolean isInCCSPostcodes(String postcode) {
    return ccsPostcodes.contains(postcode);
  }

  @PostConstruct
  private void init() {
    this.ccsPostcodes = new HashSet<>();
    String strPostcodePath = appConfig.getCcsPostcodes().getCcsPostcodePath();

    boolean isRunningCC = appConfig.getChannel() == Channel.CC;

    if (isRunningCC) {
      try (BufferedReader br = new BufferedReader(new FileReader(strPostcodePath))) {
        String postcode;
        while ((postcode = br.readLine()) != null) {
          ccsPostcodes.add(postcode.trim());
        }
        log.with("size", ccsPostcodes.size()).info("Read ccsPostcodes from file");
      } catch (IOException e) {
        if (new File(strPostcodePath).exists()) {
          log.with("strPostcodePath", strPostcodePath)
              .error(
                  "APPLICATION IS MISCONFIGURED - Unable to read in postcodes from file."
                      + " Using postcodes from application.yml instead.",
                  e);
        } else {
          log.with("strUprnBlacklistPath", strPostcodePath)
              .error(
                  "APPLICATION IS MISCONFIGURED - Postcode file doesn't exist."
                      + " Using postcodes from application.yml instead.");
        }
        ccsPostcodes = appConfig.getCcsPostcodes().getCcsDefaultPostcodes();
      }
    }
  }
}
