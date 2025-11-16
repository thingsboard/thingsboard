/**
 * Rule Chain Designer Page
 * Visual node-based rule chain editor with drag-and-drop
 * Matches ThingsBoard ui-ngx rule chain designer
 */

import { useState, useCallback, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  MiniMap,
  applyNodeChanges,
  applyEdgeChanges,
  addEdge,
  NodeChange,
  EdgeChange,
  Connection,
  NodeTypes,
} from 'reactflow'
import 'reactflow/dist/style.css'
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemText,
  ListSubheader,
  Typography,
  IconButton,
  Tooltip,
  Button,
  Paper,
  TextField,
  Switch,
  FormControlLabel,
  Divider,
  Chip,
} from '@mui/material'
import {
  ArrowBack,
  Save,
  FileDownload,
  FileUpload,
  BugReport,
  PlayArrow,
  Stop,
  Settings as SettingsIcon,
  ContentCopy,
  Delete,
} from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

// Node categories matching ThingsBoard
const NODE_CATEGORIES = {
  FILTER: [
    { type: 'filter-message-type', label: 'Message Type Filter', icon: '🔍' },
    { type: 'filter-script', label: 'Script Filter', icon: '📝' },
    { type: 'filter-switch', label: 'Switch Node', icon: '🔀' },
    { type: 'filter-check-relation', label: 'Check Relation Filter', icon: '🔗' },
  ],
  ENRICHMENT: [
    { type: 'enrichment-customer-details', label: 'Customer Details', icon: '👤' },
    { type: 'enrichment-device-details', label: 'Device Details', icon: '📱' },
    { type: 'enrichment-related-attributes', label: 'Related Attributes', icon: '🏷️' },
    { type: 'enrichment-tenant-details', label: 'Tenant Details', icon: '🏢' },
    { type: 'enrichment-originator-attributes', label: 'Originator Attributes', icon: '📋' },
  ],
  TRANSFORMATION: [
    { type: 'transformation-script', label: 'Script Transformation', icon: '⚙️' },
    { type: 'transformation-change-originator', label: 'Change Originator', icon: '🔄' },
    { type: 'transformation-to-email', label: 'To Email', icon: '📧' },
  ],
  ACTION: [
    { type: 'action-create-alarm', label: 'Create Alarm', icon: '🚨' },
    { type: 'action-clear-alarm', label: 'Clear Alarm', icon: '✅' },
    { type: 'action-save-attributes', label: 'Save Attributes', icon: '💾' },
    { type: 'action-save-timeseries', label: 'Save Timeseries', icon: '📊' },
    { type: 'action-rpc-call', label: 'RPC Call Request', icon: '📞' },
    { type: 'action-create-relation', label: 'Create Relation', icon: '🔗' },
    { type: 'action-delete-relation', label: 'Delete Relation', icon: '❌' },
  ],
  EXTERNAL: [
    { type: 'external-rest-api', label: 'REST API Call', icon: '🌐' },
    { type: 'external-mqtt', label: 'MQTT Node', icon: '📡' },
    { type: 'external-kafka', label: 'Kafka Node', icon: '📨' },
    { type: 'external-send-email', label: 'Send Email', icon: '✉️' },
    { type: 'external-aws-sns', label: 'AWS SNS', icon: '☁️' },
    { type: 'external-aws-sqs', label: 'AWS SQS', icon: '📮' },
  ],
  FLOW: [
    { type: 'flow-rule-chain', label: 'Rule Chain Node', icon: '🔗' },
    { type: 'flow-checkpoint', label: 'Checkpoint', icon: '🚩' },
    { type: 'flow-log', label: 'Log', icon: '📄' },
  ],
}

const DRAWER_WIDTH = 280
const PROPERTIES_WIDTH = 320

interface RuleNode extends Node {
  data: {
    label: string
    nodeType: string
    icon?: string
    configuration?: any
  }
}

export default function RuleChainDesignerPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  // Canvas state
  const [nodes, setNodes] = useState<RuleNode[]>([
    {
      id: 'input-1',
      type: 'custom',
      position: { x: 250, y: 100 },
      data: {
        label: 'Input',
        nodeType: 'input',
        icon: '📥',
      },
    },
  ])
  const [edges, setEdges] = useState<Edge[]>([])
  const [selectedNode, setSelectedNode] = useState<RuleNode | null>(null)

  // UI state
  const [debugMode, setDebugMode] = useState(false)
  const [isRunning, setIsRunning] = useState(false)
  const [rulChainName, setRuleChainName] = useState('Root Rule Chain')

  const reactFlowWrapper = useRef<HTMLDivElement>(null)

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => setNodes((nds) => applyNodeChanges(changes, nds) as RuleNode[]),
    []
  )

  const onEdgesChange = useCallback(
    (changes: EdgeChange[]) => setEdges((eds) => applyEdgeChanges(changes, eds)),
    []
  )

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge({ ...params, animated: true }, eds)),
    []
  )

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => {
    setSelectedNode(node as RuleNode)
  }, [])

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
  }, [])

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault()

      const type = event.dataTransfer.getData('application/reactflow')
      const label = event.dataTransfer.getData('nodeLabel')
      const icon = event.dataTransfer.getData('nodeIcon')

      if (typeof type === 'undefined' || !type || !reactFlowWrapper.current) {
        return
      }

      const reactFlowBounds = reactFlowWrapper.current.getBoundingClientRect()
      const position = {
        x: event.clientX - reactFlowBounds.left - 75,
        y: event.clientY - reactFlowBounds.top - 25,
      }

      const newNode: RuleNode = {
        id: `${type}-${Date.now()}`,
        type: 'custom',
        position,
        data: {
          label,
          nodeType: type,
          icon,
          configuration: {},
        },
      }

      setNodes((nds) => nds.concat(newNode))
    },
    []
  )

  const onDragStart = (event: React.DragEvent, nodeType: string, label: string, icon: string) => {
    event.dataTransfer.setData('application/reactflow', nodeType)
    event.dataTransfer.setData('nodeLabel', label)
    event.dataTransfer.setData('nodeIcon', icon)
    event.dataTransfer.effectAllowed = 'move'
  }

  const handleSave = () => {
    const ruleChainData = {
      name: rulChainName,
      nodes,
      edges,
      debugMode,
    }
    console.log('Saving rule chain:', ruleChainData)
    // API call would go here
  }

  const handleExport = () => {
    const exportData = {
      ruleChain: {
        name: rulChainName,
        debugMode,
        nodes,
        edges,
      },
      metadata: {
        exportTime: new Date().toISOString(),
        version: '1.0',
      },
    }

    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: 'application/json',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${rulChainName.replace(/\s+/g, '_').toLowerCase()}_rule_chain.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  const handleDeleteNode = () => {
    if (selectedNode) {
      setNodes((nds) => nds.filter((n) => n.id !== selectedNode.id))
      setEdges((eds) =>
        eds.filter((e) => e.source !== selectedNode.id && e.target !== selectedNode.id)
      )
      setSelectedNode(null)
    }
  }

  const handleDuplicateNode = () => {
    if (selectedNode) {
      const newNode: RuleNode = {
        ...selectedNode,
        id: `${selectedNode.data.nodeType}-${Date.now()}`,
        position: {
          x: selectedNode.position.x + 50,
          y: selectedNode.position.y + 50,
        },
      }
      setNodes((nds) => nds.concat(newNode))
    }
  }

  // Custom node component
  const CustomNode = ({ data }: { data: RuleNode['data'] }) => (
    <Box
      sx={{
        px: 2,
        py: 1.5,
        minWidth: 150,
        bgcolor: 'white',
        border: '2px solid #0F3E5C',
        borderRadius: 1,
        boxShadow: 2,
        '&:hover': {
          boxShadow: 4,
          borderColor: '#FFB300',
        },
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Typography sx={{ fontSize: '1.2rem' }}>{data.icon}</Typography>
        <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, color: '#0F3E5C' }}>
          {data.label}
        </Typography>
      </Box>
    </Box>
  )

  const nodeTypes: NodeTypes = {
    custom: CustomNode,
  }

  return (
    <MainLayout>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 64px)' }}>
        {/* Top Toolbar */}
        <Paper
          elevation={2}
          sx={{
            p: 2,
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            borderRadius: 0,
          }}
        >
          <IconButton onClick={() => navigate('/rule-chains')}>
            <ArrowBack />
          </IconButton>

          <TextField
            value={rulChainName}
            onChange={(e) => setRuleChainName(e.target.value)}
            variant="standard"
            sx={{ flex: 1, maxWidth: 400 }}
            InputProps={{
              sx: { fontSize: '1.25rem', fontWeight: 600 },
            }}
          />

          <Box sx={{ flex: 1 }} />

          <FormControlLabel
            control={
              <Switch
                checked={debugMode}
                onChange={(e) => setDebugMode(e.target.checked)}
                icon={<BugReport />}
                checkedIcon={<BugReport />}
              />
            }
            label="Debug Mode"
          />

          <Tooltip title={isRunning ? 'Stop Testing' : 'Start Testing'}>
            <IconButton
              onClick={() => setIsRunning(!isRunning)}
              sx={{
                bgcolor: isRunning ? '#C62828' : '#2E7D6F',
                color: 'white',
                '&:hover': { bgcolor: isRunning ? '#B71C1C' : '#26695C' },
              }}
            >
              {isRunning ? <Stop /> : <PlayArrow />}
            </IconButton>
          </Tooltip>

          <Tooltip title="Export">
            <IconButton onClick={handleExport}>
              <FileDownload />
            </IconButton>
          </Tooltip>

          <Button
            variant="contained"
            startIcon={<Save />}
            onClick={handleSave}
            sx={{ bgcolor: '#0F3E5C' }}
          >
            Save
          </Button>
        </Paper>

        {/* Main Content Area */}
        <Box sx={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
          {/* Left Drawer - Node Palette */}
          <Drawer
            variant="permanent"
            sx={{
              width: DRAWER_WIDTH,
              flexShrink: 0,
              '& .MuiDrawer-paper': {
                width: DRAWER_WIDTH,
                boxSizing: 'border-box',
                position: 'relative',
                overflowY: 'auto',
              },
            }}
          >
            <Box sx={{ p: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 600, color: '#0F3E5C', mb: 2 }}>
                Node Library
              </Typography>

              {Object.entries(NODE_CATEGORIES).map(([category, nodes]) => (
                <List
                  key={category}
                  subheader={
                    <ListSubheader
                      sx={{
                        bgcolor: 'transparent',
                        color: '#0F3E5C',
                        fontWeight: 600,
                        lineHeight: '32px',
                      }}
                    >
                      {category}
                    </ListSubheader>
                  }
                >
                  {nodes.map((node) => (
                    <ListItem
                      key={node.type}
                      draggable
                      onDragStart={(e) => onDragStart(e, node.type, node.label, node.icon)}
                      sx={{
                        cursor: 'grab',
                        bgcolor: '#F5F5F5',
                        mb: 1,
                        borderRadius: 1,
                        border: '1px solid #E0E0E0',
                        '&:hover': {
                          bgcolor: '#E3F2FD',
                          borderColor: '#0F3E5C',
                        },
                        '&:active': {
                          cursor: 'grabbing',
                        },
                      }}
                    >
                      <Typography sx={{ mr: 1 }}>{node.icon}</Typography>
                      <ListItemText
                        primary={node.label}
                        primaryTypographyProps={{
                          fontSize: '0.875rem',
                          fontWeight: 500,
                        }}
                      />
                    </ListItem>
                  ))}
                  {category !== 'FLOW' && <Divider sx={{ my: 1 }} />}
                </List>
              ))}
            </Box>
          </Drawer>

          {/* Center - ReactFlow Canvas */}
          <Box
            ref={reactFlowWrapper}
            sx={{ flex: 1, bgcolor: '#FAFAFA', position: 'relative' }}
          >
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onConnect={onConnect}
              onNodeClick={onNodeClick}
              onDrop={onDrop}
              onDragOver={onDragOver}
              nodeTypes={nodeTypes}
              fitView
            >
              <Background />
              <Controls />
              <MiniMap
                nodeColor={(node) => {
                  const ruleNode = node as RuleNode
                  if (ruleNode.data.nodeType.includes('filter')) return '#2196F3'
                  if (ruleNode.data.nodeType.includes('enrichment')) return '#4CAF50'
                  if (ruleNode.data.nodeType.includes('transformation')) return '#FF9800'
                  if (ruleNode.data.nodeType.includes('action')) return '#F44336'
                  if (ruleNode.data.nodeType.includes('external')) return '#9C27B0'
                  return '#0F3E5C'
                }}
              />
            </ReactFlow>
          </Box>

          {/* Right Panel - Node Properties */}
          {selectedNode && (
            <Paper
              elevation={3}
              sx={{
                width: PROPERTIES_WIDTH,
                overflowY: 'auto',
                p: 2,
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6" sx={{ flex: 1, fontWeight: 600, color: '#0F3E5C' }}>
                  Node Properties
                </Typography>
                <Tooltip title="Duplicate">
                  <IconButton size="small" onClick={handleDuplicateNode}>
                    <ContentCopy fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Delete">
                  <IconButton size="small" onClick={handleDeleteNode} color="error">
                    <Delete fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>

              <Divider sx={{ mb: 2 }} />

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box>
                  <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                    Node Type
                  </Typography>
                  <Chip
                    label={selectedNode.data.nodeType}
                    size="small"
                    sx={{ bgcolor: '#E3F2FD', color: '#0F3E5C' }}
                  />
                </Box>

                <TextField
                  label="Node Name"
                  value={selectedNode.data.label}
                  onChange={(e) => {
                    setNodes((nds) =>
                      nds.map((n) =>
                        n.id === selectedNode.id
                          ? { ...n, data: { ...n.data, label: e.target.value } }
                          : n
                      )
                    )
                    setSelectedNode({
                      ...selectedNode,
                      data: { ...selectedNode.data, label: e.target.value },
                    })
                  }}
                  fullWidth
                  size="small"
                />

                <Box>
                  <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                    Configuration
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    Configuration options for {selectedNode.data.nodeType} will appear here.
                  </Typography>
                </Box>

                <Divider />

                <Box>
                  <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
                    Position
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    X: {Math.round(selectedNode.position.x)}, Y:{' '}
                    {Math.round(selectedNode.position.y)}
                  </Typography>
                </Box>
              </Box>
            </Paper>
          )}
        </Box>
      </Box>
    </MainLayout>
  )
}
