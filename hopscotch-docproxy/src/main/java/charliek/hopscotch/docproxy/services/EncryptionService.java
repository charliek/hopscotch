package charliek.hopscotch.docproxy.services;

import charliek.hopscotch.docproxy.exceptions.HopscotchException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class EncryptionService {

	private static final int ITERATION_COUNT = 65536;
	private static final int KEY_LENGTH = 256;

	private final Cipher encryptor;
	private final Cipher decryptor;

	public EncryptionService(String password, String salt) {
		try {
			// build up a key with the given password and salt
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(password.toCharArray(),
				salt.substring(0, 8).getBytes(StandardCharsets.US_ASCII), ITERATION_COUNT, KEY_LENGTH);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

			encryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
			encryptor.init(Cipher.ENCRYPT_MODE, secret);

			decryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] iv = encryptor.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
			decryptor.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
		} catch (Exception e) {
			throw new IllegalStateException("Unable to initialize Ciphers", e);
		}
	}

	public String encrypt(String text) {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		byte[] encrypted = encrypt(bytes);
		return Base64.getEncoder().encodeToString(encrypted);
	}

	public byte[] encrypt(byte[] bytes) {
		try {
			return encryptor.doFinal(bytes);
		} catch (IllegalBlockSizeException|BadPaddingException e) {
			throw new HopscotchException("Error when encrypting bytes", e);
		}
	}

	public String decrypt(String text) {
		byte[] bytes = Base64.getDecoder().decode(text);
		byte[] decrypted = decrypt(bytes);
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	public byte[] decrypt(byte[] bytes) {
		try {
			return decryptor.doFinal(bytes);
		} catch (IllegalBlockSizeException|BadPaddingException e) {
			throw new HopscotchException("Error when decrypting bytes", e);
		}
	}
}
