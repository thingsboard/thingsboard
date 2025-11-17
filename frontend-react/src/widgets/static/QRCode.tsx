/**
 * QR Code Widget - Generate and display QR codes
 * Useful for device provisioning and information sharing
 */
import { useEffect, useRef } from 'react'
import { Box, Typography, Paper, Button } from '@mui/material'
import { Download as DownloadIcon } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function QRCode({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const canvasRef = useRef<HTMLCanvasElement>(null)

  // QR code data from config or datasource
  const qrData = config.settings?.data || data?.datasources?.[0]?.data?.[0]?.value || 'https://thingsboard.io'
  const size = config.settings?.size || 200
  const foreground = config.settings?.foreground || '#0F3E5C'
  const background = config.settings?.background || '#FFFFFF'

  useEffect(() => {
    // Simple QR code generation (in production, use qrcode library)
    // For now, create a placeholder with pattern
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    canvas.width = size
    canvas.height = size

    // Background
    ctx.fillStyle = background
    ctx.fillRect(0, 0, size, size)

    // Create a simple QR-like pattern
    const moduleSize = size / 25
    const modules = 25

    // Random pattern based on data
    const seed = qrData.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)
    const random = (x: number, y: number) => {
      const val = Math.sin(seed + x * 12.9898 + y * 78.233) * 43758.5453
      return val - Math.floor(val)
    }

    // Draw pattern
    for (let y = 0; y < modules; y++) {
      for (let x = 0; x < modules; x++) {
        // Position markers (corners)
        const isPositionMarker =
          (x < 7 && y < 7) ||
          (x >= modules - 7 && y < 7) ||
          (x < 7 && y >= modules - 7)

        if (isPositionMarker) {
          // Draw position marker pattern
          const isBorder = x === 0 || x === 6 || y === 0 || y === 6 ||
                          (x === modules - 1 || x === modules - 7) ||
                          (y === modules - 1 || y === modules - 7)
          const isCenter = (x === 3 || x === modules - 4) && (y === 3 || y === modules - 4)

          if (isBorder || isCenter) {
            ctx.fillStyle = foreground
            ctx.fillRect(x * moduleSize, y * moduleSize, moduleSize, moduleSize)
          }
        } else if (random(x, y) > 0.5) {
          ctx.fillStyle = foreground
          ctx.fillRect(x * moduleSize, y * moduleSize, moduleSize, moduleSize)
        }
      }
    }

    // Add border
    ctx.strokeStyle = foreground
    ctx.lineWidth = 2
    ctx.strokeRect(1, 1, size - 2, size - 2)
  }, [qrData, size, foreground, background])

  const handleDownload = () => {
    const canvas = canvasRef.current
    if (!canvas) return

    const link = document.createElement('a')
    link.download = 'qrcode.png'
    link.href = canvas.toDataURL('image/png')
    link.click()
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'QR Code'}
        </Typography>
      )}

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
        {/* QR Code Canvas */}
        <Box
          sx={{
            p: 2,
            bgcolor: background,
            borderRadius: 2,
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          }}
        >
          <canvas ref={canvasRef} style={{ display: 'block' }} />
        </Box>

        {/* QR Code Data */}
        <Box sx={{ textAlign: 'center', maxWidth: '100%' }}>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{
              display: 'block',
              fontSize: '11px',
              wordBreak: 'break-all',
              px: 2,
            }}
          >
            {qrData}
          </Typography>
        </Box>

        {/* Download Button */}
        <Button
          variant="outlined"
          size="small"
          startIcon={<DownloadIcon />}
          onClick={handleDownload}
          sx={{
            borderColor: '#0F3E5C',
            color: '#0F3E5C',
            '&:hover': {
              borderColor: '#2E7D6F',
              bgcolor: 'rgba(46, 125, 111, 0.1)',
            },
          }}
        >
          Download QR Code
        </Button>

        {/* Info note */}
        <Box sx={{ mt: 1, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1, width: '100%' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
            💡 For production use, install 'qrcode' library for full QR code support
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'qr_code',
  name: 'QR Code',
  description: 'Generate and display QR codes for data sharing',
  type: 'static',
  tags: ['qr', 'code', 'barcode', 'scan', 'share'],
}

registerWidget(descriptor, QRCode)
export default QRCode
