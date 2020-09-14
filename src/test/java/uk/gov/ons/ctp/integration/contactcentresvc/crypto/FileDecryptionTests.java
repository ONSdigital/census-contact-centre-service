package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@RunWith(MockitoJUnitRunner.class)
public class FileDecryptionTests {

  private String testPrivatePgpKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----\n"
      + "Version: OpenPGP v2.0.8\n"
      + "Comment: https://sela.io/pgp/\n"
      + "\n"
      + "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\n"
      + "kdP2zIVSmo6Z6UU=\n"
      + "=TECY\n"
      + "-----END PGP PRIVATE KEY BLOCK-----\n";

  private final String encryptedTestCsv = "-----BEGIN PGP MESSAGE-----\n"
      + "Version: OpenPGP v2.0.8\n"
      + "Comment: https://sela.io/pgp/\n"
      + "\n"
      + "wcBMA4uhu3pvKvOWAQf7B+LRQQ9eu6JhIsxsExu3AceyqDdJX9rQO/UqeYxfkDw4\n"
      + "PhYUYWM85X3ggmItPemEf6Hu6EnmrbSaZv363xCMqN9bUBw4eUx9jBrAoqqwCC6a\n"
      + "gkRZ0UVIAHQy0Rp+4p5MsuyRPGjUy6+0XcxlWPHT8P9rsHxEhAgmaYeHchngHtnD\n"
      + "JpYBUMACrKzIex6CMB3nQ9x0FJvnx11gkoPc+ApARbM9rOHwNhECUyg69OIwwkVl\n"
      + "BNAYgRj2EEZkZlJhoKLbbVrC7vlyKqzZjAl3XAephF1qPRj2SZuLBlTrNvk1fOTx\n"
      + "1sTZrRT3Xi4uJKDQvnAOA4pryOk6Tt+TTao3IkVZ0NLA/gFqCldL0ibMaf0wyUBP\n"
      + "h/owJg99gIgYL6hQvAoGzV6oc5NYZPQUlVYJmzYaNRXCOMjteTkmBQgff7hu3iTu\n"
      + "fDYbJwXeVkdik43uVup0wjC8s08J5+QsSe6D71u5q22ghiO6JkpocLShA1il3gRv\n"
      + "Eiyn8RAG2ZsvUlUdNuDsGcSIDzKiXXOIr4KTIFdH1uV7EVAx+FopC2ie3fpM/wBL\n"
      + "vb1v1lsZ4RxLYifJbvdqW7nis1aMeeZVboGdSjkb64MpTYacCPs7pgrEAfzgV1F5\n"
      + "XaNAGiwMhD5WTBdGCM2V8pld7yPFTgXKA/8EkFc1KyvNDrk/NqFsRTHZYl6zNF90\n"
      + "51hffTe/pmbyVmndR5kUAKoc7OrHjecWRrzVSjSv0T1Th2P0QGGYJpx1sgXK1oVI\n"
      + "mhmjulfqrrXhWGoPlNSReDlOcM57WgM8TXUbC1tJgBjN5lRSdiJX9xqZjnH9sGP1\n"
      + "YQWPTibm2DtBNJ+Rm97onP1lcZTqLoL6LZ8SRo8A44vCPfPnXJkk/2odJ3IDr32Y\n"
      + "K5RxYJkbhjn//hU5IBRhYGBjI0FqC4Mi9bHp3cv3wXK0TPVNVrFL7kwg2VsaNZIv\n"
      + "=doNI\n"
      + "-----END PGP MESSAGE-----\n";

  private final String nisraCsv =
      "NISRA unique employee ID|First name|Surname|Preferred name|Address 1|Address 2|Town/City|County|Post Code|Personal email address|Telephone number contact 1|Telephone number contact 2|Emergency contact|Emergency contact number 1|Job Role|Role ID|Line manager first name|Line manager surname|Line manager telephone number|Area Location|Country|Weekly hours|Contract start date|Contract end date|Assignment start date|Assignment end date|Status\n"
          + "FJ13ED2|first|surname|test pref|test address 1|test address 2|test town|test county|test postcode|test email|01234567890|09876543210|emergency contact|07123456744|Census Team Coordinator|TES1-AA|Gareth|Trolle|07123456744|test location|Northern Ireland|37|07/01/2019|08/01/2021|07/01/2019|07/01/2021|Employee";

  @Test
  public void decryptFile() throws FsdrEncryptionException {
    ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(testPrivatePgpKey.getBytes());
    ByteArrayInputStream encryptedFile = new ByteArrayInputStream(encryptedTestCsv.getBytes());
    String decryptedFile = FileEncryption.decryptFile(secretKeyFile, encryptedFile, "password".toCharArray());
    assertThat(decryptedFile).contains(nisraCsv);
  }

  @Test
  public void shouldEncrypt() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    byte[] encrypted = FileEncryption.encryptFile(RobsPgpTest.TEST_STRING, ress);
    String encStr = new String(encrypted);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);
  }

  @Test
  public void shouldEncryptThenDecrypt() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    byte[] encrypted = FileEncryption.encryptFile(RobsPgpTest.TEST_STRING, ress);
    String encStr = new String(encrypted);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);

    // ----

    String privKey = RobsPgpTest.readFileIntoString("robs.priv");
    try (ByteArrayInputStream encryptedFile = new ByteArrayInputStream(encrypted);
        ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decryptedFile = FileEncryption.decryptFile(secretKeyFile, encryptedFile,
          RobsPgpTest.PASS_PHRASE.toCharArray());

      System.out.println("---- decrpted -----");
      System.out.println(decryptedFile);
    }
  }

}
