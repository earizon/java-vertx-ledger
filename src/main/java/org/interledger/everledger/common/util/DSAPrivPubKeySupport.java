package org.interledger.everledger.common.util;

//REF: http://stackoverflow.com/questions/9755057/converting-strings-to-encryption-keys-and-vice-versa-java

// TODO:(0) Move to java-ilp-core/... utils/... ?
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
public class DSAPrivPubKeySupport {
 public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
     byte[] clear = Base64.getDecoder().decode(key64);
     PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
     KeyFactory fact = KeyFactory.getInstance("DSA");
     PrivateKey priv = fact.generatePrivate(keySpec);
     Arrays.fill(clear, (byte) 0);
     return priv;
 }

 public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
     byte[] data = Base64.getDecoder().decode(stored);
     X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
     KeyFactory fact = KeyFactory.getInstance("DSA");
     return fact.generatePublic(spec);
 }

 public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
     KeyFactory fact = KeyFactory.getInstance("DSA");
     PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
             PKCS8EncodedKeySpec.class);
     byte[] packed = spec.getEncoded();
     String key64 = new String(Base64.getEncoder().encode(packed));
     Arrays.fill(packed, (byte) 0);
     return key64;
 }

 public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
     KeyFactory fact = KeyFactory.getInstance("DSA");
     X509EncodedKeySpec spec = fact.getKeySpec(publ,
             X509EncodedKeySpec.class);
     return new String(Base64.getEncoder().encode(spec.getEncoded()));
 }

 public static void main(String[] args) throws Exception {
     KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
     KeyPair pair = gen.generateKeyPair();
     String pubKey = savePublicKey(pair.getPublic());
     String privKey = savePrivateKey(pair.getPrivate());
     System.out.println("privKey:"+privKey);
     System.out.println("pubKey :"+pubKey );
     // PublicKey pubSaved = loadPublicKey(pubKey);
     // System.out.println(pair.getPublic()+"\n"+pubSaved);
     // PrivateKey privSaved = loadPrivateKey(privKey);
     // System.out.println(pair.getPrivate()+"\n"+privSaved);
 }
}