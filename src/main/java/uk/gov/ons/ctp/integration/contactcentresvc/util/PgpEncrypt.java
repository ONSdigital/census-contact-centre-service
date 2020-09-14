package uk.gov.ons.ctp.integration.contactcentresvc.util;

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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.springframework.core.io.Resource;

/** PGP encryption using one or more ascii armoured public keys. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PgpEncrypt {
  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

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
    final PGPEncryptedDataGenerator generator =
        new PGPEncryptedDataGenerator(
            new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                .setWithIntegrityPacket(true)
                .setSecureRandom(new SecureRandom())
                .setProvider(PROVIDER));

    for (PGPPublicKey publicKey : publicPgpKeys) {
      generator.addMethod(
          new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider(PROVIDER));
    }
    final ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();
    try (OutputStream armoredOutputStream = new ArmoredOutputStream(encryptedBytes);
        OutputStream encryptedOut =
            generator.open(armoredOutputStream, compressedContents.length)) {
      encryptedOut.write(compressedContents);
    }
    return encryptedBytes.toByteArray();
  }

  private static Collection<PGPPublicKey> getPublicKeys(Collection<Resource> publicKeyResources)
      throws IOException, PGPException {
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
      PGPPublicKeyRing nextKeyRing = keyRings.next();
      Iterator<PGPPublicKey> publicKeys = nextKeyRing.getPublicKeys();
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
      final OutputStream outputStream =
          literal.open(
              compressedDataGenerator.open(byteOutputStream),
              PGPLiteralData.BINARY,
              "filename",
              inputStream.available(),
              new Date());
      Streams.pipeAll(inputStream, outputStream);
      compressedDataGenerator.close();
      return byteOutputStream.toByteArray();
    }
  }
}
