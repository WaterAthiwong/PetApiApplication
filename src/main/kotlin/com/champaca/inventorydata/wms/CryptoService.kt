package com.champaca.inventorydata.wms

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


@Service
class CryptoService {

    @Value("\${wms.encryption.key}")
    lateinit var encryptionKey: String


    companion object {
//        const val ENCRYPTION_KEY = "MwawFDVwGMGtJk90HXZizAYVW9pMeKwv"
        const val CIPHER_NAME = "AES/CBC/PKCS5Padding"
    }

    fun encrypt(data: String): String {
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val initVector = IvParameterSpec(iv)

        val decodedKey = Base64.getDecoder().decode(encryptionKey)

        // If the decoded key is longer than 32 bytes, we need to trim it to 32. Otherwise, WMS side will fail the decryption.
        // Reference: https://itecnote.com/tecnote/java-aes-256-cbc-encrypted-with-php-and-decrypt-in-java/
        val secretKeySpec = SecretKeySpec(Arrays.copyOfRange(decodedKey, 0, 32), "AES")

        val cipher = Cipher.getInstance(CIPHER_NAME).apply {
            init(Cipher.ENCRYPT_MODE, secretKeySpec, initVector)
        }

        val encryptedData = Base64.getEncoder().encode(cipher.doFinal(data.toByteArray()))
        return Base64.getEncoder().encodeToString(encryptedData + "::".toByteArray() + iv)
    }

    fun encode(data: String): String {
        return Base64.getEncoder().encodeToString(data.toByteArray())
    }

    fun decode(encrypted: String): String {
        return String(Base64.getDecoder().decode(encrypted))
    }
}