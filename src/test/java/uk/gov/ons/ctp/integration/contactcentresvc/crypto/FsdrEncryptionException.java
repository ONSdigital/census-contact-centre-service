package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

@SuppressWarnings("serial")
public class FsdrEncryptionException extends Exception {

  public FsdrEncryptionException(String msg, Exception e) {
    super(msg,e);
  }

}
