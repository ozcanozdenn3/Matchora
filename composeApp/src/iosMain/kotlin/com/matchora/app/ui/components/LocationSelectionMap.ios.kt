package com.matchora.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.matchora.app.state.EventGeoPoint
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
private class LocationSelectionCoordinator(var onCenterChanged: (EventGeoPoint) -> Unit) : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
        mapView.centerCoordinate.useContents { onCenterChanged(EventGeoPoint(latitude, longitude)) }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LocationSelectionMap(modifier: Modifier, initialPoint: EventGeoPoint, onCenterChanged: (EventGeoPoint) -> Unit) {
    val callback by rememberUpdatedState(onCenterChanged)
    val coordinator = remember { LocationSelectionCoordinator { callback(it) } }
    UIKitView(modifier = modifier, factory = {
        MKMapView().apply {
            setRegion(MKCoordinateRegionMakeWithDistance(CLLocationCoordinate2DMake(initialPoint.latitude, initialPoint.longitude), 2200.0, 2200.0), false)
            delegate = coordinator
            showsUserLocation = false
            rotateEnabled = false
            pitchEnabled = false
        }
    }, interactive = true)
}
