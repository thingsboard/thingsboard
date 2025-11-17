/**
 * Network Topology Widget - Visual network diagram
 * Shows device connections and network structure
 */
import { Box, Typography, Paper, Chip, Tooltip } from '@mui/material'
import {
  Router,
  Sensors,
  Computer,
  Cloud,
  CheckCircle,
  Error,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface NetworkNode {
  id: string
  name: string
  type: 'cloud' | 'gateway' | 'router' | 'device'
  status: 'online' | 'offline' | 'warning'
  connections: string[]
  x: number
  y: number
}

function NetworkTopology({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo network topology
  const nodes: NetworkNode[] = [
    { id: 'cloud', name: 'Cloud Server', type: 'cloud', status: 'online', connections: ['gateway1'], x: 300, y: 50 },
    { id: 'gateway1', name: 'Gateway-001', type: 'gateway', status: 'online', connections: ['router1', 'router2'], x: 300, y: 150 },
    { id: 'router1', name: 'Router-A', type: 'router', status: 'online', connections: ['device1', 'device2'], x: 150, y: 250 },
    { id: 'router2', name: 'Router-B', type: 'router', status: 'warning', connections: ['device3', 'device4'], x: 450, y: 250 },
    { id: 'device1', name: 'Sensor-01', type: 'device', status: 'online', connections: [], x: 50, y: 350 },
    { id: 'device2', name: 'Sensor-02', type: 'device', status: 'online', connections: [], x: 250, y: 350 },
    { id: 'device3', name: 'Sensor-03', type: 'device', status: 'offline', connections: [], x: 350, y: 350 },
    { id: 'device4', name: 'Sensor-04', type: 'device', status: 'online', connections: [], x: 550, y: 350 },
  ]

  const nodeConfig = {
    cloud: {
      icon: Cloud,
      color: '#1E88E5',
      size: 50,
    },
    gateway: {
      icon: Router,
      color: '#2E7D6F',
      size: 40,
    },
    router: {
      icon: Computer,
      color: '#FFB300',
      size: 35,
    },
    device: {
      icon: Sensors,
      color: '#757575',
      size: 30,
    },
  }

  const statusConfig = {
    online: { color: '#2E7D6F', label: 'Online' },
    warning: { color: '#FFB300', label: 'Warning' },
    offline: { color: '#C62828', label: 'Offline' },
  }

  const width = 600
  const height = 400

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Network Topology'}
        </Typography>
      )}

      <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
          {/* Connection lines */}
          {nodes.map((node) =>
            node.connections.map((targetId, i) => {
              const target = nodes.find((n) => n.id === targetId)
              if (!target) return null

              return (
                <line
                  key={`${node.id}-${targetId}`}
                  x1={node.x}
                  y1={node.y}
                  x2={target.x}
                  y2={target.y}
                  stroke="#E0E0E0"
                  strokeWidth={2}
                  strokeDasharray={node.status === 'online' && target.status === 'online' ? 'none' : '4 2'}
                />
              )
            })
          )}

          {/* Nodes */}
          {nodes.map((node) => {
            const config = nodeConfig[node.type]
            const statusConf = statusConfig[node.status]
            const Icon = config.icon

            return (
              <g key={node.id}>
                {/* Node circle */}
                <circle
                  cx={node.x}
                  cy={node.y}
                  r={config.size / 2 + 5}
                  fill="white"
                  stroke={statusConf.color}
                  strokeWidth={3}
                  style={{ cursor: 'pointer' }}
                />

                {/* Node icon */}
                <g transform={`translate(${node.x - config.size / 2}, ${node.y - config.size / 2})`}>
                  <foreignObject width={config.size} height={config.size}>
                    <Box
                      sx={{
                        width: config.size,
                        height: config.size,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <Icon sx={{ fontSize: config.size * 0.6, color: config.color }} />
                    </Box>
                  </foreignObject>
                </g>

                {/* Status indicator */}
                <circle
                  cx={node.x + config.size / 2}
                  cy={node.y - config.size / 2}
                  r={6}
                  fill={statusConf.color}
                />

                {/* Label */}
                <text
                  x={node.x}
                  y={node.y + config.size / 2 + 20}
                  fontSize="11"
                  fontWeight="bold"
                  fill="#0F3E5C"
                  textAnchor="middle"
                >
                  {node.name}
                </text>
              </g>
            )
          })}
        </svg>
      </Box>

      {/* Legend */}
      <Box sx={{ mt: 2, display: 'flex', gap: 2, flexWrap: 'wrap', p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        {Object.entries({
          online: nodes.filter((n) => n.status === 'online').length,
          warning: nodes.filter((n) => n.status === 'warning').length,
          offline: nodes.filter((n) => n.status === 'offline').length,
        }).map(([status, count]) => {
          const conf = statusConfig[status as keyof typeof statusConfig]
          return (
            <Chip
              key={status}
              icon={status === 'online' ? <CheckCircle sx={{ fontSize: 14 }} /> : <Error sx={{ fontSize: 14 }} />}
              label={`${conf.label}: ${count}`}
              size="small"
              sx={{
                fontSize: '10px',
                height: 22,
                bgcolor: conf.color,
                color: 'white',
              }}
            />
          )
        })}
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'network_topology',
  name: 'Network Topology',
  description: 'Visual network diagram showing device connections',
  type: 'latest',
  tags: ['network', 'topology', 'diagram', 'connections', 'infrastructure'],
}

registerWidget(descriptor, NetworkTopology)
export default NetworkTopology
