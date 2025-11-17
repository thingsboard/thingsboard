/**
 * Image Viewer Widget - Display images from URL or base64
 * Supports zoom, fullscreen, and multiple images
 */
import { useState } from 'react'
import { Box, Typography, Paper, IconButton } from '@mui/material'
import {
  ZoomIn,
  ZoomOut,
  Fullscreen,
  NavigateBefore,
  NavigateNext,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function ImageViewer({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [currentIndex, setCurrentIndex] = useState(0)
  const [zoom, setZoom] = useState(1)

  // Demo images - in production, these would come from datasources
  const images = config.settings?.images || [
    'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80',
    'https://images.unsplash.com/photo-1446776653964-20c1d3a81b06?w=800&q=80',
    'https://images.unsplash.com/photo-1484417894907-623942c8ee29?w=800&q=80',
  ]

  const handleZoomIn = () => setZoom(Math.min(zoom + 0.2, 3))
  const handleZoomOut = () => setZoom(Math.max(zoom - 0.2, 0.5))
  const handlePrevious = () => setCurrentIndex((prev) => (prev > 0 ? prev - 1 : images.length - 1))
  const handleNext = () => setCurrentIndex((prev) => (prev < images.length - 1 ? prev + 1 : 0))
  const handleFullscreen = () => {
    const element = document.getElementById(`image-viewer-${widget.id}`)
    if (element) {
      if (document.fullscreenElement) {
        document.exitFullscreen()
      } else {
        element.requestFullscreen()
      }
    }
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {config.showTitle && (
        <Box sx={{ p: 2, pb: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'Image Viewer'}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {currentIndex + 1} / {images.length}
          </Typography>
        </Box>
      )}

      <Box
        id={`image-viewer-${widget.id}`}
        sx={{
          flex: 1,
          position: 'relative',
          overflow: 'hidden',
          bgcolor: '#000',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {/* Image */}
        <Box
          component="img"
          src={images[currentIndex]}
          alt={`Image ${currentIndex + 1}`}
          sx={{
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'contain',
            transform: `scale(${zoom})`,
            transition: 'transform 0.3s ease',
            cursor: zoom > 1 ? 'move' : 'default',
          }}
        />

        {/* Navigation overlays */}
        {images.length > 1 && (
          <>
            <IconButton
              onClick={handlePrevious}
              sx={{
                position: 'absolute',
                left: 8,
                top: '50%',
                transform: 'translateY(-50%)',
                bgcolor: 'rgba(0,0,0,0.5)',
                color: 'white',
                '&:hover': { bgcolor: 'rgba(0,0,0,0.7)' },
              }}
            >
              <NavigateBefore />
            </IconButton>

            <IconButton
              onClick={handleNext}
              sx={{
                position: 'absolute',
                right: 8,
                top: '50%',
                transform: 'translateY(-50%)',
                bgcolor: 'rgba(0,0,0,0.5)',
                color: 'white',
                '&:hover': { bgcolor: 'rgba(0,0,0,0.7)' },
              }}
            >
              <NavigateNext />
            </IconButton>
          </>
        )}

        {/* Controls overlay */}
        <Box
          sx={{
            position: 'absolute',
            bottom: 8,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: 1,
            bgcolor: 'rgba(0,0,0,0.6)',
            borderRadius: 2,
            p: 0.5,
          }}
        >
          <IconButton size="small" onClick={handleZoomOut} sx={{ color: 'white' }}>
            <ZoomOut fontSize="small" />
          </IconButton>
          <Typography variant="caption" sx={{ color: 'white', alignSelf: 'center', minWidth: 40, textAlign: 'center' }}>
            {(zoom * 100).toFixed(0)}%
          </Typography>
          <IconButton size="small" onClick={handleZoomIn} sx={{ color: 'white' }}>
            <ZoomIn fontSize="small" />
          </IconButton>
          <Box sx={{ width: 1, bgcolor: 'rgba(255,255,255,0.3)', mx: 0.5 }} />
          <IconButton size="small" onClick={handleFullscreen} sx={{ color: 'white' }}>
            <Fullscreen fontSize="small" />
          </IconButton>
        </Box>
      </Box>

      {/* Thumbnail strip */}
      {images.length > 1 && (
        <Box sx={{ p: 1, display: 'flex', gap: 1, overflowX: 'auto', bgcolor: '#F5F5F5' }}>
          {images.map((img, index) => (
            <Box
              key={index}
              component="img"
              src={img}
              alt={`Thumbnail ${index + 1}`}
              onClick={() => setCurrentIndex(index)}
              sx={{
                width: 60,
                height: 40,
                objectFit: 'cover',
                borderRadius: 1,
                cursor: 'pointer',
                border: currentIndex === index ? '2px solid #0F3E5C' : '2px solid transparent',
                opacity: currentIndex === index ? 1 : 0.6,
                transition: 'all 0.2s ease',
                '&:hover': { opacity: 1 },
              }}
            />
          ))}
        </Box>
      )}
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'image_viewer',
  name: 'Image Viewer',
  description: 'Display images with zoom, navigation, and fullscreen',
  type: 'static',
  tags: ['image', 'viewer', 'gallery', 'media'],
}

registerWidget(descriptor, ImageViewer)
export default ImageViewer
