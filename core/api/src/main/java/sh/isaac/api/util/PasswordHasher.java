/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributions from 2013-2017 where performed either by US government 
 * employees, or under US Veterans Health Administration contracts. 
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government 
 * employees are USGovWork (17USC §105). Not subject to copyright. 
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 * 
 * Contributions prior to 2013:
 *
 * Copyright (C) International Health Terminology Standards Development Organisation.
 * Licensed under the Apache License, Version 2.0.
 *
 */



package sh.isaac.api.util;

//~--- JDK imports ------------------------------------------------------------

import java.nio.ByteBuffer;

import java.security.SecureRandom;

import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

//~--- non-JDK imports --------------------------------------------------------

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- classes ----------------------------------------------------------------

/**
 * {@link PasswordHasher}
 *
 * A safe, modern way to 1-way hash user passwords.
 * Adapted and enhanced from http://stackoverflow.com/a/11038230/2163960
 *
 * Later, added the ability to encrypt and decrypt arbitrary data - using many of the same
 * techniques.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class PasswordHasher {
   /** The Constant log_. */
   private static final Logger log = LoggerFactory.getLogger(PasswordHasher.class);

   // The higher the number of iterations the more expensive computing the hash is for us

   /** The Constant iterations. */
   // and also for a brute force attack.
   private static final int iterations = 10 * 1024;

   /** The Constant saltLen. */
   private static final int saltLen = 32;

   /** The Constant desiredKeyLen. */
   private static final int desiredKeyLen = 256;

   /** The Constant keyFactoryAlgorithm. */
   private static final String keyFactoryAlgorithm = "PBKDF2WithHmacSHA1";

   /** The Constant cipherAlgorithm. */
   private static final String cipherAlgorithm = "PBEWithSHA1AndDESede";

   // private static final Random random = new Random();  //Note, it would be more secure to use SecureRandom... but the entropy issues on Linux are a nasty issue
   // and it results in SecureRandom.getInstance(...).generateSeed(...) blocking for long periods of time.  A regular random is certainly good enough

   /** The Constant secureRandom. */
   // for our encryption purposes.
   private static final SecureRandom secureRandom = new SecureRandom();

   //~--- methods -------------------------------------------------------------

   /**
    * Checks whether given plaintext password corresponds to a stored salted hash of the password.
    *
    * @param password the password
    * @param stored the stored
    * @return true, if successful
    * @throws Exception the exception
    */
   public static boolean check(String password, String stored)
            throws Exception {
      final String[] saltAndPass = stored.split("\\$\\$\\$");

      if (saltAndPass.length != 2) {
         return false;
      }

      if ((password == null) || (password.length() == 0)) {
         return false;
      }

      final String hashOfInput = hash(password, Base64.getUrlDecoder()
                                                      .decode(saltAndPass[0]));

      return hashOfInput.equals(saltAndPass[1]);
   }

   /**
    * Decrypt.
    *
    * @param password the password
    * @param encryptedData the encrypted data
    * @return the byte[]
    * @throws Exception the exception
    */
   public static byte[] decrypt(String password, String encryptedData)
            throws Exception {
      final long     startTime   = System.currentTimeMillis();
      final String[] saltAndPass = encryptedData.split("\\$\\$\\$");

      if (saltAndPass.length != 2) {
         throw new Exception("Invalid encrypted data, can't find salt");
      }

      final byte[] result = decrypt(password, Base64.getUrlDecoder()
                                                    .decode(saltAndPass[0]), saltAndPass[1]);

      log.debug("Decrypt Time {} ms", System.currentTimeMillis() - startTime);
      return result;
   }

   /**
    * Decrypt to string.
    *
    * @param password the password
    * @param encryptedData the encrypted data
    * @return the string
    * @throws Exception the exception
    */
   public static String decryptToString(String password, String encryptedData)
            throws Exception {
      return new String(decrypt(password, encryptedData), "UTF-8");
   }

   /**
    * Encrypt.
    *
    * @param password the password
    * @param data the data
    * @return the string
    * @throws Exception the exception
    */
   public static String encrypt(String password, byte[] data)
            throws Exception {
      final long   startTime = System.currentTimeMillis();
      final byte[] salt      = new byte[saltLen];

      secureRandom.nextBytes(salt);

      // store the salt with the password
      final String result = Base64.getUrlEncoder()
                                  .encodeToString(salt) + "$$$" + encrypt(password, salt, data);

      log.debug("Encrypt Time {} ms", System.currentTimeMillis() - startTime);
      return result;
   }

   /**
    * Encrypt.
    *
    * @param password the password
    * @param data the data
    * @return the string
    * @throws Exception the exception
    */
   public static String encrypt(String password, String data)
            throws Exception {
      return encrypt(password, data.getBytes("UTF-8"));
   }

   /**
    * Computes a salted PBKDF2 hash of given plaintext password with the provided salt
    * Empty passwords are not supported.
    *
    * @param password the password
    * @param salt the salt
    * @return a Base64 encoded hash
    * @throws Exception the exception
    */
   public static String hash(String password, byte[] salt)
            throws Exception {
      return hash(password, salt, iterations, desiredKeyLen);
   }

   /**
    * Computes a salted PBKDF2 hash of given plaintext password with the provided salt
    * Empty passwords are not supported.
    *
    * @param password the password
    * @param salt the salt
    * @param iterationCount the iteration count
    * @param keyLength the key length
    * @return a URL Safe Base64 encoded hash
    * @throws Exception the exception
    */
   public static String hash(String password, byte[] salt, int iterationCount, int keyLength)
            throws Exception {
      final long startTime = System.currentTimeMillis();

      if ((password == null) || (password.length() == 0)) {
         throw new IllegalArgumentException("Empty passwords are not supported.");
      }

      final SecretKeyFactory f      = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
      final SecretKey key = f.generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength));
      final String           result = Base64.getUrlEncoder()
                                            .encodeToString(key.getEncoded());

      log.debug("Password compute time: {} ms", System.currentTimeMillis() - startTime);
      return result;
   }

   /**
    * Decrypt.
    *
    * @param password the password
    * @param salt the salt
    * @param data the data
    * @return the byte[]
    * @throws Exception the exception
    */
   private static byte[] decrypt(String password, byte[] salt, String data)
            throws Exception {
      final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
      final SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray(),
                                                                     salt,
                                                                     iterations,
                                                                     desiredKeyLen));
      final Cipher pbeCipher = Cipher.getInstance(cipherAlgorithm);

      pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, iterations));

      byte[] decrypted;

      try {
         decrypted = pbeCipher.doFinal(Base64.getUrlDecoder()
               .decode(data));
      } catch (final Exception e) {
         throw new Exception("Invalid decryption password");
      }

      if (decrypted.length >= 40) {
         Locale.setDefault(Locale.US);  // ensure .equals below is using same Locale. (Fortify)

         // The last 40 bytes should be the SHA1 Sum
         final String checkSum = new String(Arrays.copyOfRange(decrypted, decrypted.length - 40, decrypted.length));
         final byte[] userData = Arrays.copyOf(decrypted, decrypted.length - 40);
         final String computed = ChecksumGenerator.calculateChecksum("SHA1", userData);

         if (!checkSum.equals(computed)) {
            throw new Exception("Invalid decryption password, or truncated data");
         } else {
            return userData;
         }
      } else {
         throw new Exception("Truncated data");
      }
   }

   /**
    * Encrypt.
    *
    * @param password the password
    * @param salt the salt
    * @param data the data
    * @return the string
    * @throws Exception the exception
    */
   private static String encrypt(String password, byte[] salt, byte[] data)
            throws Exception {
      final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
      final SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray(),
                                                                     salt,
                                                                     iterations,
                                                                     desiredKeyLen));
      final Cipher pbeCipher = Cipher.getInstance(cipherAlgorithm);

      pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, iterations));

      // attach a sha1 checksum to the end of the data, so we know if we decrypted it properly.
      final byte[]     dataCheckSum = ChecksumGenerator.calculateChecksum("SHA1", data)
                                                       .getBytes();
      final ByteBuffer temp         = ByteBuffer.allocate(data.length + dataCheckSum.length);

      temp.put(data);
      temp.put(dataCheckSum);
      return Base64.getUrlEncoder()
                   .encodeToString(pbeCipher.doFinal(temp.array()));
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Computes a salted PBKDF2 hash of given plaintext password suitable for storing in a database.
    * Empty passwords are not supported.
    *
    * @param password the password
    * @return the salted hash
    * @throws Exception the exception
    */
   public static String getSaltedHash(String password)
            throws Exception {
      final long   startTime = System.currentTimeMillis();
      final byte[] salt      = new byte[saltLen];

      secureRandom.nextBytes(salt);

      // store the salt with the password
      final String result = Base64.getUrlEncoder()
                                  .encodeToString(salt) + "$$$" + hash(password, salt);

      log.debug("Compute Salted Hash time {} ms", System.currentTimeMillis() - startTime);
      return result;
   }
}
