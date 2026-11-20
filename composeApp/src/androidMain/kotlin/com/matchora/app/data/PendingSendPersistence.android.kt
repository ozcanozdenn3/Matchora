package com.matchora.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object PendingSendPersistence {
    private const val alias = "matchora_pending_sends"
    private fun file(owner: String): AtomicFile {
        require(owner.matches(Regex("[A-Za-z0-9_-]{1,200}")))
        return AtomicFile(File(AndroidBackendRuntime.context.noBackupFilesDir,"pending-$owner.bin"))
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias,null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    actual fun read(owner: String): String? {
        val target = file(owner)
        if (!target.baseFile.exists()) return null
        val bytes = target.readFully()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12))) }
        return cipher.doFinal(bytes.copyOfRange(12,bytes.size)).decodeToString()
    }
    actual fun write(owner: String, value: String?) {
        val target = file(owner)
        if (value == null) { target.delete(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val bytes = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        val output = target.startWrite()
        try { output.write(bytes); target.finishWrite(output) }
        catch (error: Exception) { target.failWrite(output); throw error }
    }
}
