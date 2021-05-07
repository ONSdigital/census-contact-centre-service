package uk.gov.ons.ctp.integration.contactcentresvc;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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

  public boolean isInCCSPostcodes(String rawPostcode) {
    String cleanedPostcode = normalisePostcode(rawPostcode);
    return ccsPostcodes.contains(cleanedPostcode);
  }

  @PostConstruct
  private void init() {
    this.ccsPostcodes = new HashSet<>();
    String strPostcodePath = appConfig.getCcsPostcodes().getCcsPostcodePath();

    boolean isRunningCC = appConfig.getChannel() == Channel.CC;

    if (isRunningCC) {
      try (BufferedReader br = new BufferedReader(new FileReader(strPostcodePath))) {
        String rawPostcode;
        while ((rawPostcode = br.readLine()) != null) {
          String postcode = normalisePostcode(rawPostcode);
          ccsPostcodes.add(postcode);
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
        ccsPostcodes =
            appConfig.getCcsPostcodes().getCcsDefaultPostcodes().stream()
                .map(p -> normalisePostcode(p))
                .collect(Collectors.toSet());
      }
    }
  }

  private String normalisePostcode(String rawPostcode) {
    String normalisedPostcode = rawPostcode.trim();
    normalisedPostcode = normalisedPostcode.replace(" ", "");
    return normalisedPostcode;
  }
}
