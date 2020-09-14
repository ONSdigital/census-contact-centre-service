package uk.gov.ons.ctp.integration.contactcentresvc.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.util.io.Streams;

/** PGP encryption and decryption methods. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PgpDecrypt {
  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

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
          decryptedData =
              encryptedData.getDataStream(
                  new JcePublicKeyDataDecryptorFactoryBuilder()
                      .setProvider(PROVIDER)
                      .build(secretKey));
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
        key =
            k.extractPrivateKey(
                new JcePBESecretKeyDecryptorBuilder().setProvider(PROVIDER).build(password));
      }
    }
    return key;
  }

  private static Iterator<PGPEncryptedData> getEncryptedObjects(final byte[] message)
      throws IOException {
    final PGPObjectFactory factory =
        new PGPObjectFactory(
            PGPUtil.getDecoderStream(new ByteArrayInputStream(message)),
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
      for (int safety = 0;
          (safety++ < 1000) && !(object instanceof PGPLiteralData);
          object = pgpFact.nextObject()) {
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
