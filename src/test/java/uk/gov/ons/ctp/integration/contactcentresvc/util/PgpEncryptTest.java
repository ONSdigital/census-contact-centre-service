package uk.gov.ons.ctp.integration.contactcentresvc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.io.CharStreams;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class PgpEncryptTest {
  private static final String PUBLIC_KEY_1 = "pgp/key1.asc";
  private static final String PUBLIC_KEY_2 = "pgp/key2.asc";

  static final String PRIVATE_KEY_1 = "pgp/priv-key1.asc";
  static final String PRIVATE_KEY_2 = "pgp/priv-key2.asc";

  public static final String PASS_PHRASE = "Good Golly Miss Molly";
  public static final String PASS_PHRASE2 = "Bless My Soul";

  static final String TEST_STRING =
      "God grant me the serenity to accept the things I cannot change, "
          + "Courage to change the things I can, "
          + "and Wisdom to know the difference.";

  public static String readFileIntoString(String filename) throws Exception {
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
    verifyDecrypt(TEST_STRING, encStr, PASS_PHRASE, PRIVATE_KEY_1);
  }

  @Test
  public void shouldEncryptThenDecryptWithAlternativeKey() throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);
    verifyDecrypt(TEST_STRING, encStr, PASS_PHRASE2, PRIVATE_KEY_2);
  }

  @Test
  public void shouldDoubleEncryptThenDecryptWithFirstPrivateKey() throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_1);
    Resource res2 = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);
    verifyDecrypt(TEST_STRING, encStr, PASS_PHRASE, PRIVATE_KEY_1);
  }

  @Test
  public void shouldDoubleEncryptThenDecryptWithSecondPrivateKey() throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_1);
    Resource res2 = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(TEST_STRING, ress);
    verifyDecrypt(TEST_STRING, encStr, PASS_PHRASE2, PRIVATE_KEY_2);
  }

  private void verifyEncryptWithBase64(String clearText) throws Exception {
    Resource res = new ClassPathResource(PUBLIC_KEY_1);
    Resource res2 = new ClassPathResource(PUBLIC_KEY_2);
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res2);
    ress.add(res);
    String encStr = PgpEncrypt.encrypt(clearText, ress);
    String base64str = Base64.getEncoder().encodeToString(encStr.getBytes(StandardCharsets.UTF_8));
    String pgpField = new String(Base64.getDecoder().decode(base64str), StandardCharsets.UTF_8);
    verifyDecrypt(clearText, pgpField, PASS_PHRASE2, PRIVATE_KEY_2);
  }

  @Test
  public void shouldBase64AndPgpEncryptAndDecryptNamesWithAlternateCharacters() throws Exception {
    String[] names = {
      "Zoë",
      "Renée",
      "Noël",
      "Sørina",
      "Sévērus",
      "Adrián",
      "François",
      "Mary-Jo",
      "Peggy-Sue",
      "Mónica",
      "Seán",
      "Ruairí",
      "Mátyás",
      "Jokūbas",
      "Siân",
      "Agnès",
      "KŠthe",
      "Øyvind",
      "Fañch",
      "Nuñez",
      "Mæve",
      "Jœ"
    };

    for (String n : names) {
      verifyEncryptWithBase64(n);
    }
  }

  private void verifyDecrypt(
      String clearText, String encrypted, String passPhrase, String privateKey) throws Exception {
    String privKey = PgpEncryptTest.readFileIntoString(privateKey);
    try (ByteArrayInputStream is = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted = PgpDecrypt.decrypt(is, encrypted, passPhrase.toCharArray());
      assertEquals(clearText, decrypted, "Failed to decrypt: " + clearText);
    }
  }
}
