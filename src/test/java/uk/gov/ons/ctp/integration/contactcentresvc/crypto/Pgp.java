package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
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
import org.springframework.core.io.Resource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * PGP encryption and decryption methods.
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Pgp {
  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

  // ----- ENCRYPTION ... ---

  public static String encrypt(String clearMsg, Collection<Resource> publicKeyResources) {
    try {
      Collection<PGPPublicKey> publicPgpKeys = getPublicKeys(publicKeyResources);
      byte[] encyptedFileContents = encrypt(publicPgpKeys, clearMsg);
      return new String(encyptedFileContents);
    } catch (PGPException | IOException e) {
      throw new RuntimeException("failed to encrypt contents", e);
    }
  }

  private static byte[] encrypt(Collection<PGPPublicKey> publicPgpKeys, String clearMsg)
    throws IOException, PGPException {
    final byte[] compressedContents = compress(clearMsg);
    final PGPEncryptedDataGenerator generator = new PGPEncryptedDataGenerator(
        new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
            .setWithIntegrityPacket(true).setSecureRandom(new SecureRandom())
            .setProvider(PROVIDER));

    for (PGPPublicKey publicKey : publicPgpKeys) {
      generator
          .addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider(PROVIDER));
    }
    final ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();
    try (OutputStream armoredOutputStream = new ArmoredOutputStream(encryptedBytes);
        OutputStream encryptedOut = generator.open(armoredOutputStream, compressedContents.length)) {
      encryptedOut.write(compressedContents);
    }
    return encryptedBytes.toByteArray();
  }

  private static Collection<PGPPublicKey> getPublicKeys(
      Collection<Resource> publicKeyResources) throws IOException, PGPException {
    var publicKeys = new ArrayList<PGPPublicKey>();
    for (Resource resource : publicKeyResources) {
      PGPPublicKey publicPgpKey = getPublicKey(resource);
      publicKeys.add(publicPgpKey);
    }
    return publicKeys;
  }

  private static PGPPublicKey getPublicKey(Resource pgpKey) throws IOException, PGPException {
    InputStream input = PGPUtil.getDecoderStream(pgpKey.getInputStream());

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
  }

  private static byte[] compress(String clearMsg) throws IOException {
    try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(clearMsg.getBytes());
        final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {
      final PGPCompressedDataGenerator compressedDataGenerator =
          new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
      final PGPLiteralDataGenerator literal = new PGPLiteralDataGenerator();
      final OutputStream outputStream = literal.open(compressedDataGenerator.open(byteOutputStream),
          PGPLiteralData.BINARY, "filename", inputStream.available(), new Date());
      Streams.pipeAll(inputStream, outputStream);
      compressedDataGenerator.close();
      return byteOutputStream.toByteArray();
    }
  }

  // --------- DECRYPTION ... ----

  public static String decrypt(InputStream secretKeyFile, String encryptedContent, char[] passwd) {
    try {
      PGPPrivateKey secretKey = getPrivateKey(secretKeyFile, passwd);
      PGPPublicKeyEncryptedData encryptedData = null;
      Iterator<PGPEncryptedData> encryptedObjects =
          getEncryptedObjects(encryptedContent.getBytes());

      InputStream decryptedData = null;
      while (encryptedObjects.hasNext()) {
        encryptedData = (PGPPublicKeyEncryptedData) encryptedObjects.next();
        try {
          decryptedData = encryptedData.getDataStream(
              new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(PROVIDER).build(secretKey));
        } catch (PGPException e) {
          // try another public provider.
          continue;
        }
      }
      PGPLiteralData pgpLiteralData = asLiteral(decryptedData);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      Streams.pipeAll(pgpLiteralData.getInputStream(), out);
      return out.toString();
    } catch (IOException | PGPException e) {
      throw new RuntimeException("failed to decrypt contents", e);
    }
  }

  private static PGPPrivateKey getPrivateKey(InputStream pgpSecretKey, char[] password)
      throws IOException, PGPException {
    InputStream decoderStream = PGPUtil.getDecoderStream(pgpSecretKey);
    JcaPGPSecretKeyRing pgpSecretKeys = new JcaPGPSecretKeyRing(decoderStream);
    decoderStream.close();
    Iterator<PGPSecretKey> secretKeys = pgpSecretKeys.getSecretKeys();
    PGPPrivateKey key = null;
    while (key == null && secretKeys.hasNext()) {
      PGPSecretKey k = secretKeys.next();
      if (!k.isMasterKey() && !k.isPrivateKeyEmpty()) {
        key = k.extractPrivateKey(
            new JcePBESecretKeyDecryptorBuilder().setProvider(PROVIDER).build(password));
      }
    }
    return key;
  }

  private static Iterator<PGPEncryptedData> getEncryptedObjects(final byte[] message)
      throws IOException {
    final PGPObjectFactory factory =
        new PGPObjectFactory(PGPUtil.getDecoderStream(new ByteArrayInputStream(message)),
            new JcaKeyFingerprintCalculator());
    final Object first = factory.nextObject();
    final Object list = (first instanceof PGPEncryptedDataList) ? first : factory.nextObject();
    return ((PGPEncryptedDataList) list).getEncryptedDataObjects();
  }

  private static PGPLiteralData asLiteral(final InputStream clear)
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
      throw new RuntimeException(
          "message is not a simple encryption - type unknown: " + message.getClass().getName());
    }
  }
}
