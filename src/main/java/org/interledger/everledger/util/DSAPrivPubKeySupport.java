package org.interledger.everledger.util;

//REF: http://stackoverflow.com/questions/9755057/converting-strings-to-encryption-keys-and-vice-versa-java

// TODO:(core) Move to java-ilp-core/... utils/... ?
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
public class DSAPrivPubKeySupport {
 public static PrivateKey loadPrivateKey(String key64) {
     byte[] clear = Base64.getDecoder().decode(key64);
     PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
                 try{
     KeyFactory fact = KeyFactory.getInstance("DSA");
     PrivateKey priv = fact.generatePrivate(keySpec);
     Arrays.fill(clear, (byte) 0);
     return priv;
                 }catch(Exception e){
     throw new RuntimeException(e.toString());
                 }
 }

 public static PublicKey loadPublicKey(String stored) {
     byte[] data = Base64.getDecoder().decode(stored);
     X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
             try {
     KeyFactory fact = KeyFactory.getInstance("DSA");
     return fact.generatePublic(spec);
             }catch(Exception e){
     throw new RuntimeException(e.toString());
             }
 }

 public static String savePrivateKey(PrivateKey priv) {
                 try {
     KeyFactory fact = KeyFactory.getInstance("DSA");
     PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
             PKCS8EncodedKeySpec.class);
     byte[] packed = spec.getEncoded();
     String key64 = new String(Base64.getEncoder().encode(packed));
     Arrays.fill(packed, (byte) 0);
     return key64;
                 }catch(Exception e){
     throw new RuntimeException(e.toString());
                 }

 }

 public static String savePublicKey(PublicKey publ) {
     // TODO:(0) FIXME:
     // It's supposed to return something similar to
     //    2A5PxZUtFUuoL64r8oxzrsV73Y5ma76NZLUV8P2DG1M=
     // but it's actually returning something like:
     //    MIIBtzCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGAARbabwyUW4v/xtnQjbRd4iEPvHnOCQpZx5d1RbaNe1XkmYj4JNdD1kmqjBhIDD8nKSdBk2oPWpujzjPs+T//7xWxixZ6BFrhAQ8qNWXF4tZKkmjtHqxo3JWhBe5OvGwNmBR9VJ4K7Xyk/YbZX2dK6o/Gl87yh/zWiUXfGAkua7A=
                 try {
     KeyFactory fact = KeyFactory.getInstance("DSA");
     X509EncodedKeySpec spec = fact.getKeySpec(publ,
             X509EncodedKeySpec.class);
     return new String(Base64.getEncoder().encode(spec.getEncoded()));
                 }catch(Exception e){
     throw new RuntimeException(e.toString());
                 }
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