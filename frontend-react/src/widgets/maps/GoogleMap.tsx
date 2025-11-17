/**
 * Google Map Widget - Interactive Google Maps integration
 * Shows device locations with markers
 */
import { useEffect, useRef, useState } from 'react'
import { Box, Typography, Paper, Alert } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function GoogleMap({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const mapRef = useRef<HTMLDivElement>(null)
  const [error, setError] = useState<string | null>(null)

  const defaultLat = config.settings?.defaultCenterLat ?? 37.7749
  const defaultLng = config.settings?.defaultCenterLng ?? -122.4194
  const defaultZoom = config.settings?.defaultZoom ?? 12

  useEffect(() => {
    // Check if Google Maps API is loaded
    if (typeof google === 'undefined' || !google.maps) {
      setError('Google Maps API not loaded. Add API key in environment.')
      return
    }

    if (!mapRef.current) return

    try {
      // Initialize map
      const map = new google.maps.Map(mapRef.current, {
        center: { lat: defaultLat, lng: defaultLng },
        zoom: defaultZoom,
        mapTypeControl: true,
        streetViewControl: true,
        fullscreenControl: true,
      })

      // Add markers from datasources
      const markers: google.maps.Marker[] = []

      config.datasources?.forEach((ds: any, index: number) => {
        // In real implementation, extract lat/lng from data
        // For demo, use default position with slight offset
        const lat = defaultLat + (Math.random() - 0.5) * 0.02
        const lng = defaultLng + (Math.random() - 0.5) * 0.02

        const marker = new google.maps.Marker({
          position: { lat, lng },
          map,
          title: ds.name || `Location ${index + 1}`,
          animation: google.maps.Animation.DROP,
        })

        const infoWindow = new google.maps.InfoWindow({
          content: `<div style="padding: 8px;">
            <h3 style="margin: 0 0 8px 0; color: #0F3E5C;">${ds.name || 'Device'}</h3>
            <p style="margin: 0; color: #757575;">Lat: ${lat.toFixed(6)}</p>
            <p style="margin: 0; color: #757575;">Lng: ${lng.toFixed(6)}</p>
          </div>`,
        })

        marker.addListener('click', () => {
          infoWindow.open(map, marker)
        })

        markers.push(marker)
      })

      return () => {
        // Cleanup markers
        markers.forEach((marker) => marker.setMap(null))
      }
    } catch (err) {
      setError('Failed to initialize Google Maps')
      console.error('Google Maps error:', err)
    }
  }, [config.datasources, defaultLat, defaultLng, defaultZoom])

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {config.showTitle && (
        <Box sx={{ p: 2, pb: 0 }}>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'Google Map'}
          </Typography>
        </Box>
      )}
      <Box sx={{ flex: 1, position: 'relative', minHeight: 200 }}>
        {error ? (
          <Alert severity="warning" sx={{ m: 2 }}>
            {error}
            <br />
            <Typography variant="caption">
              To enable Google Maps, add VITE_GOOGLE_MAPS_API_KEY to .env file
            </Typography>
          </Alert>
        ) : (
          <div ref={mapRef} style={{ width: '100%', height: '100%' }} />
        )}
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'google_map',
  name: 'Google Map',
  description: 'Interactive Google Maps with device markers',
  type: 'latest',
  tags: ['map', 'location', 'gps', 'google'],
}

registerWidget(descriptor, GoogleMap)
export default GoogleMap
