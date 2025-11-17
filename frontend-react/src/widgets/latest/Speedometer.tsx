/**
 * Speedometer Widget - Canvas-based speedometer gauge
 * Displays value with animated needle on circular gauge
 */
import { useEffect, useRef } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function Speedometer({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0

  const min = config.settings?.min ?? 0
  const max = config.settings?.max ?? 100
  const units = config.settings?.units ?? ''
  const majorTicks = config.settings?.majorTicks ?? 10

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Set canvas size
    const size = 280
    canvas.width = size
    canvas.height = size

    const centerX = size / 2
    const centerY = size / 2
    const radius = size / 2 - 20

    // Clear canvas
    ctx.clearRect(0, 0, size, size)

    // Draw outer circle
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
    ctx.strokeStyle = '#0F3E5C'
    ctx.lineWidth = 8
    ctx.stroke()

    // Draw inner circle (background)
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius - 10, 0, 2 * Math.PI)
    ctx.fillStyle = '#F5F5F5'
    ctx.fill()

    // Draw ticks and labels
    const startAngle = Math.PI * 0.75 // 135 degrees
    const endAngle = Math.PI * 2.25 // 405 degrees
    const angleRange = endAngle - startAngle

    for (let i = 0; i <= majorTicks; i++) {
      const angle = startAngle + (angleRange * i) / majorTicks
      const tickValue = min + ((max - min) * i) / majorTicks

      // Major tick
      const tickStartRadius = radius - 20
      const tickEndRadius = radius - 35
      const tickStartX = centerX + tickStartRadius * Math.cos(angle)
      const tickStartY = centerY + tickStartRadius * Math.sin(angle)
      const tickEndX = centerX + tickEndRadius * Math.cos(angle)
      const tickEndY = centerY + tickEndRadius * Math.sin(angle)

      ctx.beginPath()
      ctx.moveTo(tickStartX, tickStartY)
      ctx.lineTo(tickEndX, tickEndY)
      ctx.strokeStyle = '#0F3E5C'
      ctx.lineWidth = 3
      ctx.stroke()

      // Label
      const labelRadius = radius - 50
      const labelX = centerX + labelRadius * Math.cos(angle)
      const labelY = centerY + labelRadius * Math.sin(angle)

      ctx.fillStyle = '#0F3E5C'
      ctx.font = 'bold 12px Arial'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(tickValue.toFixed(0), labelX, labelY)

      // Minor ticks
      if (i < majorTicks) {
        for (let j = 1; j < 5; j++) {
          const minorAngle = angle + (angleRange / majorTicks) * (j / 5)
          const minorTickStartX = centerX + (radius - 20) * Math.cos(minorAngle)
          const minorTickStartY = centerY + (radius - 20) * Math.sin(minorAngle)
          const minorTickEndX = centerX + (radius - 28) * Math.cos(minorAngle)
          const minorTickEndY = centerY + (radius - 28) * Math.sin(minorAngle)

          ctx.beginPath()
          ctx.moveTo(minorTickStartX, minorTickStartY)
          ctx.lineTo(minorTickEndX, minorTickEndY)
          ctx.strokeStyle = '#999'
          ctx.lineWidth = 1
          ctx.stroke()
        }
      }
    }

    // Draw colored arc for current value
    const normalizedValue = Math.max(min, Math.min(max, latestValue))
    const valueAngle = startAngle + (angleRange * (normalizedValue - min)) / (max - min)

    ctx.beginPath()
    ctx.arc(centerX, centerY, radius - 5, startAngle, valueAngle)
    ctx.strokeStyle = normalizedValue > max * 0.8 ? '#C62828' : normalizedValue > max * 0.6 ? '#FFB300' : '#2E7D6F'
    ctx.lineWidth = 10
    ctx.stroke()

    // Draw needle
    const needleLength = radius - 40
    const needleX = centerX + needleLength * Math.cos(valueAngle)
    const needleY = centerY + needleLength * Math.sin(valueAngle)

    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.lineTo(needleX, needleY)
    ctx.strokeStyle = '#C62828'
    ctx.lineWidth = 4
    ctx.lineCap = 'round'
    ctx.stroke()

    // Draw center dot
    ctx.beginPath()
    ctx.arc(centerX, centerY, 8, 0, 2 * Math.PI)
    ctx.fillStyle = '#0F3E5C'
    ctx.fill()

    // Draw value text
    ctx.fillStyle = '#0F3E5C'
    ctx.font = 'bold 24px Arial'
    ctx.textAlign = 'center'
    ctx.fillText(normalizedValue.toFixed(1), centerX, centerY + 50)

    ctx.font = '14px Arial'
    ctx.fillStyle = '#757575'
    ctx.fillText(units, centerX, centerY + 70)
  }, [latestValue, min, max, majorTicks, units])

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 1, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C', textAlign: 'center' }}>
          {config.title || 'Speedometer'}
        </Typography>
      )}
      <Box sx={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <canvas ref={canvasRef} style={{ maxWidth: '100%', height: 'auto' }} />
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'speedometer',
  name: 'Speedometer',
  description: 'Canvas-based speedometer gauge with animated needle',
  type: 'latest',
  tags: ['gauge', 'speedometer', 'canvas', 'indicator'],
}

registerWidget(descriptor, Speedometer)
export default Speedometer
