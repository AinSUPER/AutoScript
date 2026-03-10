package com.autoscript.advanced.security

import android.util.Base64
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.*

/**
 * 脚本加密器
 * 提供脚本加密、解密、签名验证等功能
 */
class ScriptEncryptor {

    /**
     * 加密算法
     */
    enum class Algorithm {
        AES, AES_CBC, AES_GCM, RSA, BLOWFISH
    }

    /**
     * 加密配置
     */
    data class EncryptConfig(
        val algorithm: Algorithm = Algorithm.AES_GCM,
        val keySize: Int = 256,
        val ivSize: Int = 16,
        val saltSize: Int = 16,
        val iterationCount: Int = 65536
    )

    /**
     * 加密结果
     */
    data class EncryptResult(
        val success: Boolean,
        val data: String? = null,
        val iv: String? = null,
        val salt: String? = null,
        val error: String? = null
    )

    /**
     * 解密结果
     */
    data class DecryptResult(
        val success: Boolean,
        val data: String? = null,
        val error: String? = null
    )

    /**
     * 签名结果
     */
    data class SignResult(
        val success: Boolean,
        val signature: String? = null,
        val publicKey: String? = null,
        val error: String? = null
    )

    private var config = EncryptConfig()

    /**
     * 设置加密配置
     */
    fun setConfig(config: EncryptConfig) {
        this.config = config
    }

    /**
     * 生成随机密钥
     * @return Base64编码的密钥
     */
    fun generateKey(): String {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(config.keySize)
        val secretKey = keyGenerator.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
    }

    /**
     * 生成随机IV
     * @return Base64编码的IV
     */
    fun generateIV(): String {
        val iv = ByteArray(config.ivSize)
        SecureRandom().nextBytes(iv)
        return Base64.encodeToString(iv, Base64.DEFAULT)
    }

    /**
     * 从密码生成密钥
     * @param password 密码
     * @param salt 盐值
     * @return 密钥
     */
    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, config.iterationCount, config.keySize)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /**
     * 加密数据
     * @param plainText 明文
     * @param key 密钥 (Base64编码)
     * @return 加密结果
     */
    fun encrypt(plainText: String, key: String): EncryptResult {
        return try {
            val keyBytes = Base64.decode(key, Base64.DEFAULT)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            when (config.algorithm) {
                Algorithm.AES -> encryptAES(plainText, secretKey)
                Algorithm.AES_CBC -> encryptAESCBC(plainText, secretKey)
                Algorithm.AES_GCM -> encryptAESGCM(plainText, secretKey)
                else -> EncryptResult(false, error = "不支持的算法")
            }
        } catch (e: Exception) {
            EncryptResult(false, error = e.message)
        }
    }

    /**
     * AES加密 (ECB模式)
     */
    private fun encryptAES(plainText: String, key: SecretKey): EncryptResult {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptResult(
                success = true,
                data = Base64.encodeToString(encrypted, Base64.DEFAULT)
            )
        } catch (e: Exception) {
            EncryptResult(false, error = e.message)
        }
    }

    /**
     * AES-CBC加密
     */
    private fun encryptAESCBC(plainText: String, key: SecretKey): EncryptResult {
        return try {
            val iv = ByteArray(config.ivSize)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptResult(
                success = true,
                data = Base64.encodeToString(encrypted, Base64.DEFAULT),
                iv = Base64.encodeToString(iv, Base64.DEFAULT)
            )
        } catch (e: Exception) {
            EncryptResult(false, error = e.message)
        }
    }

    /**
     * AES-GCM加密
     */
    private fun encryptAESGCM(plainText: String, key: SecretKey): EncryptResult {
        return try {
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptResult(
                success = true,
                data = Base64.encodeToString(encrypted, Base64.DEFAULT),
                iv = Base64.encodeToString(iv, Base64.DEFAULT)
            )
        } catch (e: Exception) {
            EncryptResult(false, error = e.message)
        }
    }

    /**
     * 解密数据
     * @param encryptedData 加密数据 (Base64编码)
     * @param key 密钥 (Base64编码)
     * @param iv IV (Base64编码, CBC/GCM模式需要)
     * @return 解密结果
     */
    fun decrypt(encryptedData: String, key: String, iv: String? = null): DecryptResult {
        return try {
            val keyBytes = Base64.decode(key, Base64.DEFAULT)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            when (config.algorithm) {
                Algorithm.AES -> decryptAES(encryptedData, secretKey)
                Algorithm.AES_CBC -> decryptAESCBC(encryptedData, secretKey, iv!!)
                Algorithm.AES_GCM -> decryptAESGCM(encryptedData, secretKey, iv!!)
                else -> DecryptResult(false, error = "不支持的算法")
            }
        } catch (e: Exception) {
            DecryptResult(false, error = e.message)
        }
    }

    /**
     * AES解密 (ECB模式)
     */
    private fun decryptAES(encryptedData: String, key: SecretKey): DecryptResult {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)

            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
            DecryptResult(
                success = true,
                data = String(decrypted, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            DecryptResult(false, error = e.message)
        }
    }

    /**
     * AES-CBC解密
     */
    private fun decryptAESCBC(encryptedData: String, key: SecretKey, iv: String): DecryptResult {
        return try {
            val ivBytes = Base64.decode(iv, Base64.DEFAULT)
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
            DecryptResult(
                success = true,
                data = String(decrypted, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            DecryptResult(false, error = e.message)
        }
    }

    /**
     * AES-GCM解密
     */
    private fun decryptAESGCM(encryptedData: String, key: SecretKey, iv: String): DecryptResult {
        return try {
            val ivBytes = Base64.decode(iv, Base64.DEFAULT)
            val gcmSpec = GCMParameterSpec(128, ivBytes)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
            DecryptResult(
                success = true,
                data = String(decrypted, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            DecryptResult(false, error = e.message)
        }
    }

    /**
     * 使用密码加密
     * @param plainText 明文
     * @param password 密码
     * @return 加密结果
     */
    fun encryptWithPassword(plainText: String, password: String): EncryptResult {
        return try {
            val salt = ByteArray(config.saltSize)
            SecureRandom().nextBytes(salt)

            val key = deriveKeyFromPassword(password, salt)

            val iv = ByteArray(config.ivSize)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            EncryptResult(
                success = true,
                data = Base64.encodeToString(encrypted, Base64.DEFAULT),
                iv = Base64.encodeToString(iv, Base64.DEFAULT),
                salt = Base64.encodeToString(salt, Base64.DEFAULT)
            )
        } catch (e: Exception) {
            EncryptResult(false, error = e.message)
        }
    }

    /**
     * 使用密码解密
     * @param encryptedData 加密数据
     * @param password 密码
     * @param iv IV
     * @param salt 盐值
     * @return 解密结果
     */
    fun decryptWithPassword(encryptedData: String, password: String, iv: String, salt: String): DecryptResult {
        return try {
            val saltBytes = Base64.decode(salt, Base64.DEFAULT)
            val ivBytes = Base64.decode(iv, Base64.DEFAULT)

            val key = deriveKeyFromPassword(password, saltBytes)
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))

            DecryptResult(
                success = true,
                data = String(decrypted, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            DecryptResult(false, error = e.message)
        }
    }

    /**
     * 生成RSA密钥对
     * @return 密钥对 (公钥, 私钥)
     */
    fun generateRSAKeyPair(): Pair<String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val publicKey = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)
        val privateKey = Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT)

        return Pair(publicKey, privateKey)
    }

    /**
     * RSA加密
     * @param plainText 明文
     * @param publicKey 公钥
     * @return 加密结果
     */
    fun encryptRSA(plainText: String, publicKey: String): EncryptResult {
        return try {
            val keyBytes = Base64.decode(publicKey, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(keySpec)

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, pubKey)

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptResult(
                success = true,
                data = Base64.encodeToString(encrypted, Base64.DEFAULT)
            )
        } catch (e: Exception) {
            EncryptResult(false, error = e.message)
        }
    }

    /**
     * RSA解密
     * @param encryptedData 加密数据
     * @param privateKey 私钥
     * @return 解密结果
     */
    fun decryptRSA(encryptedData: String, privateKey: String): DecryptResult {
        return try {
            val keyBytes = Base64.decode(privateKey, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privKey = keyFactory.generatePrivate(keySpec)

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privKey)

            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
            DecryptResult(
                success = true,
                data = String(decrypted, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            DecryptResult(false, error = e.message)
        }
    }

    /**
     * 签名数据
     * @param data 数据
     * @param privateKey 私钥
     * @return 签名结果
     */
    fun sign(data: String, privateKey: String): SignResult {
        return try {
            val keyBytes = Base64.decode(privateKey, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privKey = keyFactory.generatePrivate(keySpec)

            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privKey)
            signature.update(data.toByteArray(Charsets.UTF_8))

            val signBytes = signature.sign()
            SignResult(
                success = true,
                signature = Base64.encodeToString(signBytes, Base64.DEFAULT)
            )
        } catch (e: Exception) {
            SignResult(false, error = e.message)
        }
    }

    /**
     * 验证签名
     * @param data 数据
     * @param signature 签名
     * @param publicKey 公钥
     * @return 是否验证通过
     */
    fun verify(data: String, signature: String, publicKey: String): Boolean {
        return try {
            val keyBytes = Base64.decode(publicKey, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val pubKey = keyFactory.generatePublic(keySpec)

            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(pubKey)
            sig.update(data.toByteArray(Charsets.UTF_8))

            val signBytes = Base64.decode(signature, Base64.DEFAULT)
            sig.verify(signBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 计算哈希值
     * @param data 数据
     * @param algorithm 哈希算法
     * @return 哈希值
     */
    fun hash(data: String, algorithm: String = "SHA-256"): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 计算MD5
     */
    fun md5(data: String): String = hash(data, "MD5")

    /**
     * 计算SHA-256
     */
    fun sha256(data: String): String = hash(data, "SHA-256")

    /**
     * 计算SHA-512
     */
    fun sha512(data: String): String = hash(data, "SHA-512")
}
