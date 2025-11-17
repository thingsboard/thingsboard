/**
 * Compass Widget - Canvas-based directional compass
 * Shows heading/direction with cardinal points
 */
import { useEffect, useRef } from 'react'
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function Compass({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const latestValue = data?.datasources?.[0]?.data?.[data.datasources[0].data.length - 1]?.value ?? 0

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
    ctx.lineWidth = 6
    ctx.stroke()

    // Draw inner circle (background)
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius - 5, 0, 2 * Math.PI)
    ctx.fillStyle = '#F5F5F5'
    ctx.fill()

    // Draw cardinal points
    const cardinals = [
      { label: 'N', angle: -Math.PI / 2, color: '#C62828' },
      { label: 'E', angle: 0, color: '#0F3E5C' },
      { label: 'S', angle: Math.PI / 2, color: '#0F3E5C' },
      { label: 'W', angle: Math.PI, color: '#0F3E5C' },
    ]

    const intercardinals = [
      { label: 'NE', angle: -Math.PI / 4 },
      { label: 'SE', angle: Math.PI / 4 },
      { label: 'SW', angle: (3 * Math.PI) / 4 },
      { label: 'NW', angle: -(3 * Math.PI) / 4 },
    ]

    // Draw cardinal labels
    cardinals.forEach(({ label, angle, color }) => {
      const labelRadius = radius - 25
      const x = centerX + labelRadius * Math.cos(angle)
      const y = centerY + labelRadius * Math.sin(angle)

      ctx.fillStyle = color
      ctx.font = 'bold 20px Arial'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(label, x, y)
    })

    // Draw intercardinal labels
    intercardinals.forEach(({ label, angle }) => {
      const labelRadius = radius - 25
      const x = centerX + labelRadius * Math.cos(angle)
      const y = centerY + labelRadius * Math.sin(angle)

      ctx.fillStyle = '#757575'
      ctx.font = '14px Arial'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(label, x, y)
    })

    // Draw degree ticks
    for (let i = 0; i < 360; i += 10) {
      const angle = (i - 90) * (Math.PI / 180) // -90 to start from North
      const tickLength = i % 30 === 0 ? 15 : 8
      const tickWidth = i % 30 === 0 ? 2 : 1

      const tickStartRadius = radius - 5
      const tickEndRadius = radius - 5 - tickLength
      const tickStartX = centerX + tickStartRadius * Math.cos(angle)
      const tickStartY = centerY + tickStartRadius * Math.sin(angle)
      const tickEndX = centerX + tickEndRadius * Math.cos(angle)
      const tickEndY = centerY + tickEndRadius * Math.sin(angle)

      ctx.beginPath()
      ctx.moveTo(tickStartX, tickStartY)
      ctx.lineTo(tickEndX, tickEndY)
      ctx.strokeStyle = '#999'
      ctx.lineWidth = tickWidth
      ctx.stroke()
    }

    // Draw compass needle (pointing to heading)
    const heading = ((latestValue % 360) + 360) % 360 // Normalize to 0-360
    const headingAngle = (heading - 90) * (Math.PI / 180) // Convert to radians, offset by 90

    ctx.save()
    ctx.translate(centerX, centerY)
    ctx.rotate(headingAngle)

    // North pointer (red)
    ctx.beginPath()
    ctx.moveTo(0, -radius + 40)
    ctx.lineTo(-10, 0)
    ctx.lineTo(10, 0)
    ctx.closePath()
    ctx.fillStyle = '#C62828'
    ctx.fill()

    // South pointer (white with border)
    ctx.beginPath()
    ctx.moveTo(0, radius - 40)
    ctx.lineTo(-10, 0)
    ctx.lineTo(10, 0)
    ctx.closePath()
    ctx.fillStyle = '#FFF'
    ctx.fill()
    ctx.strokeStyle = '#0F3E5C'
    ctx.lineWidth = 2
    ctx.stroke()

    ctx.restore()

    // Draw center circle
    ctx.beginPath()
    ctx.arc(centerX, centerY, 12, 0, 2 * Math.PI)
    ctx.fillStyle = '#0F3E5C'
    ctx.fill()

    // Draw heading value
    ctx.fillStyle = '#0F3E5C'
    ctx.font = 'bold 18px Arial'
    ctx.textAlign = 'center'
    ctx.fillText(`${heading.toFixed(0)}°`, centerX, centerY + 60)

    // Draw cardinal direction
    const getCardinalDirection = (deg: number): string => {
      const directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']
      const index = Math.round(deg / 45) % 8
      return directions[index]
    }

    ctx.font = '14px Arial'
    ctx.fillStyle = '#757575'
    ctx.fillText(getCardinalDirection(heading), centerX, centerY + 78)
  }, [latestValue])

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 1, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C', textAlign: 'center' }}>
          {config.title || 'Compass'}
        </Typography>
      )}
      <Box sx={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <canvas ref={canvasRef} style={{ maxWidth: '100%', height: 'auto' }} />
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'compass',
  name: 'Compass',
  description: 'Canvas-based directional compass with cardinal points',
  type: 'latest',
  tags: ['compass', 'direction', 'heading', 'canvas'],
}

registerWidget(descriptor, Compass)
export default Compass
