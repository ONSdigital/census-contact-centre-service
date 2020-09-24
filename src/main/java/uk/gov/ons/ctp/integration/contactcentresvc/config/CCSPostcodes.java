package uk.gov.ons.ctp.integration.contactcentresvc.config;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class CCSPostcodes {
  private static final Logger log = LoggerFactory.getLogger(CCSPostcodes.class);
  private Set<String> ccsPostcodesToCheck;
  private String ccsPostcodePath;

  public boolean isInCCSPostcodes(String postcode) {
    readInPostcodes();
    return ccsPostcodesToCheck.contains(postcode);
  }

  private void readInPostcodes() {
    Set<String> ccsPostcodes = new HashSet<>();

    List<String> postcodes;
    try {
      postcodes = Files.readAllLines(Paths.get(this.ccsPostcodePath));
      for (String postcode : postcodes) {
        postcode = postcode.trim();
        ccsPostcodes.add(postcode);
      }
      this.ccsPostcodesToCheck = ccsPostcodes;
    } catch (IOException e) {
      log.with(this.ccsPostcodePath)
          .warn(
              "IOException caught as unable to read in postcodes from file."
                  + " Using postcodes from application.yml instead.",
              e);
    }
  }
}
