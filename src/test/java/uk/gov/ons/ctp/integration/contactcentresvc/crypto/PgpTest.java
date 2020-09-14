package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import com.google.common.io.CharStreams;
import lombok.SneakyThrows;

public class PgpTest {

  static final String PASS_PHRASE = "Good Golly Miss Molly";
  static final String PASS_PHRASE2 = "Bless My Soul";

  static final String TEST_STRING =
      "God grant me the serenity to accept the things I cannot change, "
          + "Courage to change the things I can, "
          + "and Wisdom to know the difference.";

  @SneakyThrows
  public static String readFileIntoString(String filename) {
    try (InputStream is = ClassLoader.getSystemResourceAsStream(filename)) {
      String text = null;
      try (Reader reader = new InputStreamReader(is)) {
        text = CharStreams.toString(reader);
      }
      return text;
    }
  }

  @Test
  public void shouldEncryptThenDecrypt() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    String encStr = Pgp.encrypt(TEST_STRING, ress);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);

    String base64encStr = Base64.getEncoder().encodeToString(encStr.getBytes());
    System.out.println("Base64: " + base64encStr);

    // ----

    String privKey = PgpTest.readFileIntoString("robs.priv");
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = Pgp.decrypt(secretKeyFile, encStr, PASS_PHRASE.toCharArray());

      System.out.println("---- decrypted -----");
      System.out.println(decrypted);
      assertEquals(TEST_STRING, decrypted);
    }
  }

  @Test
  public void shouldEncryptThenDecryptWithAlternativeKey() throws Exception {
    Resource res = new ClassPathResource("rob2.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    String encStr = Pgp.encrypt(TEST_STRING, ress);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);

    String base64encStr = Base64.getEncoder().encodeToString(encStr.getBytes());
    System.out.println("Base64: " + base64encStr);

    // ----

    String privKey = PgpTest.readFileIntoString("rob2.priv");
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = Pgp.decrypt(secretKeyFile, encStr, PASS_PHRASE2.toCharArray());

      System.out.println("---- decrypted -----");
      System.out.println(decrypted);
      assertEquals(TEST_STRING, decrypted);
    }
  }

  @Test
  public void shouldDoubleEncryptThenDecryptWithFirstPrivateKey() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Resource res2 = new ClassPathResource("rob2.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    String encStr = Pgp.encrypt(TEST_STRING, ress);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);

    // ----

    String privKey = PgpTest.readFileIntoString("robs.priv");
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = Pgp.decrypt(secretKeyFile, encStr, PASS_PHRASE.toCharArray());

      System.out.println("---- decrypted -----");
      System.out.println(decrypted);
    }
  }

  @Test
  public void shouldDoubleEncryptThenDecryptWithSecondPrivateKey() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Resource res2 = new ClassPathResource("rob2.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    // ress.add(res2);
    String encStr = Pgp.encrypt(TEST_STRING, ress);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);

    // ----

    String privKey2 = PgpTest.readFileIntoString("rob2.priv");
    try (ByteArrayInputStream secretKeyFile2 = new ByteArrayInputStream(privKey2.getBytes())) {
      String decrypted = Pgp.decrypt(secretKeyFile2, encStr, PASS_PHRASE2.toCharArray());

      System.out.println("---- decrypted -----");
      System.out.println(decrypted);
    }
  }


  @Test
  public void shouldDoubleEncrypt() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Resource res2 = new ClassPathResource("ryan.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    // ress.add(res2);
    String encStr = Pgp.encrypt(TEST_STRING, ress);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);
  }

}
