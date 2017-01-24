package ru.ogpscenter.maps3d.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class CryptoUtil {
  private static Random ourRandom = new Random();
  private final static int LENGTH = 12;

  public static String hash(String input) {
    try {
      return hash(input.getBytes("UTF-8"), "SHA-512", false);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String hashBase62(byte[] input, String algorithm) {
    return hash(input, algorithm, true);
  }

  private static String hash(byte[] input, String algorithm, boolean useBase62) {
    try {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      md.update(input);
      byte[] digest = md.digest();
      return useBase62 ? bytesAsString62(digest) : bytesAsString16(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String shortHash(String text) {
    return shortHash(text, LENGTH);
  }

  public static String shortHash(String text, int length) {
    return hash(text).substring(0, length);
  }

  public static String newSalt() {
    byte[] bytes = new byte[16];
    ourRandom.nextBytes(bytes);
    return bytesAsString16(bytes);
  }

  private static String bytesAsString16(byte[] bytes) {
    BigInteger bigInt = new BigInteger(1, bytes);
    return bigInt.toString(16);
  }

  public static String bytesAsString62(byte[] bytes) {
    BigInteger _62 = BigInteger.valueOf(62);
    BigInteger bigInt = new BigInteger(1, bytes);

    char data[] = new char[(bytes.length * 8 + 4) / 5];
    int pos = 0;

    BigInteger t = bigInt;
    while (t.compareTo(_62) >= 0) {
      BigInteger[] divRem = t.divideAndRemainder(_62);
      t = divRem[0];
      data[pos++] = toChar(divRem[1].intValue());
    }

    data[pos++] = toChar(t.intValue());

    return new String(data, 0, pos);
  }

  private static char toChar(int x) {
    return (char) (x < 10 ? '0' + x : x < 36 ? 'a' + x - 10 : 'A' + x - 36);
  }

  public static void main(String[] args) throws NoSuchAlgorithmException {

    for (int i = 0; i < 10; i++) {
      byte[] bytes = Integer.toString(i).getBytes(StandardCharsets.UTF_8);

      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] digest = md.digest(bytes);

      System.out.println("base16 = " + bytesAsString16(digest));
      System.out.println("base62 = " + bytesAsString62(digest));
    }
  }
}
