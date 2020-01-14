package rest.util;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;

/*==============================================================================================================================
 * CRYPTOGRAPHIC UTILITIES * Provides security and cryptographic methods for the web service
 *============================================================================================================================*/
public class CryptUtils {

	// Private static values CRYPTOGRAPHY
	private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
	private static final Argon2 ARGON2 = Argon2Factory.create();
	private static Map<String,Object> crypt_map;
	private static int ARGON_ITERATIONS, ARGON_MEMORY, ARGON_PARALLELISM, RANDOM_LENGTH, SIMILAR_LENGTH, PW_LENGTH, OTP_LENGTH;
	private static SecureRandom random_generator;
	private static MessageDigest hash_generator;
	private static String BASE_REGEX, NAME_REGEX, EMAIL_REGEX, PASS_REGEX, USER_REGEX, RSA_ALGORITHM, RSA_BEGIN, RSA_END;
	private static char[] PW_CHAR, OTP_CHAR;
	// static values
	static int ACCOUNT_EXPIRES, SIGNIN_EXPIRES;

	/*--------------------------------------------------------------------------------------------------------------------------
	 * UTILITIES METHODS * Methods used by the classes that extends this one
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Retrieve the next combination of RANDOM_BOUND bytes and turn it into an encoded string compatible with HTTP
	public static String getRandomOtp() { return getRandomKey(OTP_CHAR, OTP_LENGTH); }
	public static String getRandomPassword() { return getRandomKey(PW_CHAR, PW_LENGTH); }
	public static String getRandomKey(char[] rand_char, int rand_len) {
		char[] password = new char[rand_len];
		for (int i = 0; i < rand_len; i++) password[i] = rand_char[random_generator.nextInt(rand_char.length)];
		return String.valueOf(password);
	}
	public static String getRandomString() { return getRandomString(RANDOM_LENGTH); }
	public static String getRandomString(int rand_len) {
		byte[] bytes = new byte[rand_len];
	    random_generator.nextBytes(bytes);
	    return BASE64_ENCODER.encodeToString(bytes).substring(0, rand_len);
	}
	// Return the SHA256 of the given value
	public static String getHash(String value) {
		return new HexBinaryAdapter().marshal(hash_generator.digest(value.getBytes())); }
	public static String getHash(String hash, String message) {
		return encodeToString(hash_generator.digest((hash + ":" + message).getBytes())); }
	public static boolean compareHash(String hash1, String hash2) {
		return MessageDigest.isEqual(decodeToString(hash1).getBytes(), decodeToString(hash2).getBytes()); }
	// Return an Argon-Hash of the given value
	public static String getArgonHash(String value) {
		return ARGON2.hash(ARGON_ITERATIONS, ARGON_MEMORY, ARGON_PARALLELISM, value.toCharArray()); }
	// Retrieve the SALT from the HASH, calculate the password' HASH and compare the two
	public static boolean verifyPassword(String hash, String password) { return ARGON2.verify(hash, password.toCharArray()); }
	// Encoder and decoder to String
	public static String encodeToString(byte[] data) { return BASE64_ENCODER.encodeToString(data); }
	public static String decodeToString(String data) {
		return new String(BASE64_DECODER.decode(data.replaceAll("\\+", "-").replaceAll("/", "_"))); }
	public static String verifyPublicKey(String public_key) throws InvalidKeySpecException, NoSuchAlgorithmException {
		public_key = public_key.replace(RSA_BEGIN, "").replace(RSA_END, "").replaceAll("\\s", "")
							   .replaceAll("\\+", "-").replaceAll("/", "_");
		KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(new X509EncodedKeySpec(BASE64_DECODER.decode(public_key)));
		return public_key;
	}
	public static boolean verifyName(String name) {			  return CryptUtils.match(name, CryptUtils.NAME_REGEX); }
	public static boolean verifyUsername(String username) {	  return CryptUtils.match(username, CryptUtils.USER_REGEX); }
	public static boolean verifyEmail(String address) {		  return CryptUtils.match(address, CryptUtils.EMAIL_REGEX); }
	public static boolean verifyPassword(String password) {	  return CryptUtils.match(password, CryptUtils.PASS_REGEX); }
	public static boolean verifyValue(String value) {		  return CryptUtils.match(value, CryptUtils.BASE_REGEX); }
	public static boolean match(String value, String regex) { return value.matches(regex); }
	public static boolean similar(String[] strings) {
		for (int i = 0; i < strings.length - 1; i++) for (int j = i + 1; j < strings.length; j++)
			if (CryptUtils.similar(strings[i], strings[j], CryptUtils.SIMILAR_LENGTH)) return true;
		return false;
	}
	public static boolean similar(String[] strings, int length) {
		for (int i = 0; i < strings.length - 1; i++) for (int j = i + 1; j < strings.length; j++)
			if (CryptUtils.similar(strings[i], strings[j], length)) return true;
		return false;
	}
	public static boolean similar(String str1, String[] strings, int length) {
		for (String str : strings) if (CryptUtils.similar(str1, str, length)) return true;
		return false;
	}
	public static boolean similar(String str1, String[] strings) {
		for (String str : strings) if (CryptUtils.similar(str1, str, CryptUtils.SIMILAR_LENGTH)) return true;
		return false;
	}
	public static boolean similar(String str1, String str2) { return CryptUtils.similar(str1,str2,CryptUtils.SIMILAR_LENGTH); }
	public static boolean similar(String str1, String str2, int len) {
		String s1 = str1.toLowerCase(); String s2 = str2.toLowerCase();
		for (int i = 0; i < str2.length() - len; i++) if (s1.contains(s2.substring(i,i+len))) return true;
		return false;
	}

	/*--------------------------------------------------------------------------------------------------------------------------
	 * PUBLIC METHODS * Keep this as short as possible
	 *------------------------------------------------------------------------------------------------------------------------*/
	// Initial configuration: Put the properties for the DB and the connection
	private static String getStr(String key) { return (String) crypt_map.get(key); }
	private static int getInt(String key) { return (int) crypt_map.get(key); }
	public static void setCryptMap(Map<String,Object> crypto_map) {
		crypt_map = crypto_map;
		RANDOM_LENGTH = getInt("random_length");
		try { random_generator = SecureRandom.getInstance(getStr("random_algorithm"));
			  hash_generator = MessageDigest.getInstance(getStr("digest_algorithm"));
		} catch (NoSuchAlgorithmException ignore) {}
		if (crypt_map.containsKey("argon_iterations")) ARGON_ITERATIONS = getInt("argon_iterations");
		else ARGON_ITERATIONS = Argon2Helper.findIterations(ARGON2, getInt("argon_duration"),
															getInt("argon_memory"), getInt("argon_parallelism"));
		ARGON_MEMORY = getInt("argon_memory");
		ARGON_PARALLELISM = getInt("argon_parallelism");
		BASE_REGEX = getStr("base_regex");
		NAME_REGEX = getStr("name_regex");
		EMAIL_REGEX = getStr("email_regex");
		PASS_REGEX = getStr("pass_regex");
		USER_REGEX = getStr("user_regex");
		RSA_ALGORITHM = getStr("rsa_algorithm");
		RSA_BEGIN = getStr("rsa_begin");
		RSA_END = getStr("rsa_end");
		SIMILAR_LENGTH = getInt("similarity_length");
		PW_CHAR = getStr("password_characters").toCharArray();
		PW_LENGTH = getInt("password_length");
		OTP_CHAR = getStr("onetimep_characters").toCharArray();
		OTP_LENGTH = getInt("onetimep_length");
		ACCOUNT_EXPIRES = getInt("account_expires");
		SIGNIN_EXPIRES = getInt("signin_expires");
	}
}
