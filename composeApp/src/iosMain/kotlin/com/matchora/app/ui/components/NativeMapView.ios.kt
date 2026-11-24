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
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
private class SportPinAnnotation(val pinId: String) : MKPointAnnotation()

@OptIn(ExperimentalForeignApi::class)
private class MapCoordinator(
    var pins: List<MapPinData>,
    var onPinSelected: (String) -> Unit
) : NSObject(), MKMapViewDelegateProtocol {
    
    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, didSelectAnnotation: MKAnnotationProtocol) {
        val annotation = didSelectAnnotation as? SportPinAnnotation ?: return
        if (pins.any { it.id == annotation.pinId }) onPinSelected(annotation.pinId)
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
        val pin = pins.firstOrNull { it.id == (viewForAnnotation as? SportPinAnnotation)?.pinId }
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
    val safeLatitude = if (validMapCoordinate(latitude, longitude)) latitude.coerceIn(-85.0, 85.0) else 39.0
    val safeLongitude = if (validMapCoordinate(latitude, longitude)) longitude else 35.0
    val safeSpan = zoomMeters.takeIf { it.isFinite() && it > 0 }?.coerceIn(300.0, 20000000.0) ?: 4200.0
    val callback by rememberUpdatedState(onPinSelected)
    val coordinator = remember {
        MapCoordinator(pins) { callback(it) }
    }

    var appliedPins by remember { mutableStateOf(pins) }
    var appliedViewport by remember { mutableStateOf(Triple(latitude, longitude, zoomMeters)) }
    val annotations = remember(pins) {
        pins.filter { validMapCoordinate(it.latitude, it.longitude) }.distinctBy { it.id }.map { pin ->
            SportPinAnnotation(pin.id).apply {
                setCoordinate(CLLocationCoordinate2DMake(pin.latitude, pin.longitude))
                setTitle(pin.title)
                setSubtitle("${pin.sportEmoji} ${pin.subtitle}")
            }
        }
    }

    UIKitView(
        factory = {
            MKMapView().apply {
                val center = CLLocationCoordinate2DMake(safeLatitude, safeLongitude)
                val region = MKCoordinateRegionMakeWithDistance(center, safeSpan, safeSpan)
                setRegion(region, animated = false)
                showsUserLocation = true
                overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
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
            mapView.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
            mapView.showsUserLocation = true
            coordinator.pins = pins
            mapView.delegate = coordinator
            mapView.userInteractionEnabled = interactive
            mapView.scrollEnabled = interactive
            mapView.zoomEnabled = interactive
            mapView.rotateEnabled = interactive
            mapView.pitchEnabled = interactive
            if (appliedPins != pins) {
                mapView.removeAnnotations(mapView.annotations)
                annotations.forEach { mapView.addAnnotation(it) }
                appliedPins = pins
            }
            val viewport = Triple(latitude, longitude, zoomMeters)
            if (appliedViewport != viewport) {
                mapView.setRegion(MKCoordinateRegionMakeWithDistance(CLLocationCoordinate2DMake(safeLatitude, safeLongitude), safeSpan, safeSpan), animated = true)
                appliedViewport = viewport
            }
            if (selectedPinId != null) {
                val targetPin = pins.firstOrNull { it.id == selectedPinId }
                if (targetPin != null) {
                    annotations.firstOrNull { it.pinId == targetPin.id }?.let { selectedAnn ->
                        mapView.selectAnnotation(selectedAnn, animated = true)
                    }
                }
            } else {
                mapView.selectedAnnotations.forEach { selectedAnn ->
                    (selectedAnn as? platform.MapKit.MKAnnotationProtocol)?.let { ann ->
                        mapView.deselectAnnotation(ann, animated = true)
                    }
                }
            }
        },
        interactive = interactive,
        modifier = modifier
    )
}
