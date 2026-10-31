package com.matchora.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
private class MapCoordinator(
    var pins: List<MapPinData>,
    var onPinSelected: (String) -> Unit
) : NSObject(), MKMapViewDelegateProtocol {
    
    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, didSelectAnnotation: MKAnnotationProtocol) {
        val title = didSelectAnnotation.title ?: return
        val pin = pins.firstOrNull { it.title == title || it.id == title }
        if (pin != null) {
            onPinSelected(pin.id)
        }
    }

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        if (viewForAnnotation is MKUserLocation) return null
        
        val identifier = "SportVenueMarker"
        val marker = (mapView.dequeueReusableAnnotationViewWithIdentifier(identifier) as? MKMarkerAnnotationView)
            ?: MKMarkerAnnotationView(annotation = viewForAnnotation, reuseIdentifier = identifier)
        
        marker.annotation = viewForAnnotation
        marker.canShowCallout = true
        
        // Find matching pin
        val pin = pins.firstOrNull { it.id == viewForAnnotation.title || it.title == viewForAnnotation.title }
        if (pin != null) {
            marker.glyphText = pin.sportEmoji
        }
        
        // Matchora Vibrant Brand Orange: #E05A12
        marker.markerTintColor = UIColor(red = 0.88, green = 0.35, blue = 0.07, alpha = 1.0)
        return marker
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoomMeters: Double,
    pins: List<MapPinData>,
    selectedPinId: String?,
    onPinSelected: (String) -> Unit,
    interactive: Boolean
) {
    val callback by rememberUpdatedState(onPinSelected)
    val coordinator = remember {
        MapCoordinator(pins) { callback(it) }
    }

    var appliedPins by remember { mutableStateOf(pins) }
    var appliedViewport by remember { mutableStateOf(Triple(latitude, longitude, zoomMeters)) }
    val annotations = remember(pins) {
        pins.map { pin ->
            MKPointAnnotation().apply {
                setCoordinate(CLLocationCoordinate2DMake(pin.latitude, pin.longitude))
                setTitle(pin.title)
                setSubtitle("${pin.sportEmoji} ${pin.subtitle}")
            }
        }
    }

    UIKitView(
        factory = {
            MKMapView().apply {
                val center = CLLocationCoordinate2DMake(latitude, longitude)
                val region = MKCoordinateRegionMakeWithDistance(center, zoomMeters, zoomMeters)
                setRegion(region, animated = false)
                showsUserLocation = true
                userInteractionEnabled = interactive
                scrollEnabled = interactive
                zoomEnabled = interactive
                pitchEnabled = interactive
                rotateEnabled = interactive
                if (interactive) {
                    layoutMargins = UIEdgeInsetsMake(100.0, 0.0, 190.0, 0.0)
                } else {
                    layoutMargins = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)
                }
                delegate = coordinator
                annotations.forEach { ann -> addAnnotation(ann) }
            }
        },
        update = { mapView ->
            coordinator.pins = pins
            mapView.delegate = coordinator
            if (appliedPins != pins) {
                mapView.removeAnnotations(mapView.annotations)
                annotations.forEach { mapView.addAnnotation(it) }
                appliedPins = pins
            }
            val viewport = Triple(latitude, longitude, zoomMeters)
            if (appliedViewport != viewport) {
                mapView.setRegion(MKCoordinateRegionMakeWithDistance(CLLocationCoordinate2DMake(latitude, longitude), zoomMeters, zoomMeters), animated = true)
                appliedViewport = viewport
            }
            if (selectedPinId != null) {
                val targetPin = pins.firstOrNull { it.id == selectedPinId }
                if (targetPin != null) {
                    annotations.firstOrNull { it.title == targetPin.title }?.let { selectedAnn ->
                        mapView.selectAnnotation(selectedAnn, animated = true)
                    }
                }
            }
        },
        interactive = interactive,
        modifier = modifier
    )
}
