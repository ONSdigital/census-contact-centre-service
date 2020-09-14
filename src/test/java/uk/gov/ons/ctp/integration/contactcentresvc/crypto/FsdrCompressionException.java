package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

@SuppressWarnings("serial")
public class FsdrCompressionException extends Exception {

  public FsdrCompressionException(String msg, Exception e) {
    super(msg,e);
  }

}
