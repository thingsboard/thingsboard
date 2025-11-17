/**
 * Treemap Chart Widget - Hierarchical data visualization
 * Shows nested rectangles representing data hierarchy
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface TreeNode {
  name: string
  value: number
  color: string
}

function TreemapChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo treemap data (resource usage)
  const treeData: TreeNode[] = config.settings?.data || [
    { name: 'CPU Usage', value: 35, color: '#0F3E5C' },
    { name: 'Memory', value: 28, color: '#2E7D6F' },
    { name: 'Storage', value: 18, color: '#4CAF50' },
    { name: 'Network', value: 12, color: '#FFB300' },
    { name: 'GPU', value: 7, color: '#FF6B6B' },
  ]

  const totalValue = treeData.reduce((sum, node) => sum + node.value, 0)

  // Simple treemap layout algorithm
  const calculateLayout = () => {
    const containerWidth = 100
    const containerHeight = 100
    let currentX = 0
    let currentY = 0
    let rowHeight = 0
    let rowWidth = 0

    return treeData.map((node, index) => {
      const areaPercent = (node.value / totalValue) * 100
      const width = Math.sqrt(areaPercent) * 10
      const height = areaPercent / (Math.sqrt(areaPercent) * 10) * 10

      if (currentX + width > containerWidth) {
        currentY += rowHeight
        currentX = 0
        rowHeight = 0
        rowWidth = 0
      }

      const layout = {
        x: currentX,
        y: currentY,
        width: Math.min(width, containerWidth - currentX),
        height: Math.max(height, 15),
      }

      currentX += layout.width
      rowHeight = Math.max(rowHeight, layout.height)
      rowWidth += layout.width

      return { ...node, ...layout }
    })
  }

  const layoutedData = calculateLayout()

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Treemap Chart'}
        </Typography>
      )}

      <Box
        sx={{
          flex: 1,
          position: 'relative',
          minHeight: 200,
          bgcolor: '#F5F5F5',
          borderRadius: 1,
          overflow: 'hidden',
        }}
      >
        {layoutedData.map((node) => {
          const percent = ((node.value / totalValue) * 100).toFixed(1)

          return (
            <Box
              key={node.name}
              sx={{
                position: 'absolute',
                left: `${node.x}%`,
                top: `${node.y}%`,
                width: `${node.width}%`,
                height: `${node.height}%`,
                bgcolor: node.color,
                border: '2px solid white',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                p: 1,
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                '&:hover': {
                  opacity: 0.8,
                  transform: 'scale(1.02)',
                  zIndex: 10,
                },
              }}
            >
              <Typography
                variant="caption"
                sx={{
                  color: 'white',
                  fontWeight: 'bold',
                  fontSize: '11px',
                  textAlign: 'center',
                  mb: 0.5,
                  textShadow: '1px 1px 2px rgba(0,0,0,0.5)',
                }}
              >
                {node.name}
              </Typography>
              <Typography
                variant="h6"
                sx={{
                  color: 'white',
                  fontWeight: 'bold',
                  fontSize: '16px',
                  textShadow: '1px 1px 2px rgba(0,0,0,0.5)',
                }}
              >
                {percent}%
              </Typography>
              <Typography
                variant="caption"
                sx={{
                  color: 'rgba(255,255,255,0.9)',
                  fontSize: '10px',
                  textShadow: '1px 1px 2px rgba(0,0,0,0.5)',
                }}
              >
                {node.value}
              </Typography>
            </Box>
          )
        })}
      </Box>

      {/* Legend */}
      <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 1, justifyContent: 'center' }}>
        {treeData.map((node) => (
          <Box key={node.name} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Box sx={{ width: 12, height: 12, bgcolor: node.color, borderRadius: 0.5 }} />
            <Typography variant="caption" sx={{ fontSize: '11px', color: '#757575' }}>
              {node.name}
            </Typography>
          </Box>
        ))}
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'treemap_chart',
  name: 'Treemap Chart',
  description: 'Hierarchical data visualization with nested rectangles',
  type: 'timeseries',
  tags: ['chart', 'treemap', 'hierarchy', 'nested'],
}

registerWidget(descriptor, TreemapChart)
export default TreemapChart
