@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.ui.components

import com.matchora.app.state.SportEvent
import platform.Foundation.*
import platform.UIKit.*
import platform.EventKit.*
import platform.EventKitUI.*
import platform.darwin.NSObject
import kotlinx.cinterop.useContents
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private fun eventPresenter(): UIViewController? {
    val window = UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
    return window?.rootViewController?.let { generateSequence(it) { item -> item.presentedViewController }.last() }
}
private var calendarDelegate: EKEventEditViewDelegateProtocol? = null

actual fun shareEventInvitation(event: SportEvent) {
    shareText("Maç davetini paylaş", eventInvitationText(event))
}

actual fun shareText(title: String, text: String) {
    val top = eventPresenter() ?: return
    val sheet = UIActivityViewController(listOf(text), null)
    sheet.popoverPresentationController?.sourceView = top.view
    top.presentViewController(sheet, true, null)
}
actual fun openEventDirections(event: SportEvent): Boolean {
    if (isOnlineMeeting(event)) return false
    val point = eventDirectionsPoint(event)
    return openLocationDirections(point?.latitude, point?.longitude, "${event.venue}, ${event.area}, ${event.city}")
}
actual fun openLocationDirections(latitude: Double?, longitude: Double?, query: String): Boolean {
    if (latitude != null && longitude != null && validMapCoordinate(latitude, longitude)) {
        val url = NSURL.URLWithString("https://maps.apple.com/?daddr=$latitude,$longitude&dirflg=d") ?: return false
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
        return true
    }
    val encoded = NSString.create(string = query).stringByAddingPercentEncodingWithAllowedCharacters(
        NSCharacterSet.characterSetWithCharactersInString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")) ?: return false
    val url = NSURL.URLWithString("https://maps.apple.com/?q=$encoded") ?: return false
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
    return true
}
actual fun openEventCalendar(event: SportEvent, includeInviteLink: Boolean): Boolean {
    val start = event.startsAtEpochMillis ?: return false
    val top = eventPresenter() ?: return false
    val store = EKEventStore()
    val nativeEvent = EKEvent.eventWithEventStore(store).apply {
        title = event.title
        location = if (isOnlineMeeting(event)) event.venue else "${event.venue}, ${event.area}, ${event.city}"
        startDate = NSDate.dateWithTimeIntervalSince1970(start / 1000.0)
        endDate = NSDate.dateWithTimeIntervalSince1970(start / 1000.0 + event.durationMin * 60)
        notes = calendarNotesForViewer(event, includeInviteLink)
        if (includeInviteLink) URL = NSURL.URLWithString("matchora://event/${event.id}")
    }
    val editor = EKEventEditViewController()
    editor.eventStore = store; editor.event = nativeEvent
    val delegate = object : NSObject(), EKEventEditViewDelegateProtocol {
        override fun eventEditViewController(controller: EKEventEditViewController, didCompleteWithAction: EKEventEditViewAction) {
            controller.dismissViewControllerAnimated(true, null); calendarDelegate = null
        }
    }
    calendarDelegate = delegate; editor.editViewDelegate = delegate
    if (NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion } >= 17) top.presentViewController(editor, true, null)
    else {
        store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                if (granted) top.presentViewController(editor, true, null)
                else {
                    calendarDelegate = null
                    val alert = UIAlertController.alertControllerWithTitle("Takvim izni kapalı", "Maçı cihaz takvimine kaydetmek için ayarlardan takvim erişimini açabilirsin.", UIAlertControllerStyleAlert)
                    alert.addAction(UIAlertAction.actionWithTitle("Ayarlar", UIAlertActionStyleDefault) { openApplicationSettings() })
                    alert.addAction(UIAlertAction.actionWithTitle("Kapat", UIAlertActionStyleCancel, null))
                    top.presentViewController(alert, true, null)
                }
            }
        }
    }
    return true
}
