package uk.gov.ons.ctp.integration.contactcentresvc.crypto;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.junit.Test;

public class PGPEncryption {

  private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

  private static final String PASS_PHRASE = "Yours is the Earth and everything thats in it";

  private static final String TEST_STRING =
      "God grant me the serenity to accept the things I cannot change, "
          + "Courage to change the things I can, "
          + "and Wisdom to know the difference.";

  private static final String FEILD_SECRET_KEY =
      "-----BEGIN PGP PRIVATE KEY BLOCK-----\n"
          + "Version: Keybase OpenPGP v1.0.0\n"
          + "Comment: https://keybase.io/crypto\n"
          + "\n"
          + "xcaGBF9Z3PUBEADUBsIfygzVjU21g0fg4kJ61Alodyg2EKQLRZQVmsB2Hg3aD2AJ\n"
          + "4prEW36rliS9svDR/Z3zZ2TnqtDw44TTARj7m/P5YV5WE+z85tzsqFhjd2lfDf2X\n"
          + "wpfuMS6wk8PosALOmp4LkQkzuvMMs1w+FsQ9ufhGsY7joE7ZZG9nosdZeF2XyV+r\n"
          + "7+xBqT3IRgfMla3ZcacqTRRuLmCe9xcud4YY16Vpoig+n1HQI74XB0Hm4OVEcmX5\n"
          + "RVmC/HjL9Z2B0JrB1WxGyV+GaMyFEJUobdjZ4llUxEWtd2sVmvbapREnhg+fQ811\n"
          + "ZddR3JrBi8bfxLxibUqMyEksL7NvZmFBOVhRRQknkpI7gQnTYkinS0QECOLEP6qa\n"
          + "LP+CJifPRcvm7nj8aGstQpu7CWPGM/lOZzo9/N7nqHHU3lcfpb4PVzEJX+5Fr7C/\n"
          + "F1iV4JKSyBZKl4PVTtSwgVAbnebTyp+0cXS/ih6eFNX4aYl8Au7O983kQpfoIBb6\n"
          + "j8eVkddQC+5aEY417pcHO4WoSMvnMh8tJyqBT62OR3oO2MAm/yEj49EIAg0Hw18y\n"
          + "xgUTMyD77eLJB1iUc5AJPTCeaG4Rh86ujSvfSYi8YJ4TDGeU9/HD01s2KwSy0g0s\n"
          + "EVJz5VRRkZAWHwcXeS0fYgPnAudBz7/u6S1R8c2wXN84BeP5xaVBX8Z4jQARAQAB\n"
          + "/gkDCMLjOR86Yh1vYF/bERjpp9BNQkJb/uH3FBV3DADqEkWKru0gyPXJCazXLPKC\n"
          + "o7IdxIsKbueJz96NLAwR/tnoLBckjFdp5JrWSg9T2NQ8ZtxSLVI8CCHvD5CSMON1\n"
          + "dm3cEgLDSSPUowzTOZtNZEK6emYxKzcNU5/wG2pbpMFTIeJUvvQWSFJ3F+3ZEzQp\n"
          + "omKbcmsaB33sLtgW/RjbRntKF+n6d+WeJgLBD/zhMr7aFniNJwaqw2q+nMRNncYu\n"
          + "8KnaYJm5Pe4kbBP5oJEy1k27cwVdiaBYRMw25gwjsY8HhimGKPmxiH+4JEwPNQbm\n"
          + "xXcwVdn3nA36todDeTrKnofo8AAKdd4fbhBoBiglKW8VVGjOffPLMfEEOcQm4xDE\n"
          + "p8pmjmREkPwIflqxQDqo5+bpGZ/8fc3z1URkcLY407Acxi/a30Kj+8V8DVhyD0/l\n"
          + "UCbrQMKcCp2qyo1/uCQGEqCK3VnwRt0Y+2fUnpeGyomRw03zIGBWTzld7wvBSSa1\n"
          + "qutkMw8xYduwnDx6pcNVOewSy11tk+oW+D4oelTRFHE2BPAa+9bJuAL+LmAWdlrR\n"
          + "250ib2hZHdC0n42N7udlxQD6epATOsNoyLP1efCNthTt+jFwBzOGqXMTLO5fLYvV\n"
          + "gN43DiStZxye7VSfPu+8/S4R6tsoa3i47/PIufr/GgC7eyx07mR6FGvAfD1UdLLc\n"
          + "Axo8Fu70k3q3RW1xt40qXofIQF7nVbCLka1Ab32ifWDyjgMc3h2iCZd/LlgY2hFG\n"
          + "9ldaRyrXH4JNqv9+1ClS1P9V8DIbxIBhFHj5XSh2M+ItFhKFEohxmXiBMio1YYmW\n"
          + "cRVajgDjpOefeAd0K8wnoIbqJA1YqPKS4oB8uumaJHiXShY6wlosrXo22WHl+Mna\n"
          + "cwMtFmXaQ+PF9Z9HDc4zGuKyafeCX6x5lGG8o9HQKQNuIFK++PWR/fR6wqMu4nQC\n"
          + "Vcg8jMD3htTlWFm3+RK267r+vleyop3XMedpR2cyJ/c6huiYrEFI5x/kj87PSofL\n"
          + "6DNh/TgIfXO4LnHJ+7NTWD3OKp9+f0XyGFjeDfLZapPm4VaO8Ef0dWtesGOVO19e\n"
          + "aixjXEmnQ5qiXiwncqbKyopXG0a93cZDuXROcEUnh3tqw/30o9xYQWb7kj9ahUJW\n"
          + "kgSAJqdKhfEXOiTa+CAwwLSbiCMN8vI8OnTlJQXIBJTu8DFpLB5zPeFq2mYaFmpN\n"
          + "mnhe0BrR5wI7GmQBHQ1oq5SXLDiTpDemgpgY45zQ4A/WHDen/4eKzXWU/QBxT/1f\n"
          + "q1dgs7reNt2ku4nyTZDi09lkzgY4syDtnuio1Gn5XRULpQxRKjMWN7Y+yVYcCo4j\n"
          + "M0KftH1UlifrJ32VN2A3JUIJZ7SBKYRjiv4P90zRZCJNfPvzEACztfUlw+GHewAb\n"
          + "fZkEY+fWU2Ud/pQCKdW9wmi1xXvOiDwb4ALJ3rRre+A11wqgwjtpssAkkEsWOV9n\n"
          + "/HiZ04zmFsuyMgD7Wnj+aYlH8Mfon/0S6qBh9yfK4/6V56yxi3NRhs/OY6OxtA2X\n"
          + "wGQxMGyEz+zeBtCnPyop2zgcw6P3ZlZaxxMHmBiA1ad4MnrydlyFPL+HOUaeq1b+\n"
          + "O0mjHTs5pq5H+GeIVSJkWrlzYYFIQzAfdZcLckqzDdDoUtBOecRzN95oJvuwMjxJ\n"
          + "ZnGGBFeHsYyOOVF3bdk+C1OxcB+ubTf4+sXPTC0PrtXSYWveZEVLopzW06CD1WxS\n"
          + "cH6p0IWq5fbfEnqg5/O7+yWjyMzmg1AFrORXKQmiKll4MoCOhTP2g+HNHGZpZWxk\n"
          + "LnRlc3QgPHRlc3RAZG9tYWluLmNvbT7CwW0EEwEKABcFAl9Z3PUCGy8DCwkHAxUK\n"
          + "CAIeAQIXgAAKCRAbeupLRnLR4dXTEACc988yYivdpFIf1WkWbM0nRKSQmspQSjVe\n"
          + "SMXQvNcXDHQDktlcreCwF08U7me0/GL5mLudW1Qh4LFZxqLn7YvNiJJEigtnMBRe\n"
          + "0lqDlcPDV0JhHd5qOf0nrX4Poy+qKMTQQw+ndRuj8oCthQrPH6LpUvCCkjmWA1gW\n"
          + "Te40AWGJP9PMnJdttH0YB6xadPk3Y+7r01nnHaVQphQoqIwg6uXC0wlOEc7PMBdt\n"
          + "n/iPPLPJgrvvbOHw/MuxI5AISZmdUhbjsgyosBR0PUc5MD3RnOuc2/+in9RDotwC\n"
          + "s4HSnI3l4UZe6+6WBnb+WF4jg8iL0cQBeer3Ue/+Xr42pkSOsE46iijtCw1NHef/\n"
          + "0BLeohWkozWXqnvZPjUYXhok/TsysuCecFgfebR2L3tFagU7k5VwhaNKdfWgksZi\n"
          + "X0+iT8Twc0T4bNHuDzIzi18EOH62ww1PyDOk+JLtsa3IwLmluyKUgBHvyGfnTefW\n"
          + "2vBpe6cWxWYuJqTRX5rl7+pa9LjZcB8NIYjJX8rn9cfMTzr7W6J5HDfRt8rsSpmj\n"
          + "wEQT9ykfiKMu5YZXk/+WxjxGtZK4qCNURGH+l6/goUDvMBLwUX/wDgLQtaFFbQDC\n"
          + "RgSBm15lDdq/jq+2twaZDyxuhiBIeAPT15Mwnajn2qOGwCVvZThnTj5LXsZxarpD\n"
          + "y7MA801NVsfGhgRfWdz1ARAAwSUXkDwjItM2XewmcQprznJkRNvgSBNAjJqW0ltQ\n"
          + "HdT8Qbv2WW2z9ISlDvc3/o+0F5WSK0zU0b70/ZAbVaYCogB8+cW0Tj2WIxG5x3k1\n"
          + "GRah70ZscnSmSHdoe7sufvzOiwq6Y75oEkV/IEeAxDIR8D0pkqRrebjy36jQ87wa\n"
          + "3vm/aAJskmeVqMvwABF1tD007tWE2N8PtFSbzRUd4Wh/AQOL0UZHhGOXZ8wrrIk6\n"
          + "+62mQichcN51mvwnDfewA9zrkCfMei/c5ZOBX059g7yui8fpMN+ctMq7evKv8yKF\n"
          + "FZrxQJSevGUWz5uss/AUEidRXOzXLmHAqwMlKxTvYVA5N3bOWxl1WY11Hio34/Id\n"
          + "TGzsWk2lraV32M99ZSa8qpYGqXXhCK9ysH5LUbBPA+rXkoS4Uv+DzlGhFkl6o6yI\n"
          + "qUyyOv9U/IkD1NrBXrxCChdD9Fcnt8+cG6zvozgool42awVRRcGTp6o0BNnSMbSM\n"
          + "UwSrrQqXEHvqhnbFzJXIdCwBKHE0AE6jlxHm9lFhA1Zk1XmyAo+Wyhorhn3r9WDh\n"
          + "16KhTfJm62e3Li2oTLhUf+9mE2M0UONXgXv7NH3V8gUa+krTqqXu+RXalZeDi/RK\n"
          + "63o7liqLu4cWvvk9mAdjNi9NnNvWdUVExBYSojqiDmgguMjRRnAT0tVMkVG+O6CL\n"
          + "xUkAEQEAAf4JAwiVa78HF0eGCGDN2HzevqFqraORwqBeKJt377yVRnG9Up+iDw42\n"
          + "tCrTJvrJ5uApiqc7G/DNTlAllIE3xy3O+QwRnHtuQRxQY1ZpS9RvnroaW4gMcBT3\n"
          + "0TmBZ7NbQLM3PoQWNrS2GrRDs1yZH7Hbs2S9TbUkDjJ8dnvquG0zgv9dFUhU8isN\n"
          + "N5mMj6Wzz0jJOxHtM7wFEPLlpAeJUw/+8IVVf5KXdmxHATJyqbYqPwfmeCqWPcH2\n"
          + "/puBn3BwMP+/823/VRowkFi2GQaRz7BLF3TdHJElnSZgjiFoCSjVvGPVshM3c/W/\n"
          + "PZgGBwr26JostuADXCNowF9XEWwei1pTRWk8ZUF4BK3EZIiavFEt09JZ8MW158Bd\n"
          + "Xa3ZS1mUaOa9yAt5wGSaiK2+5GCCIHER0qjZOBP688YHW1k8QdJkbuQLAVRASW+D\n"
          + "vlCstupmT8Yq1COL7QRFm1JfBybwdKyCVfCX+PA3bHcoQLtWmb4N4T1KSI5DWyQY\n"
          + "35PakiUnSHif8RfHAlt9BRSozcLyhfA5bgIu68OlVYIOm8qPa30cZuL03MZR2lfO\n"
          + "OC0s64CbBEMQ4lyxZ+ggQwbo0sMdBCsYLOcXStVXGYpXTpJ+UASd+AhDee7lbNEE\n"
          + "o6i2YSNEoM137FQkwxCnePSNl4rq2aourGx6MIHXySZw9KIVkLi/2swCCqgJGH1a\n"
          + "SHt9kjg54HFmjbbnVsI7S5eC1Bg0u9fxcFWBmMg6ZDjkLScS6QxTXuFMrm0wQpTD\n"
          + "c+Ngqiv+WjMLUKwgYgFBiIGr9fOpittsIrYwcXHDPeTv77BWf7wsQb0u4FdP9D52\n"
          + "P0tuv7KR8e0VAO7kxt1T2H84xgwuRXnzQXTDNCUA6YFMItvlAiHXeji+BN3zt9HD\n"
          + "4RSBJVsIIb5FaxXpCqZ67WavaUHodzxgKklFLDQY50l/tZ+lwasEAu2eSH762WAt\n"
          + "k2HV3rc8dQr4DSlSFbF+YqYE7ewHGWMzjXONGKNiHrqxRaP5PVncR1I/2ES1wZNL\n"
          + "hJs6dQc6x4yy3TfGQi0OvcKXJBNUNb6OugO8KZLHyfcBp+Djwd6/qNMSeBXAv0MZ\n"
          + "brnIFYbtwYYyn6P6FT5PJdkrbWonQbn2+2l4wODZlQq9NF2hxLPWpvDI/Ei+j5Hc\n"
          + "HVm9ePMxodvbML+91WveUIyCBYKpG0+nFCd8b3HyPlgNyz7VM3xawmHbED0/CvRH\n"
          + "dRmp3cnyPBNNxA0Oxl07lGsgy1WAW5gTY/576eVHcAuCy6j6hfyLg7ayBcc42pIr\n"
          + "FzrDm8HcneNnX4tA/db9tn5qkm5ZJ5Uf8ww2TgdTiJXx2QT9SCv4gZ/ryzT72nYp\n"
          + "FYeSnozkmAH4xkUM0XPNJQVXiiYJXmRMwR/sm53Mfx9R/9DKNEW2foECkCGqQMX3\n"
          + "B/jvBX56wREt1fHax/B01PgWOqRO9SPkYipDq+42jLRNMML1Rt4Wqt7TJincAg+m\n"
          + "3700GOAXnCh6vkFNkncFesuRnmH36RaGp2Q8qnQhhBYyIZs/kr6br6cIXppYMqAd\n"
          + "vc7V8+y3CU31Hm/EapOKqQP8Vns48pb1/3eS7P/+5G7wtBrMYCNN/1XVHVmB4q10\n"
          + "+82BGx83KDkQ/Rofr3pQJuQ5DPLLWWrB8U0sjNMXMicP7bjQ/Kp0BPkhLdz9Zdei\n"
          + "ulmCFtJU664PJNLFhVMMoDp5B3NPPg18Gz6/yF08Hoo4dGNk6RxlO31Z3vGCOPWC\n"
          + "6tOCDtglM6Xjj3N86pjYCP6pm4H+bGAiCNaak7G2TwqqK7mzwYQMlzDixu0xjixO\n"
          + "wsOEBBgBCgAPBQJfWdz1BQkPCZwAAhsuAikJEBt66ktGctHhwV0gBBkBCgAGBQJf\n"
          + "Wdz1AAoJEH1tVC6w0HImV6IQAMEeBqlLZ2y6SStCHo5xlkKSeg9fPrktgiuxV4c+\n"
          + "GhezjpwzyZOO4x+DAUceDG8zGLPCHcLHHjNCCcwunRhdtpPqTsdbVGzGt6Yub2Ey\n"
          + "50+UAzYW3+3nptLtUgHkd12sJZnnZodIDtSROY5sfgKnDCma6g1nSXLSyzZvGRni\n"
          + "7JjdjY9ZLqmiunYO4N1bkihIYIm1ROQlqzdw7Lo/rnycPWS59aidhFMsuHYVpqyB\n"
          + "ulebNiPYkeI2cC0ky20qW/eu/Kmrkd6ZfB0tmw+KT16a7R3fWbF4B245w0JvatQQ\n"
          + "fOd++PW7QTypSW4mjNxnRQWbEa02TDnI0TMtgGBEwQ4gZoCkiDvENlynOqNUqWsA\n"
          + "vwqX+TTCfjoBS/z4/g78pu0ZqO2ni1t39z58JPJ0JF//voeQQ52htnpsVRGVzcFA\n"
          + "vKzYfSRyKLqiDHmCqRYHVWx5yR+YUsFNWdLhZJBgs0xkJY1GD80aGPlh/UvoJSKK\n"
          + "K78zJWvBXpUTlxUSZ4ezvXdTcsM9iF6javPLQO3MwrgT4ieLJAjNAsxKOx5h0Vkd\n"
          + "EPF/sEwY/cjB4FlwnPmIkbP5ZX9sKNPSYhNztTUVk5ESJok5w9bDB++vrKVAucv9\n"
          + "dXWg2mBvFoIvF2uEMnzsiYv1dSE1PKqkZ0actyTIGlnIO+d22s+7JZauzQjqkurR\n"
          + "W23sArkQALb5KMcOzuW0ICqCniVHvTu9dZDNeY1INTcuP47TEGfF8Y7/rYUZWLD+\n"
          + "ElR6e2ZSDR1ghp5gPRLKqEELSU+s9DDiT8oWP7uAa3KiPG60qVxGrGgJwhBmqiVe\n"
          + "Jwn4CLsoARTCkhKi6ycLES8XPNfXVuBzsTeYe81ZzEEyz6mqo5w0B58VSdRm9Tpm\n"
          + "r692JTMHjCP7RB7IgqNCw0UKTEct2vUg6Q7cwYOc5vxwPID6Pk51ttQxNTFjmqgt\n"
          + "mt8EGbsGwNJHiZfLg7NPOpySVREt5I4nKSpQoYtEEqfiP7c8BuHl1cSTU9v2H8JU\n"
          + "EgVew4KcwYQCxV3VwgAqgubiDHsVNgRs7+FgnUx0jXqBxJEX6cKKr3VkpyAPOM3t\n"
          + "p3h5fDHakPNTNRb83Zkc3wvkQ7rNoGi/W46aPBOI3p4jK40+zQeg5g144vR6EZhA\n"
          + "4yCm99goKwgMhIpdUnAs/MNvf7ougGniR/HbosW3OGQz5ACGvRlGvD8VAvkkCl/1\n"
          + "DGU696aM76FNVSod/MpMWGe9fDbtyzM2xWpyYExL9/tEdO7x0Qm9yIkTB5p0QRBX\n"
          + "kTqLFvsroya4irvuwjrV/iPtQmDo0Q0g8g9y5CreWJaoIbu8ecz7z688P4W3zQ2E\n"
          + "1tP1uAWN85qB9YU3DHCW6tj/saIlAvfhJHgngkZT+L2Mt94hSpEKx8aGBF9Z3PUB\n"
          + "EADIDNlLgE0C/WTzeUmJNKeXQPUJFDKPpLhHCwn8qAyv9sviOFiHa4lF5jDoEysZ\n"
          + "odBtH7uuF983TnFv63T1l4l6B2loX2N4K5nev2VqDB668IzircqNxY0BVUq17uXm\n"
          + "2uYqkWvUrfNczLbKMWQQazNGb7Hmh7HeNayuhN5PPsNB9HC5oVDjaM/5pz4K0Dsp\n"
          + "kEpf0+t/iI1Xbkx6mr+pp9KCke74MfJgeNcwkxNxH27PJF8XeYXgxVFbli0SkCFp\n"
          + "WIwp9ZBETEOPUJCNYxNiBmPPed5iC59l0zxwzTJiHaQsCxJJlrX7qcaStsUgsnnc\n"
          + "rTdfNGTFp7bdTdnJFHQmcEW68hhBG855dicnaUD1/b4Yyta1uKOXBJQ0K3W0nR0L\n"
          + "NRg5A65kqFiikodvdpuoUp+qmIv+gCxj9q8IjbdYWhLaxxmoAj4HE5k45zSfb1WX\n"
          + "BhnidjuRYiAU3EfOBGRFt0ZGofJPv3JoFOF/SH1HfDiztDYfYk6GSaZUVmu9PsF/\n"
          + "hyDM6XWDlgIgyVsVnB2VV609q3SrV+/DLlXmI8gjr7KY7JNhzhFGmZlHO3jY1ogw\n"
          + "+j1v4y4Q6A9Gmarhz2EvWnfPXIsgCv6+XHr5DW7Y3AlRY6fGIdfzMxaV9mCDfPgi\n"
          + "TV6ilCLuyz4P7HtYRRi70Ke+ecWrPkham5nSsCbJGn7bvwARAQAB/gkDCG2Ogvu4\n"
          + "KGtdYDSh6JC0AY+X2kAUDmODEKNdTOAkNbQaEf4NbtNh1zBJYTNwHrtjtwdnaIDk\n"
          + "XjKuA8cQgXqyOu2HmoIiw95LKEKm0evYNAIJy1BDTphpLkLf90zXKuBtN/y+0Cqs\n"
          + "I43AohFqi0F26Dzh71mqfzEaXc8OLngZs4Fgva4goPIp8T0jQlUHSnrVOH2Y/kQ3\n"
          + "g3DMxT8ZyApdti1H9jyBYrxbghVnZJr/J1RPKl3RLG9cn5rzgPgOABgjlpsvxPby\n"
          + "zcHFEmpp0WrLTjAh2hQdBrhQENvFjUGwbMJiQY71MetYLXOTtVo+1gqXuXLjFKHu\n"
          + "az4z+bJ1alFlgBC+2H1ANzHHh4ySWHyXjx1PC1auRqbc3TaTsLv9hTHBP0EfQq5C\n"
          + "2UtQUfQwTfU+Id0VkL/R9WJpr8JI3BAMT3hyf/u80VfnjoUKA80IRQhDarAqClQx\n"
          + "WLYLt7PpwJEZvwXa9cxyNacY9h9MR/dD/RZXpcAvFpvJ/Dyxqe7Z+pFQxjbBaVrH\n"
          + "3YFkfP7aR4sV7Ech8epXTlZlQCOR8P4AEhjDDJeuhGTcECCeDLWGaGhT3qDKOoDu\n"
          + "w8R1kAS1KqfoJwvNhLCjj8lSLTnwG7FPQ7xX0amvkVxixEkR+ZrMbf8mCz+oqn51\n"
          + "G2yUKWOY0TpJjO+43MipLij/KkwY7OCGiIOZODwbWOmZc/he6M1V6xHPek1VQAmQ\n"
          + "6By/vPQX/VYu1vEA6UsUeT2jQrdzN/9peiX4GjNR+oWhqvf30qy+PgVXfmyU9if4\n"
          + "kZsKgWcXsgp1gK7m09d8ZGnd46d79VpCk/GNUjadsPc10M907EzZZllfXHJURqFY\n"
          + "kE6JOz5jLHFREvrpea3jO6wVomjcfGTrRcksHoBrZtEU9pMcFloWCpVdcPQaj6ys\n"
          + "p15/BN8Bv3rxsc/aJeRLCgwNw3eG3wRNCasPGD2f23iDbgm82t0FyG+dzIsgjHxD\n"
          + "BH8aH0R0mLRzKYDkmPaBC5nlGlIP87chMyDqAjN7VLPMlZjaCTqAq7qZ7bKM2wTx\n"
          + "GNatUek2zMWIzYH6rArdcsU254rrLsvOHTQRAqmjxwSV6FeLexHlo6A2ZV12VClS\n"
          + "s+NSKD07Z0dAUNa1Lq3zL6TuACa/gzp8GATB/pzhBdI79wUKMpvhX5PZNmNsMh5r\n"
          + "L1j6cXBmGaF3fmclbytbZWKZoUCtenyn2hBJ+TIj5gAALijOfcn2KK3e+6QIYbTP\n"
          + "GVXAgmDLptaWWJRE9rmTsc7eAWyBxaC1VLgdKNaw1Q0kTDzdKRwlH3I+fZEM25Tp\n"
          + "6ACtvPCYr/OjS8QxmWBID9ovI1Jr+2iOycqsQt7B6gt4Q5A7KC4D5WO0YeglvjQ+\n"
          + "oMQaOJazjUaMjl2XcORIDzcU5vwpt1AU0Si9TNBo1+DMNd6M7f+l3wxezgclMwl7\n"
          + "i4m3eWxfMtmkC15iejH9+tNTfNJ3zNUHOx4Cps8pESEXJKCm4BcObiwqGvYjghsq\n"
          + "3j+ghbng3wKgC6VSe+nGNG71JU4/Is5PM7yqz3nJUjuMMpTpmBBFmh2g6e4EqKh5\n"
          + "bMqr1oDxcH8zsv29VoqCSJkVt39i/+sVbpESbFlks0/dx9pDcp4ATT6AdFCNnaOV\n"
          + "zeI1GmdVv024VNR6usPwGwdpybOnGvga50CO21bOsjAedCxtw/cAQENYOAshuyWJ\n"
          + "DyzUCz7QBP9XoUDAQfcNhSx7uwXkcyvrQUE6KNNnwdul85XYzQ6iGKdzazgkcysq\n"
          + "Ehu0Xlx/2a3jWmcT9NctfRBZRtOpCtwnddAy3JgLuBXCw4QEGAEKAA8FAl9Z3PUF\n"
          + "CQ8JnAACGy4CKQkQG3rqS0Zy0eHBXSAEGQEKAAYFAl9Z3PUACgkQGR36IZ6sn9If\n"
          + "JRAAg70fbQIhyHqcc2eHNNsvmm+6IRpE1l015H+ZPGPccUoKO48ku26Oc1xHas2n\n"
          + "QGJk0Gm4tFbEJg8tleVAMcw/crOew7wVD0GrYD474wK6we3ZVMgGLYLPHaw4tuJi\n"
          + "DJF1P/WktJSfC2P0boeBfLIbMKMjEHH3tGdXPmWLh0GOK0Yiz4GFN90Ogt0Tl4QE\n"
          + "C+zx9HC0DDrWp1kpzA/U5uCl9E05UaDzH3HxyGkig2lx4dmwGcgCHQJQiCJJP08c\n"
          + "Yhk6SSWBr5DdNiq3XX/m+Tk8uzoTLlJr1ftuiLNIAAwbMxY5i6XWOzLNX2nTCnzj\n"
          + "IJ0KuhqR7DHhqT1sjCUjXXeOd52X9E+AEp4OYJtkEoaN3VGyTcXj4gEvVUIRWSh1\n"
          + "8iHuvFd65cnX2HLVnbb9qWhjBEKDt3LAas1cHwgsu66TnI5mutCI40g5YHYN84cv\n"
          + "mrCyey0HaSP+lx91uNMCIXWFiV54jFtJQrlWIP9wT/R0YSPhqTx0xhP5lRQfL9EO\n"
          + "LhowCYVAbW/cehFlHt04zOKNRi2hQ7pYMoJ44p6E5s4ViBMM5kDJ3kTDm2vVp5Yr\n"
          + "8ilGKuVc6WXmjSVQ6VUObNNxHJOT8Al96YNqsJgrwGW58BzeB/kSgr1XFT9J/j4E\n"
          + "M69W4wSX3GkIhOo5Y+SFw5O3RnIlXTEZMcx7azSliVueNf+zmg/6AkTUqZgBCh59\n"
          + "ETdlAjESM38hK9zZudRvKaWZWrVr3yhugFkRS/ahz6uDC7BLWMQSLsj/Na4ejy12\n"
          + "Xw1A0K+1KnBRgMUwUgAuzeZKFqvK5I2wMPiqXGWm9wOTmz48ne8SgtAvzkEKj2Rh\n"
          + "2iW8YRMB7lzeRxAWiGlW2/augJjfOnpLIPgCiNYEK2qmvNOXq8ONRbp07VhxycYD\n"
          + "3dqjXoKP8oTBZ25GfV79P2M1I4XXSknFpIQEGmWhtGUqHjD7zu+aDPDhqmaiFQRS\n"
          + "9Sia7r8twAkF0jsx5MAm8LOiwOtKrqqvSb65GiyBdjegSLAkqswGrynMerxtNrV9\n"
          + "r1o5xHvHz3j8pITnTc4iUxlLJNlBdOFubjlAbWgOX8+lJpYxNevC6y/stcam//Nj\n"
          + "kZVlqTc+R2U+CspYO+O3ddBSx5/tR3ODOs3Q1TMe7fO/+bAuL7h6G/32O+najIAH\n"
          + "OBPva6I8JNfo1EJAdlgqOiqmw1RQEC19C79r6G9BsFTXrkoH3GtTaW/yP1YCg5Zz\n"
          + "msXTTCudnSPIGMBdZnZDHP8dZKmWFiioGs/wzwmDQOYkz61e61A7EvTPMCnBq9RC\n"
          + "ctMGVpjzu55D7qh4g5KFcu9fmZKhR7W//rcWlfNfNnUEspyBxvxV6K2Ae+VmzACA\n"
          + "mHBStBcWhiOjqV8b2LdN0n0am9LdJOg=\n"
          + "=QYfZ\n"
          + "-----END PGP PRIVATE KEY BLOCK-----\n";

  private static final String FIELD_PUBLIC_KEY =
      "-----BEGIN PGP PUBLIC KEY BLOCK-----\n"
          + "Version: Keybase OpenPGP v1.0.0\n"
          + "Comment: https://keybase.io/crypto\n"
          + "\n"
          + "xsFNBF9Z3PUBEADUBsIfygzVjU21g0fg4kJ61Alodyg2EKQLRZQVmsB2Hg3aD2AJ\n"
          + "4prEW36rliS9svDR/Z3zZ2TnqtDw44TTARj7m/P5YV5WE+z85tzsqFhjd2lfDf2X\n"
          + "wpfuMS6wk8PosALOmp4LkQkzuvMMs1w+FsQ9ufhGsY7joE7ZZG9nosdZeF2XyV+r\n"
          + "7+xBqT3IRgfMla3ZcacqTRRuLmCe9xcud4YY16Vpoig+n1HQI74XB0Hm4OVEcmX5\n"
          + "RVmC/HjL9Z2B0JrB1WxGyV+GaMyFEJUobdjZ4llUxEWtd2sVmvbapREnhg+fQ811\n"
          + "ZddR3JrBi8bfxLxibUqMyEksL7NvZmFBOVhRRQknkpI7gQnTYkinS0QECOLEP6qa\n"
          + "LP+CJifPRcvm7nj8aGstQpu7CWPGM/lOZzo9/N7nqHHU3lcfpb4PVzEJX+5Fr7C/\n"
          + "F1iV4JKSyBZKl4PVTtSwgVAbnebTyp+0cXS/ih6eFNX4aYl8Au7O983kQpfoIBb6\n"
          + "j8eVkddQC+5aEY417pcHO4WoSMvnMh8tJyqBT62OR3oO2MAm/yEj49EIAg0Hw18y\n"
          + "xgUTMyD77eLJB1iUc5AJPTCeaG4Rh86ujSvfSYi8YJ4TDGeU9/HD01s2KwSy0g0s\n"
          + "EVJz5VRRkZAWHwcXeS0fYgPnAudBz7/u6S1R8c2wXN84BeP5xaVBX8Z4jQARAQAB\n"
          + "zRxmaWVsZC50ZXN0IDx0ZXN0QGRvbWFpbi5jb20+wsFtBBMBCgAXBQJfWdz1Ahsv\n"
          + "AwsJBwMVCggCHgECF4AACgkQG3rqS0Zy0eHV0xAAnPfPMmIr3aRSH9VpFmzNJ0Sk\n"
          + "kJrKUEo1XkjF0LzXFwx0A5LZXK3gsBdPFO5ntPxi+Zi7nVtUIeCxWcai5+2LzYiS\n"
          + "RIoLZzAUXtJag5XDw1dCYR3eajn9J61+D6MvqijE0EMPp3Ubo/KArYUKzx+i6VLw\n"
          + "gpI5lgNYFk3uNAFhiT/TzJyXbbR9GAesWnT5N2Pu69NZ5x2lUKYUKKiMIOrlwtMJ\n"
          + "ThHOzzAXbZ/4jzyzyYK772zh8PzLsSOQCEmZnVIW47IMqLAUdD1HOTA90ZzrnNv/\n"
          + "op/UQ6LcArOB0pyN5eFGXuvulgZ2/lheI4PIi9HEAXnq91Hv/l6+NqZEjrBOOooo\n"
          + "7QsNTR3n/9AS3qIVpKM1l6p72T41GF4aJP07MrLgnnBYH3m0di97RWoFO5OVcIWj\n"
          + "SnX1oJLGYl9Pok/E8HNE+GzR7g8yM4tfBDh+tsMNT8gzpPiS7bGtyMC5pbsilIAR\n"
          + "78hn503n1trwaXunFsVmLiak0V+a5e/qWvS42XAfDSGIyV/K5/XHzE86+1uieRw3\n"
          + "0bfK7EqZo8BEE/cpH4ijLuWGV5P/lsY8RrWSuKgjVERh/pev4KFA7zAS8FF/8A4C\n"
          + "0LWhRW0AwkYEgZteZQ3av46vtrcGmQ8sboYgSHgD09eTMJ2o59qjhsAlb2U4Z04+\n"
          + "S17GcWq6Q8uzAPNNTVbOwU0EX1nc9QEQAMElF5A8IyLTNl3sJnEKa85yZETb4EgT\n"
          + "QIyaltJbUB3U/EG79llts/SEpQ73N/6PtBeVkitM1NG+9P2QG1WmAqIAfPnFtE49\n"
          + "liMRucd5NRkWoe9GbHJ0pkh3aHu7Ln78zosKumO+aBJFfyBHgMQyEfA9KZKka3m4\n"
          + "8t+o0PO8Gt75v2gCbJJnlajL8AARdbQ9NO7VhNjfD7RUm80VHeFofwEDi9FGR4Rj\n"
          + "l2fMK6yJOvutpkInIXDedZr8Jw33sAPc65AnzHov3OWTgV9OfYO8rovH6TDfnLTK\n"
          + "u3ryr/MihRWa8UCUnrxlFs+brLPwFBInUVzs1y5hwKsDJSsU72FQOTd2zlsZdVmN\n"
          + "dR4qN+PyHUxs7FpNpa2ld9jPfWUmvKqWBql14QivcrB+S1GwTwPq15KEuFL/g85R\n"
          + "oRZJeqOsiKlMsjr/VPyJA9TawV68QgoXQ/RXJ7fPnBus76M4KKJeNmsFUUXBk6eq\n"
          + "NATZ0jG0jFMEq60KlxB76oZ2xcyVyHQsAShxNABOo5cR5vZRYQNWZNV5sgKPlsoa\n"
          + "K4Z96/Vg4deioU3yZutnty4tqEy4VH/vZhNjNFDjV4F7+zR91fIFGvpK06ql7vkV\n"
          + "2pWXg4v0Sut6O5Yqi7uHFr75PZgHYzYvTZzb1nVFRMQWEqI6og5oILjI0UZwE9LV\n"
          + "TJFRvjugi8VJABEBAAHCw4QEGAEKAA8FAl9Z3PUFCQ8JnAACGy4CKQkQG3rqS0Zy\n"
          + "0eHBXSAEGQEKAAYFAl9Z3PUACgkQfW1ULrDQciZXohAAwR4GqUtnbLpJK0IejnGW\n"
          + "QpJ6D18+uS2CK7FXhz4aF7OOnDPJk47jH4MBRx4MbzMYs8IdwsceM0IJzC6dGF22\n"
          + "k+pOx1tUbMa3pi5vYTLnT5QDNhbf7eem0u1SAeR3Xawlmedmh0gO1JE5jmx+AqcM\n"
          + "KZrqDWdJctLLNm8ZGeLsmN2Nj1kuqaK6dg7g3VuSKEhgibVE5CWrN3Dsuj+ufJw9\n"
          + "ZLn1qJ2EUyy4dhWmrIG6V5s2I9iR4jZwLSTLbSpb9678qauR3pl8HS2bD4pPXprt\n"
          + "Hd9ZsXgHbjnDQm9q1BB853749btBPKlJbiaM3GdFBZsRrTZMOcjRMy2AYETBDiBm\n"
          + "gKSIO8Q2XKc6o1SpawC/Cpf5NMJ+OgFL/Pj+Dvym7Rmo7aeLW3f3Pnwk8nQkX/++\n"
          + "h5BDnaG2emxVEZXNwUC8rNh9JHIouqIMeYKpFgdVbHnJH5hSwU1Z0uFkkGCzTGQl\n"
          + "jUYPzRoY+WH9S+glIoorvzMla8FelROXFRJnh7O9d1Nywz2IXqNq88tA7czCuBPi\n"
          + "J4skCM0CzEo7HmHRWR0Q8X+wTBj9yMHgWXCc+YiRs/llf2wo09JiE3O1NRWTkRIm\n"
          + "iTnD1sMH76+spUC5y/11daDaYG8Wgi8Xa4QyfOyJi/V1ITU8qqRnRpy3JMgaWcg7\n"
          + "53baz7sllq7NCOqS6tFbbewCuRAAtvkoxw7O5bQgKoKeJUe9O711kM15jUg1Ny4/\n"
          + "jtMQZ8Xxjv+thRlYsP4SVHp7ZlINHWCGnmA9EsqoQQtJT6z0MOJPyhY/u4BrcqI8\n"
          + "brSpXEasaAnCEGaqJV4nCfgIuygBFMKSEqLrJwsRLxc819dW4HOxN5h7zVnMQTLP\n"
          + "qaqjnDQHnxVJ1Gb1Omavr3YlMweMI/tEHsiCo0LDRQpMRy3a9SDpDtzBg5zm/HA8\n"
          + "gPo+TnW21DE1MWOaqC2a3wQZuwbA0keJl8uDs086nJJVES3kjicpKlChi0QSp+I/\n"
          + "tzwG4eXVxJNT2/YfwlQSBV7DgpzBhALFXdXCACqC5uIMexU2BGzv4WCdTHSNeoHE\n"
          + "kRfpwoqvdWSnIA84ze2neHl8MdqQ81M1FvzdmRzfC+RDus2gaL9bjpo8E4jeniMr\n"
          + "jT7NB6DmDXji9HoRmEDjIKb32CgrCAyEil1ScCz8w29/ui6AaeJH8duixbc4ZDPk\n"
          + "AIa9GUa8PxUC+SQKX/UMZTr3pozvoU1VKh38ykxYZ718Nu3LMzbFanJgTEv3+0R0\n"
          + "7vHRCb3IiRMHmnRBEFeROosW+yujJriKu+7COtX+I+1CYOjRDSDyD3LkKt5Ylqgh\n"
          + "u7x5zPvPrzw/hbfNDYTW0/W4BY3zmoH1hTcMcJbq2P+xoiUC9+EkeCeCRlP4vYy3\n"
          + "3iFKkQrOwU0EX1nc9QEQAMgM2UuATQL9ZPN5SYk0p5dA9QkUMo+kuEcLCfyoDK/2\n"
          + "y+I4WIdriUXmMOgTKxmh0G0fu64X3zdOcW/rdPWXiXoHaWhfY3grmd6/ZWoMHrrw\n"
          + "jOKtyo3FjQFVSrXu5eba5iqRa9St81zMtsoxZBBrM0ZvseaHsd41rK6E3k8+w0H0\n"
          + "cLmhUONoz/mnPgrQOymQSl/T63+IjVduTHqav6mn0oKR7vgx8mB41zCTE3Efbs8k\n"
          + "Xxd5heDFUVuWLRKQIWlYjCn1kERMQ49QkI1jE2IGY8953mILn2XTPHDNMmIdpCwL\n"
          + "EkmWtfupxpK2xSCyedytN180ZMWntt1N2ckUdCZwRbryGEEbznl2JydpQPX9vhjK\n"
          + "1rW4o5cElDQrdbSdHQs1GDkDrmSoWKKSh292m6hSn6qYi/6ALGP2rwiNt1haEtrH\n"
          + "GagCPgcTmTjnNJ9vVZcGGeJ2O5FiIBTcR84EZEW3Rkah8k+/cmgU4X9IfUd8OLO0\n"
          + "Nh9iToZJplRWa70+wX+HIMzpdYOWAiDJWxWcHZVXrT2rdKtX78MuVeYjyCOvspjs\n"
          + "k2HOEUaZmUc7eNjWiDD6PW/jLhDoD0aZquHPYS9ad89ciyAK/r5cevkNbtjcCVFj\n"
          + "p8Yh1/MzFpX2YIN8+CJNXqKUIu7LPg/se1hFGLvQp755xas+SFqbmdKwJskaftu/\n"
          + "ABEBAAHCw4QEGAEKAA8FAl9Z3PUFCQ8JnAACGy4CKQkQG3rqS0Zy0eHBXSAEGQEK\n"
          + "AAYFAl9Z3PUACgkQGR36IZ6sn9IfJRAAg70fbQIhyHqcc2eHNNsvmm+6IRpE1l01\n"
          + "5H+ZPGPccUoKO48ku26Oc1xHas2nQGJk0Gm4tFbEJg8tleVAMcw/crOew7wVD0Gr\n"
          + "YD474wK6we3ZVMgGLYLPHaw4tuJiDJF1P/WktJSfC2P0boeBfLIbMKMjEHH3tGdX\n"
          + "PmWLh0GOK0Yiz4GFN90Ogt0Tl4QEC+zx9HC0DDrWp1kpzA/U5uCl9E05UaDzH3Hx\n"
          + "yGkig2lx4dmwGcgCHQJQiCJJP08cYhk6SSWBr5DdNiq3XX/m+Tk8uzoTLlJr1ftu\n"
          + "iLNIAAwbMxY5i6XWOzLNX2nTCnzjIJ0KuhqR7DHhqT1sjCUjXXeOd52X9E+AEp4O\n"
          + "YJtkEoaN3VGyTcXj4gEvVUIRWSh18iHuvFd65cnX2HLVnbb9qWhjBEKDt3LAas1c\n"
          + "Hwgsu66TnI5mutCI40g5YHYN84cvmrCyey0HaSP+lx91uNMCIXWFiV54jFtJQrlW\n"
          + "IP9wT/R0YSPhqTx0xhP5lRQfL9EOLhowCYVAbW/cehFlHt04zOKNRi2hQ7pYMoJ4\n"
          + "4p6E5s4ViBMM5kDJ3kTDm2vVp5Yr8ilGKuVc6WXmjSVQ6VUObNNxHJOT8Al96YNq\n"
          + "sJgrwGW58BzeB/kSgr1XFT9J/j4EM69W4wSX3GkIhOo5Y+SFw5O3RnIlXTEZMcx7\n"
          + "azSliVueNf+zmg/6AkTUqZgBCh59ETdlAjESM38hK9zZudRvKaWZWrVr3yhugFkR\n"
          + "S/ahz6uDC7BLWMQSLsj/Na4ejy12Xw1A0K+1KnBRgMUwUgAuzeZKFqvK5I2wMPiq\n"
          + "XGWm9wOTmz48ne8SgtAvzkEKj2Rh2iW8YRMB7lzeRxAWiGlW2/augJjfOnpLIPgC\n"
          + "iNYEK2qmvNOXq8ONRbp07VhxycYD3dqjXoKP8oTBZ25GfV79P2M1I4XXSknFpIQE\n"
          + "GmWhtGUqHjD7zu+aDPDhqmaiFQRS9Sia7r8twAkF0jsx5MAm8LOiwOtKrqqvSb65\n"
          + "GiyBdjegSLAkqswGrynMerxtNrV9r1o5xHvHz3j8pITnTc4iUxlLJNlBdOFubjlA\n"
          + "bWgOX8+lJpYxNevC6y/stcam//NjkZVlqTc+R2U+CspYO+O3ddBSx5/tR3ODOs3Q\n"
          + "1TMe7fO/+bAuL7h6G/32O+najIAHOBPva6I8JNfo1EJAdlgqOiqmw1RQEC19C79r\n"
          + "6G9BsFTXrkoH3GtTaW/yP1YCg5ZzmsXTTCudnSPIGMBdZnZDHP8dZKmWFiioGs/w\n"
          + "zwmDQOYkz61e61A7EvTPMCnBq9RCctMGVpjzu55D7qh4g5KFcu9fmZKhR7W//rcW\n"
          + "lfNfNnUEspyBxvxV6K2Ae+VmzACAmHBStBcWhiOjqV8b2LdN0n0am9LdJOg=\n"
          + "=Hgf6\n"
          + "-----END PGP PUBLIC KEY BLOCK-----\n";

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

  private PGPPublicKey getPublicPgpKey(String pgpKey) throws IOException, PGPException {
    InputStream encoderStream =
        PGPUtil.getDecoderStream(new ByteArrayInputStream(pgpKey.getBytes()));
    JcaPGPPublicKeyRingCollection pgpPub = new JcaPGPPublicKeyRingCollection(encoderStream);
    encoderStream.close();
    if (pgpPub.size() < 1) {
      throw new IOException("No key available");
    }
    return pgpPub.getKeyRings().next().getPublicKey();
  }

  private String encrypt(String plainText, Set<PGPPublicKey> publicKeys)
      throws IOException, PGPException {
    final PGPEncryptedDataGenerator generator =
        new PGPEncryptedDataGenerator(
            new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                .setWithIntegrityPacket(true)
                .setSecureRandom(new SecureRandom())
                .setProvider(PROVIDER));
    for (PGPPublicKey publicKey : publicKeys) {
      generator.addMethod(
          new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider(PROVIDER));
    }
    ;
    ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();
    OutputStream armoredOutputStream = new ArmoredOutputStream(encryptedBytes);
    byte[] inputData = plainText.getBytes();
    OutputStream encryptedOut = generator.open(armoredOutputStream, inputData.length);
    encryptedOut.write(inputData.length);
    String cipherText = encryptedBytes.toString();

    // Close all streams for safety
    encryptedBytes.close();
    armoredOutputStream.close();
    encryptedOut.close();
    return cipherText;
  }

  private PGPPrivateKey getPrivatePgpKey(String pgpKey, char[] passPhrase)
      throws IOException, PGPException {
    InputStream decoderStream =
        PGPUtil.getDecoderStream(new ByteArrayInputStream(pgpKey.getBytes()));
    JcaPGPSecretKeyRingCollection pgpSecret = new JcaPGPSecretKeyRingCollection(decoderStream);
    decoderStream.close();
    if (pgpSecret.size() < 1) {
      throw new IOException("No key available");
    }
    PGPSecretKey secretKey = pgpSecret.getKeyRings().next().getSecretKey();
    System.out.println("Public key associated with secret key: " + secretKey.getKeyID());
    System.out.println("Is private key empty:" + secretKey.isPrivateKeyEmpty());
    PBESecretKeyDecryptor decryptor =
        new JcePBESecretKeyDecryptorBuilder().setProvider(PROVIDER).build(passPhrase);
    return secretKey.extractPrivateKey(decryptor);
  }

  private String decrypt(String cipherText, PGPPrivateKey privateKey)
      throws IOException, PGPException {
    InputStream decoderStream =
        PGPUtil.getDecoderStream(new ByteArrayInputStream(cipherText.getBytes()));
    PGPObjectFactory factory = new JcaPGPObjectFactory(decoderStream);
    final Object first = factory.nextObject();
    final Object list = (first instanceof PGPEncryptedDataList) ? first : factory.nextObject();
    Iterator<PGPEncryptedData> iterator = ((PGPEncryptedDataList) list).getEncryptedDataObjects();

    // Just gets required key, already know key
    System.out.println("Private keyId: " + privateKey.getKeyID());

    PGPPublicKeyEncryptedData encrypted = null;
    boolean key = false;
    while (!key && iterator.hasNext()) {
      encrypted = (PGPPublicKeyEncryptedData) iterator.next();
      System.out.println("Packet keyId:  " + encrypted.getKeyID());
      key = encrypted.getKeyID() == privateKey.getKeyID();
    }
    PublicKeyDataDecryptorFactory dataDecryptor =
        new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(PROVIDER).build(privateKey);
    try (InputStream decryptedStream = encrypted.getDataStream(dataDecryptor)) {
      JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(decryptedStream);
      Object message = plainFact.nextObject();

      // If the message is literal data, read it and process to the output stream
      if (message instanceof PGPLiteralData) {
        PGPLiteralData literalData = (PGPLiteralData) message;
        String plainText =
            new BufferedReader(
                    new InputStreamReader(literalData.getInputStream(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        return plainText;
      } else {
        return "";
      }
    }
  }
}
