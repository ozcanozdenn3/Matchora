package com.matchora.app.data

import platform.Foundation.NSUserDefaults

actual object ListingDraftStorage {
    private fun key(owner: String) = "matchora.listing.draft.$owner"
    actual fun read(owner: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(key(owner))
    actual fun write(owner: String, value: String?) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) defaults.removeObjectForKey(key(owner)) else defaults.setObject(value, forKey = key(owner))
        check(defaults.synchronize()) { "İlan taslağı kaydedilemedi." }
    }
}
