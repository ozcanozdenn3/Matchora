@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.matchora.app.data

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

actual object PendingSendPersistence {
    private fun path(owner: String): String {
        require(owner.matches(Regex("[A-Za-z0-9_-]{1,200}")))
        val root = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory,NSUserDomainMask,true).first() as String
        val folder = "$root/MatchoraPending"
        check(NSFileManager.defaultManager.createDirectoryAtPath(folder,true,mapOf(NSFileProtectionKey to NSFileProtectionCompleteUntilFirstUserAuthentication),null))
        NSURL.fileURLWithPath(folder).setResourceValue(true,NSURLIsExcludedFromBackupKey,null)
        return "$folder/$owner.json"
    }
    actual fun read(owner: String): String? {
        val target = path(owner)
        if (!NSFileManager.defaultManager.fileExistsAtPath(target)) return null
        val data = requireNotNull(NSData.dataWithContentsOfFile(target))
        require(data.length <= 40uL*1024uL*1024uL)
        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0),data.bytes,data.length) }
        return bytes.decodeToString()
    }
    actual fun write(owner: String, value: String?) {
        val target = path(owner)
        if (value == null) { NSFileManager.defaultManager.removeItemAtPath(target,null); return }
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned { NSData.create(bytes=it.addressOf(0),length=bytes.size.toULong()) }
        check(data.writeToFile(target,NSDataWritingAtomic or NSDataWritingFileProtectionCompleteUntilFirstUserAuthentication,null))
    }
}
