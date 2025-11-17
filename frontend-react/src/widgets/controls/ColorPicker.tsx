/**
 * Color Picker Widget - RGB/HSV color control for LED devices
 * Visual color selection with sliders and presets
 */
import { useState } from 'react'
import { Box, Typography, Paper, Slider, Button, Grid } from '@mui/material'
import { Palette, Brightness7 } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function ColorPicker({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  const [red, setRed] = useState(255)
  const [green, setGreen] = useState(0)
  const [blue, setBlue] = useState(0)
  const [brightness, setBrightness] = useState(100)

  const rgbToHex = (r: number, g: number, b: number): string => {
    return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
  }

  const currentColor = rgbToHex(
    Math.round((red * brightness) / 100),
    Math.round((green * brightness) / 100),
    Math.round((blue * brightness) / 100)
  )

  const presetColors = [
    { name: 'Red', r: 255, g: 0, b: 0 },
    { name: 'Orange', r: 255, g: 165, b: 0 },
    { name: 'Yellow', r: 255, g: 255, b: 0 },
    { name: 'Green', r: 0, g: 255, b: 0 },
    { name: 'Cyan', r: 0, g: 255, b: 255 },
    { name: 'Blue', r: 0, g: 0, b: 255 },
    { name: 'Purple', r: 128, g: 0, b: 128 },
    { name: 'Magenta', r: 255, g: 0, b: 255 },
    { name: 'White', r: 255, g: 255, b: 255 },
  ]

  const handlePresetClick = (preset: { r: number; g: number; b: number }) => {
    setRed(preset.r)
    setGreen(preset.g)
    setBlue(preset.b)
  }

  const handleApply = () => {
    console.log('Color applied:', { red, green, blue, brightness, hex: currentColor })
    // TODO: Send RPC command to RGB device
  }

  const handleOff = () => {
    setBrightness(0)
    console.log('LED turned off')
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Color Picker'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {/* Color Preview */}
        <Box
          sx={{
            height: 120,
            borderRadius: 2,
            bgcolor: currentColor,
            mb: 3,
            boxShadow: `0 8px 24px ${currentColor}80`,
            transition: 'all 0.3s ease',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Typography
            variant="h6"
            sx={{
              color: brightness > 50 ? '#000' : '#FFF',
              fontWeight: 'bold',
              textShadow: brightness > 50 ? '0 0 2px white' : '0 0 2px black',
            }}
          >
            {currentColor.toUpperCase()}
          </Typography>
        </Box>

        {/* RGB Sliders */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: 'bold', color: '#0F3E5C' }}>
            RGB Values
          </Typography>

          {/* Red */}
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#C62828', fontWeight: 'bold' }}>
                Red
              </Typography>
              <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold' }}>
                {red}
              </Typography>
            </Box>
            <Slider
              value={red}
              onChange={(_, v) => setRed(v as number)}
              min={0}
              max={255}
              sx={{
                '& .MuiSlider-track': { bgcolor: '#C62828' },
                '& .MuiSlider-thumb': { bgcolor: '#C62828' },
              }}
            />
          </Box>

          {/* Green */}
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#2E7D6F', fontWeight: 'bold' }}>
                Green
              </Typography>
              <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold' }}>
                {green}
              </Typography>
            </Box>
            <Slider
              value={green}
              onChange={(_, v) => setGreen(v as number)}
              min={0}
              max={255}
              sx={{
                '& .MuiSlider-track': { bgcolor: '#2E7D6F' },
                '& .MuiSlider-thumb': { bgcolor: '#2E7D6F' },
              }}
            />
          </Box>

          {/* Blue */}
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" sx={{ fontSize: '11px', color: '#1E88E5', fontWeight: 'bold' }}>
                Blue
              </Typography>
              <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold' }}>
                {blue}
              </Typography>
            </Box>
            <Slider
              value={blue}
              onChange={(_, v) => setBlue(v as number)}
              min={0}
              max={255}
              sx={{
                '& .MuiSlider-track': { bgcolor: '#1E88E5' },
                '& .MuiSlider-thumb': { bgcolor: '#1E88E5' },
              }}
            />
          </Box>
        </Box>

        {/* Brightness */}
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Brightness7 sx={{ fontSize: 18, color: '#FFB300' }} />
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
              Brightness
            </Typography>
            <Typography variant="caption" sx={{ ml: 'auto', fontWeight: 'bold' }}>
              {brightness}%
            </Typography>
          </Box>
          <Slider
            value={brightness}
            onChange={(_, v) => setBrightness(v as number)}
            min={0}
            max={100}
            sx={{
              '& .MuiSlider-track': { bgcolor: '#FFB300' },
              '& .MuiSlider-thumb': { bgcolor: '#FFB300' },
            }}
          />
        </Box>

        {/* Preset Colors */}
        <Box sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Palette sx={{ fontSize: 18, color: '#0F3E5C' }} />
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
              Presets
            </Typography>
          </Box>
          <Grid container spacing={1}>
            {presetColors.map((preset) => (
              <Grid item xs={4} key={preset.name}>
                <Box
                  onClick={() => handlePresetClick(preset)}
                  sx={{
                    height: 40,
                    borderRadius: 1,
                    bgcolor: rgbToHex(preset.r, preset.g, preset.b),
                    cursor: 'pointer',
                    border: '2px solid transparent',
                    transition: 'all 0.2s ease',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    '&:hover': {
                      borderColor: '#0F3E5C',
                      transform: 'scale(1.05)',
                    },
                  }}
                >
                  <Typography
                    variant="caption"
                    sx={{
                      fontSize: '9px',
                      fontWeight: 'bold',
                      color: preset.name === 'White' || preset.name === 'Yellow' ? '#000' : '#FFF',
                      textShadow: preset.name === 'White' || preset.name === 'Yellow' ? 'none' : '0 0 2px black',
                    }}
                  >
                    {preset.name}
                  </Typography>
                </Box>
              </Grid>
            ))}
          </Grid>
        </Box>
      </Box>

      {/* Control Buttons */}
      <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
        <Button variant="outlined" size="small" onClick={handleOff} sx={{ flex: 1 }}>
          Turn Off
        </Button>
        <Button
          variant="contained"
          size="small"
          onClick={handleApply}
          sx={{
            flex: 2,
            bgcolor: currentColor,
            color: brightness > 50 ? '#000' : '#FFF',
            '&:hover': {
              bgcolor: currentColor,
              opacity: 0.9,
            },
          }}
        >
          Apply Color
        </Button>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'color_picker',
  name: 'Color Picker',
  description: 'RGB color control for LED devices with presets',
  type: 'rpc',
  tags: ['control', 'color', 'rgb', 'led', 'lighting'],
}

registerWidget(descriptor, ColorPicker)
export default ColorPicker
