/**
 * Rule Chain Editor - Main Canvas Component
 * Visual drag-and-drop editor for building automation workflows
 */

import React, { useCallback, useRef, useState, useMemo } from 'react'
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  Node,
  Edge,
  Connection,
  addEdge,
  useNodesState,
  useEdgesState,
  MarkerType,
  NodeTypes,
  ReactFlowProvider,
  Panel,
} from 'reactflow'
import 'reactflow/dist/style.css'
import {
  Box,
  Paper,
  IconButton,
  Tooltip,
  Typography,
  Chip,
  Stack,
  Divider,
} from '@mui/material'
import {
  Save,
  PlayArrow,
  BugReport,
  FitScreen,
  Delete,
  Settings as SettingsIcon,
} from '@mui/icons-material'
import RuleNode, { RuleNodeData } from './RuleNode'
import NodeLibrary from './NodeLibrary'
import NodeConfigDialog from './NodeConfigDialog'
import {
  RuleNodeComponentDescriptor,
  RuleNodeType,
  INPUT_NODE,
  ruleNodeTypeDescriptors,
} from '../../types/rulechain.types'

interface RuleChainEditorProps {
  ruleChainName?: string
  debugMode?: boolean
  onSave?: (nodes: Node[], edges: Edge[]) => void
  onTest?: () => void
}

let nodeIdCounter = 0

function RuleChainEditorComponent({
  ruleChainName = 'Untitled Rule Chain',
  debugMode = false,
  onSave,
  onTest,
}: RuleChainEditorProps) {
  const reactFlowWrapper = useRef<HTMLDivElement>(null)
  const [nodes, setNodes, onNodesChange] = useNodesState<RuleNodeData>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [selectedNode, setSelectedNode] = useState<Node<RuleNodeData> | null>(null)
  const [configDialogOpen, setConfigDialogOpen] = useState(false)
  const [reactFlowInstance, setReactFlowInstance] = useState<any>(null)

  // Define custom node types
  const nodeTypes: NodeTypes = useMemo(() => ({ ruleNode: RuleNode }), [])

  // Initialize with Input node
  React.useEffect(() => {
    if (nodes.length === 0) {
      const inputNode: Node<RuleNodeData> = {
        id: 'input-node',
        type: 'ruleNode',
        position: { x: 100, y: 250 },
        data: {
          label: 'Input',
          type: RuleNodeType.INPUT,
          componentName: INPUT_NODE.name,
          debugMode: false,
          onConfigure: () => handleConfigureNode('input-node'),
        },
        draggable: true,
        selectable: true,
      }
      setNodes([inputNode])
    }
  }, [])

  // Handle connection creation
  const onConnect = useCallback(
    (params: Connection) => {
      if (!params.source || !params.target) return

      const edge: Edge = {
        id: `e${params.source}-${params.target}`,
        source: params.source,
        target: params.target,
        sourceHandle: params.sourceHandle,
        targetHandle: params.targetHandle,
        type: 'smoothstep',
        animated: debugMode,
        label: 'Success',
        style: { stroke: '#2E7D6F', strokeWidth: 2 },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: '#2E7D6F',
        },
      }
      setEdges((eds) => addEdge(edge, eds))
    },
    [debugMode, setEdges]
  )

  // Handle node drag from library
  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
  }, [])

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault()

      if (!reactFlowWrapper.current || !reactFlowInstance) return

      const componentData = event.dataTransfer.getData('application/reactflow')
      if (!componentData) return

      const component: RuleNodeComponentDescriptor = JSON.parse(componentData)
      const bounds = reactFlowWrapper.current.getBoundingClientRect()
      const position = reactFlowInstance.screenToFlowPosition({
        x: event.clientX - bounds.left,
        y: event.clientY - bounds.top,
      })

      const newNodeId = `node-${++nodeIdCounter}`
      const newNode: Node<RuleNodeData> = {
        id: newNodeId,
        type: 'ruleNode',
        position,
        data: {
          label: component.name,
          type: component.type,
          componentName: component.name,
          debugMode: false,
          onConfigure: () => handleConfigureNode(newNodeId),
        },
      }

      setNodes((nds) => nds.concat(newNode))
    },
    [reactFlowInstance, setNodes]
  )

  // Handle node selection from library (double-click)
  const handleNodeSelect = useCallback(
    (component: RuleNodeComponentDescriptor) => {
      const newNodeId = `node-${++nodeIdCounter}`
      const newNode: Node<RuleNodeData> = {
        id: newNodeId,
        type: 'ruleNode',
        position: { x: 250 + nodes.length * 50, y: 250 + (nodes.length % 3) * 100 },
        data: {
          label: component.name,
          type: component.type,
          componentName: component.name,
          debugMode: false,
          onConfigure: () => handleConfigureNode(newNodeId),
        },
      }

      setNodes((nds) => nds.concat(newNode))
    },
    [nodes.length, setNodes]
  )

  // Handle node configuration
  const handleConfigureNode = useCallback(
    (nodeId: string) => {
      const node = nodes.find((n) => n.id === nodeId)
      if (node) {
        setSelectedNode(node)
        setConfigDialogOpen(true)
      }
    },
    [nodes]
  )

  const handleSaveConfiguration = useCallback(
    (nodeId: string, configuration: any) => {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === nodeId) {
            return {
              ...node,
              data: {
                ...node.data,
                configuration,
              },
            }
          }
          return node
        })
      )
      setConfigDialogOpen(false)
    },
    [setNodes]
  )

  // Delete selected nodes
  const handleDeleteSelected = useCallback(() => {
    setNodes((nds) => nds.filter((node) => !node.selected || node.id === 'input-node'))
    setEdges((eds) => eds.filter((edge) => !edge.selected))
  }, [setNodes, setEdges])

  // Fit view
  const handleFitView = useCallback(() => {
    if (reactFlowInstance) {
      reactFlowInstance.fitView({ padding: 0.2 })
    }
  }, [reactFlowInstance])

  // Save rule chain
  const handleSave = useCallback(() => {
    if (onSave) {
      onSave(nodes, edges)
    }
  }, [nodes, edges, onSave])

  // Calculate stats
  const nodeStats = useMemo(() => {
    const stats: Record<RuleNodeType, number> = {
      [RuleNodeType.FILTER]: 0,
      [RuleNodeType.ENRICHMENT]: 0,
      [RuleNodeType.TRANSFORMATION]: 0,
      [RuleNodeType.ACTION]: 0,
      [RuleNodeType.EXTERNAL]: 0,
      [RuleNodeType.FLOW]: 0,
      [RuleNodeType.INPUT]: 0,
      [RuleNodeType.UNKNOWN]: 0,
    }

    nodes.forEach((node) => {
      if (node.data.type) {
        stats[node.data.type]++
      }
    })

    return stats
  }, [nodes])

  return (
    <Box sx={{ display: 'flex', height: '100%', width: '100%', position: 'relative' }}>
      {/* Node Library Sidebar */}
      <NodeLibrary onNodeSelect={handleNodeSelect} />

      {/* Main Canvas */}
      <Box ref={reactFlowWrapper} sx={{ flex: 1, position: 'relative', bgcolor: '#F5F5F5' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onInit={setReactFlowInstance}
          onDrop={onDrop}
          onDragOver={onDragOver}
          nodeTypes={nodeTypes}
          fitView
          deleteKeyCode="Delete"
        >
          <Background />
          <Controls />
          <MiniMap
            nodeColor={(node) => {
              const data = node.data as RuleNodeData
              return ruleNodeTypeDescriptors[data.type]?.color || '#9E9E9E'
            }}
          />

          {/* Top Toolbar */}
          <Panel position="top-center">
            <Paper
              elevation={3}
              sx={{
                p: 1,
                display: 'flex',
                alignItems: 'center',
                gap: 2,
                bgcolor: 'background.paper',
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <SettingsIcon color="primary" />
                <Typography variant="h6" fontWeight={600}>
                  {ruleChainName}
                </Typography>
              </Box>

              <Divider orientation="vertical" flexItem />

              <Stack direction="row" spacing={1}>
                {Object.entries(nodeStats)
                  .filter(([type, count]) => count > 0 && type !== RuleNodeType.INPUT)
                  .map(([type, count]) => {
                    const descriptor = ruleNodeTypeDescriptors[type as RuleNodeType]
                    return (
                      <Chip
                        key={type}
                        label={`${descriptor.name}: ${count}`}
                        size="small"
                        sx={{
                          bgcolor: descriptor.color,
                          color: 'white',
                          fontWeight: 600,
                        }}
                      />
                    )
                  })}
              </Stack>

              <Divider orientation="vertical" flexItem />

              <Box sx={{ display: 'flex', gap: 0.5 }}>
                <Tooltip title="Save">
                  <IconButton onClick={handleSave} size="small" color="primary">
                    <Save />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Test Rule Chain">
                  <IconButton onClick={onTest} size="small" color="success">
                    <PlayArrow />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Delete Selected">
                  <IconButton onClick={handleDeleteSelected} size="small" color="error">
                    <Delete />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Fit View">
                  <IconButton onClick={handleFitView} size="small">
                    <FitScreen />
                  </IconButton>
                </Tooltip>
              </Box>

              {debugMode && (
                <>
                  <Divider orientation="vertical" flexItem />
                  <Chip icon={<BugReport />} label="Debug Mode" color="warning" size="small" />
                </>
              )}
            </Paper>
          </Panel>

          {/* Bottom Stats */}
          <Panel position="bottom-right">
            <Paper
              elevation={2}
              sx={{
                p: 1,
                display: 'flex',
                gap: 2,
                bgcolor: 'background.paper',
                fontSize: '0.75rem',
              }}
            >
              <Typography variant="caption">
                <strong>Nodes:</strong> {nodes.length}
              </Typography>
              <Typography variant="caption">
                <strong>Connections:</strong> {edges.length}
              </Typography>
            </Paper>
          </Panel>
        </ReactFlow>
      </Box>

      {/* Node Configuration Dialog */}
      {selectedNode && (
        <NodeConfigDialog
          open={configDialogOpen}
          node={selectedNode}
          onClose={() => setConfigDialogOpen(false)}
          onSave={handleSaveConfiguration}
        />
      )}
    </Box>
  )
}

export default function RuleChainEditor(props: RuleChainEditorProps) {
  return (
    <ReactFlowProvider>
      <RuleChainEditorComponent {...props} />
    </ReactFlowProvider>
  )
}
