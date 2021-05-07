package uk.gov.ons.ctp.integration.contactcentresvc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;
import uk.gov.ons.ctp.integration.contactcentresvc.config.CCSPostcodes;

public class CCSPostcodesBeanTest {

  @Test
  public void testDefaultList_findWithSpaceInSearchPostcode() {
    CCSPostcodesBean postcodesBean = createPostcodesBeanWithNoFile("GW12 AAA");
    assertTrue(postcodesBean.isInCCSPostcodes("GW12 AAA"));
  }

  @Test
  public void testDefaultList_findWithNoSpaceInSearchPostcode() {
    CCSPostcodesBean postcodesBean = createPostcodesBeanWithNoFile("GW12 AAA");
    assertTrue(postcodesBean.isInCCSPostcodes("GW12AAA"));
  }

  @Test
  public void testDefaultList_dontFindUnknownPostcode() {
    CCSPostcodesBean postcodesBean = createPostcodesBeanWithNoFile("GW12 AAA");
    assertFalse(postcodesBean.isInCCSPostcodes("PO11 5AB"));
  }

  @Test
  public void testPostcodeFile_findWithSpaceInSearchPostcode() throws IOException {
    CCSPostcodesBean postcodesBean = createPostcodesBeanPostcodeFile("PO11 5AB", "AB12 7NB");
    assertTrue(postcodesBean.isInCCSPostcodes("AB12 7NB"));
  }

  @Test
  public void testPostcodeFile_findWithNoSpaceInSearchPostcode() throws IOException {
    CCSPostcodesBean postcodesBean = createPostcodesBeanPostcodeFile("PO11 5AB", "AB12 7NB");
    assertTrue(postcodesBean.isInCCSPostcodes("PO115AB"));
  }

  @Test
  public void testPostcodeFile_dontFindUnknownPostcode() throws IOException {
    CCSPostcodesBean postcodesBean = createPostcodesBeanPostcodeFile("PO11 5AB", "AB12 7NB");
    assertFalse(postcodesBean.isInCCSPostcodes("GW12 AAA"));
  }

  private CCSPostcodesBean createPostcodesBeanWithNoFile(String... defaultPostcodes) {
    AppConfig appConfig = new AppConfig();
    appConfig.setChannel(Channel.CC);

    CCSPostcodes ccsPostcodes = new CCSPostcodes();
    appConfig.setCcsPostcodes(ccsPostcodes);

    // Set up postcodes file (not used)
    ccsPostcodes.setCcsPostcodePath("/tmp/c0d3ceb6-16c4-4bf8-8dbb-2184e274a80b");

    // Populate default test postcodes
    Set<String> mySet = new HashSet<String>(Arrays.asList(defaultPostcodes));
    ccsPostcodes.setCcsDefaultPostcodes(mySet);

    CCSPostcodesBean postcodesBean = new CCSPostcodesBean();
    ReflectionTestUtils.setField(postcodesBean, "appConfig", appConfig);
    postcodesBean.init();

    return postcodesBean;
  }

  private CCSPostcodesBean createPostcodesBeanPostcodeFile(String... defaultPostcodes)
      throws IOException {
    AppConfig appConfig = new AppConfig();
    appConfig.setChannel(Channel.CC);

    CCSPostcodes ccsPostcodes = new CCSPostcodes();
    appConfig.setCcsPostcodes(ccsPostcodes);

    // Set up postcodes file
    Path tempFile = Files.createTempFile(null, null);
    List<String> content = Arrays.asList(defaultPostcodes);
    Files.write(tempFile, content, StandardOpenOption.APPEND);
    ccsPostcodes.setCcsPostcodePath(tempFile.toString());

    // Populate default test postcodes (not used)
    Set<String> mySet = new HashSet<String>(Arrays.asList("no-postcodes"));
    ccsPostcodes.setCcsDefaultPostcodes(mySet);

    CCSPostcodesBean postcodesBean = new CCSPostcodesBean();
    ReflectionTestUtils.setField(postcodesBean, "appConfig", appConfig);
    postcodesBean.init();

    return postcodesBean;
  }
}
