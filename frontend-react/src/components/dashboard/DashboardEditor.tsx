/**
 * Dashboard Editor Component
 * Implements drag-and-drop grid system with edit/view modes
 * Matches ThingsBoard's angular-gridster2 functionality
 */

import { useState, useCallback } from 'react'
import { Box, Paper } from '@mui/material'
import GridLayout, { Layout } from 'react-grid-layout'
import 'react-grid-layout/css/styles.css'
import 'react-resizable/css/styles.css'
import { Widget, WidgetData } from '@/types/dashboard'
import WidgetContainer from './WidgetContainer'

interface DashboardEditorProps {
  widgets: Widget[]
  widgetData?: Record<string, WidgetData>
  editMode?: boolean
  onLayoutChange?: (widgets: Widget[]) => void
  onWidgetEdit?: (widget: Widget) => void
  onWidgetDelete?: (widgetId: string) => void
  onWidgetFullscreen?: (widgetId: string) => void
}

export default function DashboardEditor({
  widgets,
  widgetData = {},
  editMode = false,
  onLayoutChange,
  onWidgetEdit,
  onWidgetDelete,
  onWidgetFullscreen,
}: DashboardEditorProps) {
  const [currentWidgets, setCurrentWidgets] = useState<Widget[]>(widgets)

  // Convert widgets to react-grid-layout format
  const layout: Layout[] = currentWidgets.map((widget) => ({
    i: widget.id,
    x: widget.col,
    y: widget.row,
    w: widget.sizeX,
    h: widget.sizeY,
    minW: 2,
    minH: 2,
    maxW: 24,
    maxH: 12,
  }))

  // Handle layout changes (drag/resize)
  const handleLayoutChange = useCallback(
    (newLayout: Layout[]) => {
      const updatedWidgets = currentWidgets.map((widget) => {
        const layoutItem = newLayout.find((item) => item.i === widget.id)
        if (layoutItem) {
          return {
            ...widget,
            col: layoutItem.x,
            row: layoutItem.y,
            sizeX: layoutItem.w,
            sizeY: layoutItem.h,
          }
        }
        return widget
      })
      setCurrentWidgets(updatedWidgets)
      if (onLayoutChange) {
        onLayoutChange(updatedWidgets)
      }
    },
    [currentWidgets, onLayoutChange]
  )

  // Calculate grid row height based on container
  const calculateRowHeight = () => {
    // Base height for each grid row (in pixels)
    // ThingsBoard uses dynamic calculation, we'll use a fixed height for now
    return 60
  }

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: '600px',
        position: 'relative',
      }}
    >
      <GridLayout
        className="dashboard-grid"
        layout={layout}
        cols={24} // ThingsBoard uses 24 columns
        rowHeight={calculateRowHeight()}
        width={1200} // Will be responsive in production
        margin={[10, 10]}
        containerPadding={[0, 0]}
        isDraggable={editMode}
        isResizable={editMode}
        compactType={null} // Don't auto-compact
        preventCollision={false}
        onLayoutChange={handleLayoutChange}
        useCSSTransforms={true}
        draggableHandle=".widget-drag-handle"
      >
        {currentWidgets.map((widget) => (
          <div
            key={widget.id}
            style={{
              display: 'flex',
              flexDirection: 'column',
              height: '100%',
            }}
          >
            <Paper
              elevation={editMode ? 3 : 1}
              sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                position: 'relative',
                overflow: 'hidden',
                border: editMode ? '2px dashed #0F3E5C' : 'none',
                transition: 'all 0.2s ease-in-out',
                '&:hover': editMode
                  ? {
                      boxShadow: 6,
                      borderColor: '#2E7D6F',
                    }
                  : {},
              }}
            >
              {/* Widget drag handle (only visible in edit mode) */}
              {editMode && (
                <Box
                  className="widget-drag-handle"
                  sx={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    height: '40px',
                    backgroundColor: 'rgba(15, 62, 92, 0.1)',
                    cursor: 'move',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 10,
                    '&:hover': {
                      backgroundColor: 'rgba(15, 62, 92, 0.2)',
                    },
                  }}
                >
                  <Box
                    component="span"
                    sx={{
                      fontSize: '12px',
                      color: '#0F3E5C',
                      fontWeight: 'bold',
                    }}
                  >
                    ⋮⋮ Drag to move
                  </Box>
                </Box>
              )}

              {/* Widget content */}
              <Box
                sx={{
                  flex: 1,
                  overflow: 'auto',
                  marginTop: editMode ? '40px' : 0,
                }}
              >
                <WidgetContainer
                  widget={widget}
                  data={widgetData[widget.id]}
                  editMode={editMode}
                  onEdit={onWidgetEdit}
                  onDelete={onWidgetDelete}
                  onFullscreen={onWidgetFullscreen}
                />
              </Box>

              {/* Resize handle indicator (only in edit mode) */}
              {editMode && (
                <Box
                  sx={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    width: '20px',
                    height: '20px',
                    cursor: 'se-resize',
                    '&::after': {
                      content: '""',
                      position: 'absolute',
                      right: '3px',
                      bottom: '3px',
                      width: '10px',
                      height: '10px',
                      borderRight: '2px solid #0F3E5C',
                      borderBottom: '2px solid #0F3E5C',
                    },
                  }}
                />
              )}
            </Paper>
          </div>
        ))}
      </GridLayout>
    </Box>
  )
}
