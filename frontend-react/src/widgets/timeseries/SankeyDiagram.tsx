/**
 * Sankey Diagram Widget - Flow visualization
 * Shows flow between nodes with proportional link widths
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface SankeyNode {
  id: string
  name: string
  color: string
}

interface SankeyLink {
  source: string
  target: string
  value: number
}

function SankeyDiagram({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo Sankey data (energy flow)
  const nodes: SankeyNode[] = config.settings?.nodes || [
    { id: 'solar', name: 'Solar Panel', color: '#FFB300' },
    { id: 'grid', name: 'Power Grid', color: '#0F3E5C' },
    { id: 'battery', name: 'Battery', color: '#2E7D6F' },
    { id: 'loads', name: 'Loads', color: '#C62828' },
    { id: 'hvac', name: 'HVAC', color: '#1E88E5' },
    { id: 'lights', name: 'Lighting', color: '#FBC02D' },
  ]

  const links: SankeyLink[] = config.settings?.links || [
    { source: 'solar', target: 'battery', value: 45 },
    { source: 'solar', target: 'loads', value: 30 },
    { source: 'grid', target: 'loads', value: 25 },
    { source: 'battery', target: 'loads', value: 20 },
    { source: 'loads', target: 'hvac', value: 40 },
    { source: 'loads', target: 'lights', value: 35 },
  ]

  // Calculate node positions (simple layout)
  const width = 600
  const height = 400
  const nodeWidth = 30
  const padding = 50

  // Group nodes by column
  const columns: { [key: string]: number } = {
    solar: 0,
    grid: 0,
    battery: 1,
    loads: 2,
    hvac: 3,
    lights: 3,
  }

  const maxColumn = Math.max(...Object.values(columns))
  const columnWidth = (width - 2 * padding) / maxColumn

  // Position nodes
  const nodePositions: { [key: string]: { x: number; y: number; height: number } } = {}
  const columnNodes: { [key: number]: string[] } = {}

  // Group nodes by column
  nodes.forEach((node) => {
    const col = columns[node.id]
    if (!columnNodes[col]) columnNodes[col] = []
    columnNodes[col].push(node.id)
  })

  // Calculate node heights based on flow
  nodes.forEach((node) => {
    const inFlow = links.filter((l) => l.target === node.id).reduce((sum, l) => sum + l.value, 0)
    const outFlow = links.filter((l) => l.source === node.id).reduce((sum, l) => sum + l.value, 0)
    const flow = Math.max(inFlow, outFlow, 10)

    const col = columns[node.id]
    const colIndex = columnNodes[col].indexOf(node.id)
    const colCount = columnNodes[col].length
    const spacing = (height - 2 * padding) / colCount

    nodePositions[node.id] = {
      x: padding + col * columnWidth,
      y: padding + colIndex * spacing,
      height: Math.min(flow * 2, spacing - 20),
    }
  })

  // Generate SVG path for links (simple curves)
  const generatePath = (link: SankeyLink): string => {
    const source = nodePositions[link.source]
    const target = nodePositions[link.target]

    const x1 = source.x + nodeWidth
    const y1 = source.y + source.height / 2
    const x2 = target.x
    const y2 = target.y + target.height / 2

    const midX = (x1 + x2) / 2

    return `M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2} ${y2}`
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Sankey Diagram'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
          {/* Links */}
          {links.map((link, index) => {
            const sourceNode = nodes.find((n) => n.id === link.source)
            const linkWidth = Math.max(link.value / 5, 2)

            return (
              <g key={index}>
                <path
                  d={generatePath(link)}
                  stroke={sourceNode?.color || '#999'}
                  strokeWidth={linkWidth}
                  fill="none"
                  opacity={0.4}
                  style={{ cursor: 'pointer', transition: 'opacity 0.3s' }}
                  onMouseEnter={(e) => (e.currentTarget.style.opacity = '0.8')}
                  onMouseLeave={(e) => (e.currentTarget.style.opacity = '0.4')}
                >
                  <title>
                    {nodes.find((n) => n.id === link.source)?.name} → {nodes.find((n) => n.id === link.target)?.name}:{' '}
                    {link.value}
                  </title>
                </path>
              </g>
            )
          })}

          {/* Nodes */}
          {nodes.map((node) => {
            const pos = nodePositions[node.id]
            return (
              <g key={node.id}>
                <rect
                  x={pos.x}
                  y={pos.y}
                  width={nodeWidth}
                  height={pos.height}
                  fill={node.color}
                  rx={4}
                  style={{ cursor: 'pointer' }}
                />
                <text
                  x={pos.x + nodeWidth + 5}
                  y={pos.y + pos.height / 2}
                  fontSize="12"
                  fontWeight="bold"
                  fill="#0F3E5C"
                  alignmentBaseline="middle"
                >
                  {node.name}
                </text>
              </g>
            )
          })}
        </svg>
      </Box>

      {/* Legend */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
          Total Flow: {links.reduce((sum, l) => sum + l.value, 0)} units
        </Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'sankey_diagram',
  name: 'Sankey Diagram',
  description: 'Flow visualization showing energy or data transfers',
  type: 'timeseries',
  tags: ['chart', 'sankey', 'flow', 'energy', 'analytics'],
}

registerWidget(descriptor, SankeyDiagram)
export default SankeyDiagram
