/**
 * Widget Container
 * Renders a widget based on its type using the widget registry
 */

import { Box, Paper, IconButton, Menu, MenuItem, ListItemIcon, ListItemText } from '@mui/material'
import { MoreVert, Settings, Delete, Fullscreen } from '@mui/icons-material'
import { useState } from 'react'
import { Widget, WidgetData } from '@/types/dashboard'
import { widgetRegistry } from '@/widgets'

interface WidgetContainerProps {
  widget: Widget
  data?: WidgetData
  editMode?: boolean
  onEdit?: (widget: Widget) => void
  onDelete?: (widgetId: string) => void
  onFullscreen?: (widgetId: string) => void
}

export default function WidgetContainer({
  widget,
  data,
  editMode = false,
  onEdit,
  onDelete,
  onFullscreen,
}: WidgetContainerProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation()
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleEdit = () => {
    handleMenuClose()
    onEdit?.(widget)
  }

  const handleDelete = () => {
    handleMenuClose()
    onDelete?.(widget.id)
  }

  const handleFullscreen = () => {
    handleMenuClose()
    onFullscreen?.(widget.id)
  }

  // Get widget component from registry
  const WidgetComponent = widgetRegistry.getComponent(widget.typeId)

  if (!WidgetComponent) {
    return (
      <Box
        sx={{
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: (theme) =>
            theme.palette.mode === 'dark' ? '#1F2428' : '#FFFFFF',
          border: '1px solid',
          borderColor: (theme) =>
            theme.palette.mode === 'dark' ? '#363C42' : '#E0E0E0',
          borderRadius: 2,
        }}
      >
        <Box sx={{ textAlign: 'center', color: '#8C959D' }}>
          <p>Widget type not found: {widget.typeId}</p>
        </Box>
      </Box>
    )
  }

  // Default empty data
  const widgetData: WidgetData = data || {
    datasources: [],
    latestValues: {},
    timeseriesData: {},
    alarms: [],
  }

  return (
    <Box
      sx={{
        height: '100%',
        position: 'relative',
        '&:hover .widget-menu-button': {
          opacity: editMode ? 1 : 0,
        },
      }}
    >
      {/* Widget Menu */}
      {editMode && (
        <>
          <IconButton
            className="widget-menu-button"
            onClick={handleMenuOpen}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              zIndex: 10,
              bgcolor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              opacity: 0,
              transition: 'opacity 0.2s',
              '&:hover': {
                bgcolor: 'rgba(0, 0, 0, 0.7)',
              },
            }}
            size="small"
          >
            <MoreVert fontSize="small" />
          </IconButton>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
          >
            <MenuItem onClick={handleEdit}>
              <ListItemIcon>
                <Settings fontSize="small" />
              </ListItemIcon>
              <ListItemText>Edit Widget</ListItemText>
            </MenuItem>
            <MenuItem onClick={handleFullscreen}>
              <ListItemIcon>
                <Fullscreen fontSize="small" />
              </ListItemIcon>
              <ListItemText>Fullscreen</ListItemText>
            </MenuItem>
            <MenuItem onClick={handleDelete}>
              <ListItemIcon>
                <Delete fontSize="small" />
              </ListItemIcon>
              <ListItemText>Remove</ListItemText>
            </MenuItem>
          </Menu>
        </>
      )}

      {/* Render Widget */}
      <WidgetComponent widget={widget} data={widgetData} editMode={editMode} />
    </Box>
  )
}
