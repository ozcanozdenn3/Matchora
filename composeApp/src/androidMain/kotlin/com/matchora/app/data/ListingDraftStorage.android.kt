package com.matchora.app.data

import android.content.Context

actual object ListingDraftStorage {
    private fun preferences() = AndroidBackendRuntime.context.getSharedPreferences("matchora_listing_drafts", Context.MODE_PRIVATE)
    actual fun read(owner: String): String? = preferences().getString(owner, null)
    actual fun write(owner: String, value: String?) {
        val editor = preferences().edit()
        if (value == null) editor.remove(owner) else editor.putString(owner, value)
        check(editor.commit()) { "İlan taslağı kaydedilemedi." }
    }
}
