package uk.gov.ons.ctp.integration.contactcentresvc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.io.CharStreams;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class PgpEncryptTest {
  private static final String PUBLIC_KEY_1 = "pgp/key1.asc";
  private static final String PUBLIC_KEY_2 = "pgp/key2.asc";

  private static final String PRIVATE_KEY_1 = "pgp/priv-key1.asc";
  private static final String PRIVATE_KEY_2 = "pgp/priv-key2.asc";

  static final String PASS_PHRASE = "Good Golly Miss Molly";
  static final String PASS_PHRASE2 = "Bless My Soul";

  static final String TEST_STRING =
      "God grant me the serenity to accept the things I cannot change, "
          + "Courage to change the things I can, "
          + "and Wisdom to know the difference.";

  private static String readFileIntoString(String filename) throws Exception {
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
    Resource res = new ClassPathResource(PUBLIC_KEY_1);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);

    String privKey = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_1);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = PgpDecrypt.decrypt(secretKeyFile, encStr, PASS_PHRASE.toCharArray());
      assertEquals(TEST_STRING, decrypted);
    }
  }

  @Test
  public void shouldEncryptThenDecryptWithAlternativeKey() throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);

    String privKey = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_2);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = PgpDecrypt.decrypt(secretKeyFile, encStr, PASS_PHRASE2.toCharArray());
      assertEquals(TEST_STRING, decrypted);
    }
  }

  @Test
  public void shouldDoubleEncryptThenDecryptWithFirstPrivateKey() throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_1);
    Resource res2 = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);

    String privKey = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_1);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = PgpDecrypt.decrypt(secretKeyFile, encStr, PASS_PHRASE.toCharArray());
      assertEquals(TEST_STRING, decrypted);
    }
  }

  @Test
  public void shouldDoubleEncryptThenDecryptWithSecondPrivateKey() throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_1);
    Resource res2 = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);

    String privKey2 = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_2);
    try (ByteArrayInputStream secretKeyFile2 = new ByteArrayInputStream(privKey2.getBytes())) {
      String decrypted = PgpDecrypt.decrypt(secretKeyFile2, encStr, PASS_PHRASE2.toCharArray());
      assertEquals(TEST_STRING, decrypted);
    }
  }
}
