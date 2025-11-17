/**
 * Radial Gauge Widget - Canvas-based radial gauge
 * Generic configurable gauge with color zones
 */
import { useEffect, useRef } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function RadialGauge({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0

  const min = config.settings?.min ?? 0
  const max = config.settings?.max ?? 100
  const units = config.settings?.units ?? '%'
  const title = config.settings?.gaugeTitle ?? 'Value'

  // Color zones
  const zones = config.settings?.zones ?? [
    { from: 0, to: 30, color: '#2E7D6F' },
    { from: 30, to: 70, color: '#FFB300' },
    { from: 70, to: 100, color: '#C62828' },
  ]

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Set canvas size
    const size = 260
    canvas.width = size
    canvas.height = size * 0.75 // Semicircle

    const centerX = size / 2
    const centerY = size * 0.7
    const radius = size / 2 - 30

    // Clear canvas
    ctx.clearRect(0, 0, size, size * 0.75)

    // Draw gauge background arc
    const startAngle = Math.PI * 0.75
    const endAngle = Math.PI * 2.25

    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, startAngle, endAngle)
    ctx.strokeStyle = '#E0E0E0'
    ctx.lineWidth = 20
    ctx.stroke()

    // Draw color zones
    zones.forEach((zone: any) => {
      const zoneStartAngle = startAngle + ((zone.from - min) / (max - min)) * (endAngle - startAngle)
      const zoneEndAngle = startAngle + ((zone.to - min) / (max - min)) * (endAngle - startAngle)

      ctx.beginPath()
      ctx.arc(centerX, centerY, radius, zoneStartAngle, zoneEndAngle)
      ctx.strokeStyle = zone.color
      ctx.lineWidth = 20
      ctx.stroke()
    })

    // Draw ticks
    const majorTickCount = 11
    for (let i = 0; i <= majorTickCount; i++) {
      const angle = startAngle + ((endAngle - startAngle) * i) / majorTickCount
      const tickValue = min + ((max - min) * i) / majorTickCount

      // Major tick
      const tickStartRadius = radius - 15
      const tickEndRadius = radius - 25
      const tickStartX = centerX + tickStartRadius * Math.cos(angle)
      const tickStartY = centerY + tickStartRadius * Math.sin(angle)
      const tickEndX = centerX + tickEndRadius * Math.cos(angle)
      const tickEndY = centerY + tickEndRadius * Math.sin(angle)

      ctx.beginPath()
      ctx.moveTo(tickStartX, tickStartY)
      ctx.lineTo(tickEndX, tickEndY)
      ctx.strokeStyle = '#FFF'
      ctx.lineWidth = 3
      ctx.stroke()

      // Label
      const labelRadius = radius - 40
      const labelX = centerX + labelRadius * Math.cos(angle)
      const labelY = centerY + labelRadius * Math.sin(angle)

      ctx.fillStyle = '#0F3E5C'
      ctx.font = 'bold 11px Arial'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(tickValue.toFixed(0), labelX, labelY)
    }

    // Draw needle
    const normalizedValue = Math.max(min, Math.min(max, latestValue))
    const needleAngle = startAngle + ((normalizedValue - min) / (max - min)) * (endAngle - startAngle)
    const needleLength = radius - 20

    ctx.save()
    ctx.translate(centerX, centerY)
    ctx.rotate(needleAngle)

    // Needle shaft
    ctx.beginPath()
    ctx.moveTo(-8, 0)
    ctx.lineTo(-3, -needleLength)
    ctx.lineTo(3, -needleLength)
    ctx.lineTo(8, 0)
    ctx.closePath()
    ctx.fillStyle = '#0F3E5C'
    ctx.fill()

    ctx.restore()

    // Draw center hub
    ctx.beginPath()
    ctx.arc(centerX, centerY, 12, 0, 2 * Math.PI)
    ctx.fillStyle = '#0F3E5C'
    ctx.fill()

    ctx.beginPath()
    ctx.arc(centerX, centerY, 8, 0, 2 * Math.PI)
    ctx.fillStyle = '#FFF'
    ctx.fill()

    // Draw value text
    ctx.fillStyle = '#0F3E5C'
    ctx.font = 'bold 28px Arial'
    ctx.textAlign = 'center'
    ctx.fillText(normalizedValue.toFixed(1), centerX, centerY + 45)

    ctx.font = '14px Arial'
    ctx.fillStyle = '#757575'
    ctx.fillText(units, centerX, centerY + 65)
  }, [latestValue, min, max, units, zones])

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 1, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C', textAlign: 'center' }}>
          {config.title || 'Radial Gauge'}
        </Typography>
      )}
      <Box sx={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <canvas ref={canvasRef} style={{ maxWidth: '100%', height: 'auto' }} />
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'radial_gauge',
  name: 'Radial Gauge',
  description: 'Canvas-based radial gauge with configurable color zones',
  type: 'latest',
  tags: ['gauge', 'radial', 'canvas', 'meter'],
}

registerWidget(descriptor, RadialGauge)
export default RadialGauge
