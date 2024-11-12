package org.elasticsearch.plugin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FPE {

    private static final String KEY = "1234567890abcdef";

    // 用于生成伪随机数
    private static int pseudoRandom(int input, int roundKey) {
        int randnum = (input ^ roundKey) & 0xFFFF;

        System.out.println(randnum);

        return randnum;
    }

    // 加密函数
    public static String encrypt(String plaintext) throws NoSuchAlgorithmException {
        int roundKey = generateRoundKey(KEY);
        char[] encryptedChars = new char[plaintext.length()];

        // Feistel Network
        for (int i = 0; i < plaintext.length(); i++) {
            char ch = plaintext.charAt(i);
            encryptedChars[i] = (char) (pseudoRandom(ch, roundKey) + 0x4e00); // 使用中文编码范围
        }

        return new String(encryptedChars);
    }

    // 解密函数
    public static String decrypt(String ciphertext) throws NoSuchAlgorithmException {
        int roundKey = generateRoundKey(KEY);
        char[] decryptedChars = new char[ciphertext.length()];

        // Feistel Network解密
        for (int i = 0; i < ciphertext.length(); i++) {
            char ch = ciphertext.charAt(i);
            decryptedChars[i] = (char) (pseudoRandom(ch - 0x4e00, roundKey));
        }

        return new String(decryptedChars);
    }

    // 用密钥生成伪随机数种子
    private static int generateRoundKey(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
        return digest[0] & 0xFFFF; // 截取部分伪随机种子
    }

    public static void main(String[] args) {
        try {
//            String plaintext = "你好，世界！";

            String plaintext = "hello world!";
            // 加密
            String encryptedText = encrypt(plaintext);
            System.out.println("加密后的密文: " + encryptedText);

            System.out.println(encryptedText.length());
            // 解密
            String decryptedText = "龜Ｕ\uF4BE\uF2FF";
            System.out.println("解密后的明文: " + decrypt(decryptedText));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
