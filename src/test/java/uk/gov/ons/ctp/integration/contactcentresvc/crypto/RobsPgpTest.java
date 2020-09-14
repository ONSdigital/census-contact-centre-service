package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import com.google.common.io.CharStreams;
import lombok.SneakyThrows;

public class RobsPgpTest {

  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

  static final String PASS_PHRASE = "Good Golly Miss Molly";

  static final String TEST_STRING =
      "God grant me the serenity to accept the things I cannot change, "
          + "Courage to change the things I can, "
          + "and Wisdom to know the difference.";

  private String FEILD_SECRET_KEY;
  private String FIELD_PUBLIC_KEY;

  @Before
  public void setup() {
    FIELD_PUBLIC_KEY = readFileIntoString("robs.pub");
    FEILD_SECRET_KEY = readFileIntoString("robs.priv");
    Security.addProvider(PROVIDER);
  }

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
  public void shouldReadPublicKey() {
    assertTrue(FIELD_PUBLIC_KEY.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----"));
  }

  @Test
  public void shouldReadPrivateKey() {
    assertTrue(FEILD_SECRET_KEY.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----"));
  }

  @Test
  public void testEncrypt() throws IOException, PGPException {
    HashSet<PGPPublicKey> publicKeys = new HashSet<PGPPublicKey>();
    publicKeys.add(getPublicPgpKey(FIELD_PUBLIC_KEY));
    String cipherText = encrypt(TEST_STRING, publicKeys);
    System.out.println(cipherText);
    PGPPrivateKey privateKey = getPrivatePgpKey(FEILD_SECRET_KEY, PASS_PHRASE.toCharArray());
    String plainText = decrypt(cipherText, privateKey);
    System.out.println(plainText);
  }

  @Test
  public void shouldEncryptThenDecrypt() throws Exception {
    Resource res = new ClassPathResource("robs.pub");
    Collection<Resource> ress = new ArrayList<Resource>();
    ress.add(res);
    byte[] encrypted = FileEncryption.encryptFile(TEST_STRING, ress);
    String encStr = new String(encrypted);
    System.out.println("---- encrypted ---- ");
    System.out.println(encStr);

//    HashSet<PGPPublicKey> publicKeys = new HashSet<PGPPublicKey>();
//    publicKeys.add(getPublicPgpKey(FIELD_PUBLIC_KEY));
//    String cipherText = encrypt(TEST_STRING, publicKeys);
//    byte[] encrypted = cipherText.getBytes();
//
//  System.out.println("---- encrypted ---- ");
//  System.out.println(cipherText);

    // ----

    String privKey = RobsPgpTest.readFileIntoString("robs.priv");
    try (ByteArrayInputStream encryptedFile = new ByteArrayInputStream(encrypted);
        ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decryptedFile = FileEncryption.decryptFile(secretKeyFile, encryptedFile,
          PASS_PHRASE.toCharArray());

      System.out.println("---- decrpted -----");
      System.out.println(decryptedFile);
    }
  }

  // RobC : Martin's original ...

//  private PGPPublicKey getPublicPgpKey(String pgpKey) throws IOException, PGPException {
//    InputStream encoderStream =
//        PGPUtil.getDecoderStream(new ByteArrayInputStream(pgpKey.getBytes()));
//    JcaPGPPublicKeyRingCollection pgpPub = new JcaPGPPublicKeyRingCollection(encoderStream);
//    encoderStream.close();
//    if (pgpPub.size() < 1) {
//      throw new IOException("No key available");
//    }
//    PGPPublicKey key = pgpPub.getKeyRings().next().getPublicKey();
//    System.out.println("Public key Id: " + Long.toHexString(key.getKeyID()).toUpperCase());
//    return key;
//  }

  // RobC from FSDR
  private PGPPublicKey getPublicPgpKey(String pgpKey) {
    try {
      InputStream keyIs = new ByteArrayInputStream(pgpKey.getBytes());
      InputStream input = PGPUtil.getDecoderStream(keyIs);

      JcaPGPPublicKeyRingCollection pgpPublicKeyRingCollection =
          new JcaPGPPublicKeyRingCollection(input);
      input.close();

      PGPPublicKey key = null;
      PGPPublicKey masterKey = null;
      Iterator<PGPPublicKeyRing> keyRings = pgpPublicKeyRingCollection.getKeyRings();
      while (key == null && keyRings.hasNext()) {
        PGPPublicKeyRing kRing = keyRings.next();
        Iterator<PGPPublicKey> publicKeys = kRing.getPublicKeys();
        while (key == null && publicKeys.hasNext()) {
          PGPPublicKey k = publicKeys.next();

          if (k.isEncryptionKey() && !k.isMasterKey()) {
            key = k;
          } else if (k.isEncryptionKey() && k.isMasterKey()) {
            masterKey = k; // should only ever be set if there is no subkey for encryption
          }
        }
        if (key == null && masterKey != null) {
          key = masterKey;
        }

      }
      return key;
    } catch (IOException | PGPException e) {
      throw new RuntimeException("failed to retrieve public key", e);
    }
  }


  private String encrypt(String plainText, Set<PGPPublicKey> publicKeys)
      throws IOException, PGPException {

    // RobC - Martin's orginal
//    final PGPEncryptedDataGenerator generator =
//        new PGPEncryptedDataGenerator(
//            new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
//                .setWithIntegrityPacket(true)
//                .setSecureRandom(new SecureRandom())
//                .setProvider(PROVIDER));
//    for (PGPPublicKey publicKey : publicKeys) {
//      generator.addMethod(
//          new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider(PROVIDER));
//    }

    final PGPEncryptedDataGenerator generator = new PGPEncryptedDataGenerator(
        new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
            .setWithIntegrityPacket(true).setSecureRandom(new SecureRandom())
            .setProvider(new BouncyCastleProvider()));

    for (PGPPublicKey publicKey : publicKeys) {
      generator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey)
          .setProvider(new BouncyCastleProvider()));
    }




    // this bit copied from FSDR

    final byte[] compressedFile = plainText.getBytes(); // compressFile(contents);
    final ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();
    try (OutputStream armoredOutputStream = new ArmoredOutputStream(encryptedBytes);
        OutputStream encryptedOut = generator.open(armoredOutputStream, compressedFile.length)) {
      encryptedOut.write(compressedFile);
    } catch (PGPException | IOException e) {
      throw new RuntimeException("failed to encrypt file", e);
    }
    return new String(encryptedBytes.toByteArray());


     // RobC: this is Martin's original code . don't think it completes the end message properly.
//
//
//    ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();
//    OutputStream armoredOutputStream = new ArmoredOutputStream(encryptedBytes);
//    byte[] inputData = plainText.getBytes();
//    OutputStream encryptedOut = generator.open(armoredOutputStream, inputData.length);
//    encryptedOut.write(inputData.length);
//    String cipherText = encryptedBytes.toString();
//
//    // Close all streams for safety
//    encryptedBytes.close();
//    armoredOutputStream.close();
//    encryptedOut.close();
//    return cipherText;
  }

  private PGPPrivateKey getPrivatePgpKey(String pgpKey, char[] passPhrase)
      throws IOException, PGPException {

    return getSecretKey(new ByteArrayInputStream(pgpKey.getBytes()), passPhrase);


    // Martin's original code. below. returns null.

//    InputStream decoderStream =
//        PGPUtil.getDecoderStream(new ByteArrayInputStream(pgpKey.getBytes()));
//    JcaPGPSecretKeyRingCollection pgpSecret = new JcaPGPSecretKeyRingCollection(decoderStream);
//    decoderStream.close();
//    if (pgpSecret.size() < 1) {
//      throw new IOException("No key available");
//    }
//    PGPSecretKey secretKey = pgpSecret.getKeyRings().next().getSecretKey();
//    System.out.println("Public key associated with secret key: " +  Long.toHexString(secretKey.getKeyID()).toUpperCase());
//    System.out.println("Is private key empty:" + secretKey.isPrivateKeyEmpty());
//    PBESecretKeyDecryptor decryptor =
//        new JcePBESecretKeyDecryptorBuilder().setProvider(PROVIDER).build(passPhrase);
//    PGPPrivateKey key = secretKey.extractPrivateKey(decryptor);
//    System.out.println("Private key: " + key);
//    return key;
  }


  private PGPPrivateKey getSecretKey(InputStream pgpSecretKey, char[] password)
      throws IOException, PGPException {
    InputStream decoderStream = PGPUtil.getDecoderStream(pgpSecretKey);
    JcaPGPSecretKeyRing pgpSecretKeys = new JcaPGPSecretKeyRing(decoderStream);
    decoderStream.close();
    Iterator<PGPSecretKey> secretKeys = pgpSecretKeys.getSecretKeys();
    PGPPrivateKey key = null;
    while (key == null && secretKeys.hasNext()) {
      PGPSecretKey k = secretKeys.next();
      if (!k.isMasterKey() && !k.isPrivateKeyEmpty()) {

        key = k.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
            .setProvider(PROVIDER).build(password));
      }

    }
    return key;
  }

  // RobC method copied from FSDR
  private Iterator<PGPEncryptedData> getEncryptedObjects(final byte[] message)
      throws IOException {
    try {
      final PGPObjectFactory factory =
          new PGPObjectFactory(PGPUtil.getDecoderStream(new ByteArrayInputStream(message)),
              new JcaKeyFingerprintCalculator());
      final Object first = factory.nextObject();
      final Object list = (first instanceof PGPEncryptedDataList) ? first : factory.nextObject();
      return ((PGPEncryptedDataList) list).getEncryptedDataObjects();
    } catch (IOException e) {
      throw new IOException(e);
    }
  }

  // RobC : method copied from FSDR
  private PGPLiteralData asLiteral(final InputStream clear)
      throws IOException, PGPException {
    final PGPObjectFactory plainFact =
        new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
    final Object message = plainFact.nextObject();
    if (message instanceof PGPCompressedData) {
      final PGPCompressedData cData = (PGPCompressedData) message;
      final PGPObjectFactory pgpFact =
          new PGPObjectFactory(cData.getDataStream(), new JcaKeyFingerprintCalculator());
      // Find the first PGPLiteralData object
      Object object = null;
      for (int safety = 0; (safety++ < 1000) && !(object instanceof PGPLiteralData); object =
          pgpFact.nextObject()) {
        // ignore
      }
      return (PGPLiteralData) object;
    } else if (message instanceof PGPLiteralData) {
      return (PGPLiteralData) message;
    } else {
      throw new PGPException(
          "message is not a simple encrypted file - type unknown: " + message.getClass().getName());
    }
  }


  private String decrypt(String cipherText, PGPPrivateKey privateKey)
      throws IOException, PGPException {

    // RobC code below copied from FSDR

    PGPPublicKeyEncryptedData encryptedData = null;
    Iterator<PGPEncryptedData> encryptedObjects;
    try {
      encryptedObjects = getEncryptedObjects(cipherText.getBytes());
    } catch (IOException e) {
      throw new RuntimeException("failed to read decrypted file", e);
    }
    while (encryptedObjects.hasNext()) {
      encryptedData = (PGPPublicKeyEncryptedData) encryptedObjects.next();
    }
    try {
      InputStream decryptedData = encryptedData.getDataStream(
          new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(PROVIDER).build(privateKey));
      PGPLiteralData pgpLiteralData = asLiteral(decryptedData);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      Streams.pipeAll(pgpLiteralData.getInputStream(), out);
      return out.toString();
    } catch (IOException | PGPException e) {
      throw new RuntimeException("failed to decrypt file", e);
    }

      // RobC - Martin's code below ...

//    InputStream decoderStream =
//        PGPUtil.getDecoderStream(new ByteArrayInputStream(cipherText.getBytes()));
//    PGPObjectFactory factory = new JcaPGPObjectFactory(decoderStream);
//    final Object first = factory.nextObject();
//    final Object list = (first instanceof PGPEncryptedDataList) ? first : factory.nextObject();
//    Iterator<PGPEncryptedData> iterator = ((PGPEncryptedDataList) list).getEncryptedDataObjects();
//
//    // Just gets required key, already know key
//    System.out.println("Private keyId: " + privateKey.getKeyID());
//
//    PGPPublicKeyEncryptedData encrypted = null;
//    boolean key = false;
//    while (!key && iterator.hasNext()) {
//      encrypted = (PGPPublicKeyEncryptedData) iterator.next();
//      System.out.println("Packet keyId:  " + encrypted.getKeyID());
//      key = encrypted.getKeyID() == privateKey.getKeyID();
//    }
//    PublicKeyDataDecryptorFactory dataDecryptor =
//        new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(PROVIDER).build(privateKey);
//    try (InputStream decryptedStream = encrypted.getDataStream(dataDecryptor)) {
//      JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(decryptedStream);
//      Object message = plainFact.nextObject();
//
//      // If the message is literal data, read it and process to the output stream
//      if (message instanceof PGPLiteralData) {
//        PGPLiteralData literalData = (PGPLiteralData) message;
//        String plainText =
//            new BufferedReader(
//                    new InputStreamReader(literalData.getInputStream(), StandardCharsets.UTF_8))
//                .lines()
//                .collect(Collectors.joining("\n"));
//        return plainText;
//      } else {
//        return "";
//      }
//    }
  }
}
