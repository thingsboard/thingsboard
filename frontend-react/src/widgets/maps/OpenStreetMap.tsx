/**
 * OpenStreet Map Widget - Leaflet-based map widget
 * Free alternative to Google Maps using OpenStreetMap
 */
import { useEffect, useRef } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function OpenStreetMap({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<any>(null)

  const defaultLat = config.settings?.defaultCenterLat ?? 37.7749
  const defaultLng = config.settings?.defaultCenterLng ?? -122.4194
  const defaultZoom = config.settings?.defaultZoom ?? 12

  useEffect(() => {
    if (!mapRef.current) return

    // Simple fallback implementation without Leaflet library
    // In production, you would use: import L from 'leaflet'

    // For now, render a styled placeholder with coordinates
    const container = mapRef.current
    container.style.background = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    container.style.position = 'relative'
    container.style.overflow = 'hidden'

    // Add grid pattern
    container.innerHTML = `
      <div style="
        position: absolute;
        inset: 0;
        background-image:
          linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
          linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px);
        background-size: 50px 50px;
      "></div>
      <div style="
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        text-align: center;
        color: white;
        z-index: 10;
      ">
        <div style="
          background: rgba(0,0,0,0.6);
          padding: 20px;
          border-radius: 8px;
          backdrop-filter: blur(10px);
        ">
          <h3 style="margin: 0 0 12px 0; font-size: 18px;">OpenStreetMap</h3>
          <div style="font-size: 14px; margin-bottom: 8px;">
            📍 Center: ${defaultLat.toFixed(4)}, ${defaultLng.toFixed(4)}
          </div>
          <div style="font-size: 14px; margin-bottom: 16px;">
            🔍 Zoom Level: ${defaultZoom}
          </div>
          ${config.datasources && config.datasources.length > 0 ? `
            <div style="font-size: 14px;">
              📌 ${config.datasources.length} marker${config.datasources.length > 1 ? 's' : ''}
            </div>
          ` : ''}
          <div style="
            margin-top: 16px;
            padding-top: 16px;
            border-top: 1px solid rgba(255,255,255,0.3);
            font-size: 12px;
            color: rgba(255,255,255,0.8);
          ">
            To enable full map functionality:<br/>
            npm install leaflet react-leaflet
          </div>
        </div>
      </div>
    `

    return () => {
      if (mapInstanceRef.current) {
        // Cleanup Leaflet map if initialized
        mapInstanceRef.current = null
      }
    }
  }, [config.datasources, defaultLat, defaultLng, defaultZoom])

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {config.showTitle && (
        <Box sx={{ p: 2, pb: 0 }}>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'OpenStreet Map'}
          </Typography>
        </Box>
      )}
      <Box sx={{ flex: 1, position: 'relative', minHeight: 200 }}>
        <div ref={mapRef} style={{ width: '100%', height: '100%' }} />
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'openstreet_map',
  name: 'OpenStreet Map',
  description: 'Free map widget using OpenStreetMap and Leaflet',
  type: 'latest',
  tags: ['map', 'location', 'gps', 'openstreetmap', 'leaflet'],
}

registerWidget(descriptor, OpenStreetMap)
export default OpenStreetMap
