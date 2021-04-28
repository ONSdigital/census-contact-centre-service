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
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

/** This class contains UPRNs which have been blacklisted. */
@Component
public class BlacklistedUPRNBean {
  private static final Logger log = LoggerFactory.getLogger(BlacklistedUPRNBean.class);

  @Autowired private AppConfig appConfig;

  private Set<Long> blacklistedUprns;

  /**
   * Determine if the supplied uprn is on the list of blacklisted UPRN.
   *
   * @param uprn is the UPRN to check.
   * @return true if the supplied UPRN is not null and is on the blacklist, otherwise false.
   */
  public boolean isUPRNBlacklisted(UniquePropertyReferenceNumber uprn) {
    if (uprn == null) {
      return false;
    }

    return blacklistedUprns.contains(uprn.getValue());
  }

  @PostConstruct
  private void init() {
    this.blacklistedUprns = new HashSet<>();

    boolean isRunningCC = appConfig.getChannel() == Channel.CC;

    if (isRunningCC) {
      String strUprnBlacklistPath = appConfig.getUprnBlacklist().getUprnBlacklistPath();

      String uprnAsString;
      try (BufferedReader br = new BufferedReader(new FileReader(strUprnBlacklistPath))) {
        while ((uprnAsString = br.readLine()) != null) {
          log.with("uprn", uprnAsString).debug("Reading blacklisted entry");
          UniquePropertyReferenceNumber uprn =
              new UniquePropertyReferenceNumber(uprnAsString.trim());
          blacklistedUprns.add(uprn.getValue());
        }
        log.with("size", blacklistedUprns.size()).info("Read blacklisted UPRNs from file");
      } catch (IOException e) {
        if (new File(strUprnBlacklistPath).exists()) {
          log.with("strUprnBlacklistPath", strUprnBlacklistPath)
              .error(
                  "APPLICATION IS MISCONFIGURED - Unable to read blacklisted UPRNs from file."
                      + " Using default blacklisted UPRNs from application.yml instead.",
                  e);
        } else {
          log.with("strUprnBlacklistPath", strUprnBlacklistPath)
              .error(
                  "APPLICATION IS MISCONFIGURED - Blacklisted UPRN file doesn't exist."
                      + " Using default blacklisted UPRNs from application.yml instead.");
        }
        blacklistedUprns = appConfig.getUprnBlacklist().getDefaultUprnBlacklist();
      }
    }
  }
}
