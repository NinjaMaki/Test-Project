package ninja.maki.utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RSAUtil {
    private final Map<Key, String> map = new HashMap<>();

    public RSAUtil() {
        generateKey();
    }

    public String getPublicKey() {
        return map.get(Key.PUBLIC);
    }

    public String getPrivateKey() {
        return map.get(Key.PRIVATE);
    }

    private void generateKey() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGen.generateKeyPair();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            String publicKeyString = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
            String privateKeyString = new String(Base64.getEncoder().encode((privateKey.getEncoded())));
            map.put(Key.PUBLIC, publicKeyString);
            map.put(Key.PRIVATE, privateKeyString);
        }catch (NoSuchAlgorithmException e){
            Logger.log(e.getMessage(), Logger.Type.ERROR);
        }
    }

    public static String encrypt(String str, String publicKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(publicKey.getBytes(StandardCharsets.UTF_8));
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            return new String(Base64.getEncoder().encode(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
        }catch (Exception e){
            Logger.log(e.getMessage(), Logger.Type.ERROR);
            return null;
        }
    }

    public static String decrypt(String str, String privateKey) {
        try{
            byte[] inputByte = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
            byte[] decoded = Base64.getDecoder().decode(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            return new String(cipher.doFinal(inputByte));
        }catch (Exception e){
            Logger.log(e.getMessage(), Logger.Type.ERROR);
            return null;
        }
    }

    enum Key {
        PUBLIC, PRIVATE
    }
}
