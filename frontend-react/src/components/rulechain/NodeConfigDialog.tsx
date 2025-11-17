/**
 * Node Configuration Dialog
 * Configure rule node parameters and settings
 */

import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Switch,
  FormControlLabel,
  Tabs,
  Tab,
  Alert,
  Chip,
  Divider,
  IconButton,
} from '@mui/material'
import { Close, Code, Settings, Info } from '@mui/icons-material'
import { Node } from 'reactflow'
import { RuleNodeData } from './RuleNode'
import { RULE_NODE_COMPONENTS, ruleNodeTypeDescriptors } from '../../types/rulechain.types'

interface NodeConfigDialogProps {
  open: boolean
  node: Node<RuleNodeData>
  onClose: () => void
  onSave: (nodeId: string, configuration: any) => void
}

interface TabPanelProps {
  children?: React.ReactNode
  value: number
  index: number
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <div role="tabpanel" hidden={value !== index}>
      {value === index && <Box sx={{ pt: 2 }}>{children}</Box>}
    </div>
  )
}

export default function NodeConfigDialog({ open, node, onClose, onSave }: NodeConfigDialogProps) {
  const [activeTab, setActiveTab] = useState(0)
  const [nodeName, setNodeName] = useState(node.data.label)
  const [debugMode, setDebugMode] = useState(node.data.debugMode || false)
  const [configuration, setConfiguration] = useState<any>({})
  const [queueName, setQueueName] = useState('Main')

  // Find component descriptor
  const component = RULE_NODE_COMPONENTS.find((c) => c.name === node.data.componentName)
  const typeDescriptor = ruleNodeTypeDescriptors[node.data.type]
  const defaultConfig = component?.configurationDescriptor?.nodeDefinition.defaultConfiguration || {}

  useEffect(() => {
    setNodeName(node.data.label)
    setDebugMode(node.data.debugMode || false)
    setConfiguration(defaultConfig)
  }, [node, defaultConfig])

  const handleSave = () => {
    onSave(node.id, {
      name: nodeName,
      debugMode,
      configuration,
      queueName,
    })
  }

  const handleConfigChange = (key: string, value: any) => {
    setConfiguration((prev: any) => ({
      ...prev,
      [key]: value,
    }))
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      {/* Header */}
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Box
              sx={{
                width: 40,
                height: 40,
                borderRadius: 1,
                bgcolor: typeDescriptor.color,
                color: 'white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Settings />
            </Box>
            <Box>
              <Typography variant="h6">Configure Node</Typography>
              <Typography variant="caption" color="text.secondary">
                {typeDescriptor.name} • {node.data.componentName}
              </Typography>
            </Box>
          </Box>
          <IconButton onClick={onClose} size="small">
            <Close />
          </IconButton>
        </Box>
      </DialogTitle>

      <Divider />

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', px: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab icon={<Settings fontSize="small" />} label="General" iconPosition="start" />
          <Tab icon={<Code fontSize="small" />} label="Configuration" iconPosition="start" />
          <Tab icon={<Info fontSize="small" />} label="Info" iconPosition="start" />
        </Tabs>
      </Box>

      <DialogContent sx={{ minHeight: 400 }}>
        {/* General Tab */}
        <TabPanel value={activeTab} index={0}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <TextField
              label="Node Name"
              value={nodeName}
              onChange={(e) => setNodeName(e.target.value)}
              fullWidth
              helperText="A descriptive name for this node"
            />

            <TextField
              label="Queue Name"
              value={queueName}
              onChange={(e) => setQueueName(e.target.value)}
              fullWidth
              helperText="Processing queue for this node"
            />

            <FormControlLabel
              control={<Switch checked={debugMode} onChange={(e) => setDebugMode(e.target.checked)} />}
              label={
                <Box>
                  <Typography variant="body2">Debug Mode</Typography>
                  <Typography variant="caption" color="text.secondary">
                    Enable detailed logging for this node
                  </Typography>
                </Box>
              }
            />

            <Divider />

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Relation Types
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1 }}>
                {component?.configurationDescriptor?.nodeDefinition.relationTypes.map((relation) => (
                  <Chip key={relation} label={relation} size="small" color="primary" variant="outlined" />
                ))}
                {component?.configurationDescriptor?.nodeDefinition.customRelations && (
                  <Chip label="Custom Relations" size="small" color="secondary" variant="outlined" />
                )}
              </Box>
            </Box>
          </Box>
        </TabPanel>

        {/* Configuration Tab */}
        <TabPanel value={activeTab} index={1}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Alert severity="info" sx={{ mb: 2 }}>
              Configure node-specific parameters below. These settings control how the node processes messages.
            </Alert>

            {/* Render configuration fields based on node type */}
            {Object.entries(defaultConfig).map(([key, defaultValue]) => {
              const value = configuration[key] !== undefined ? configuration[key] : defaultValue

              // Handle different value types
              if (typeof defaultValue === 'boolean') {
                return (
                  <FormControlLabel
                    key={key}
                    control={
                      <Switch
                        checked={value}
                        onChange={(e) => handleConfigChange(key, e.target.checked)}
                      />
                    }
                    label={key.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase())}
                  />
                )
              }

              if (typeof defaultValue === 'number') {
                return (
                  <TextField
                    key={key}
                    label={key.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase())}
                    type="number"
                    value={value}
                    onChange={(e) => handleConfigChange(key, Number(e.target.value))}
                    fullWidth
                  />
                )
              }

              // Handle script fields with multiline
              if (key.toLowerCase().includes('script')) {
                return (
                  <TextField
                    key={key}
                    label={key.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase())}
                    value={value}
                    onChange={(e) => handleConfigChange(key, e.target.value)}
                    multiline
                    rows={8}
                    fullWidth
                    InputProps={{
                      sx: {
                        fontFamily: 'monospace',
                        fontSize: '0.875rem',
                      },
                    }}
                    helperText="JavaScript code for message processing"
                  />
                )
              }

              // Default to text field
              return (
                <TextField
                  key={key}
                  label={key.replace(/([A-Z])/g, ' $1').replace(/^./, (str) => str.toUpperCase())}
                  value={typeof value === 'object' ? JSON.stringify(value, null, 2) : value}
                  onChange={(e) => {
                    try {
                      const parsed = JSON.parse(e.target.value)
                      handleConfigChange(key, parsed)
                    } catch {
                      handleConfigChange(key, e.target.value)
                    }
                  }}
                  multiline={typeof value === 'object'}
                  rows={typeof value === 'object' ? 4 : 1}
                  fullWidth
                  helperText={
                    typeof value === 'object' ? 'JSON configuration object' : 'Configuration value'
                  }
                />
              )
            })}

            {Object.keys(defaultConfig).length === 0 && (
              <Alert severity="info">This node has no configurable parameters.</Alert>
            )}
          </Box>
        </TabPanel>

        {/* Info Tab */}
        <TabPanel value={activeTab} index={2}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Description
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {component?.configurationDescriptor?.nodeDefinition.description || 'No description available'}
              </Typography>
            </Box>

            <Divider />

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Details
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {component?.configurationDescriptor?.nodeDefinition.details || 'No details available'}
              </Typography>
            </Box>

            <Divider />

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Node Class
              </Typography>
              <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>
                {component?.clazz}
              </Typography>
            </Box>

            <Divider />

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Input/Output
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, mt: 1 }}>
                <Chip
                  label={
                    component?.configurationDescriptor?.nodeDefinition.inEnabled
                      ? 'Input Enabled'
                      : 'Input Disabled'
                  }
                  size="small"
                  color={component?.configurationDescriptor?.nodeDefinition.inEnabled ? 'success' : 'default'}
                />
                <Chip
                  label={
                    component?.configurationDescriptor?.nodeDefinition.outEnabled
                      ? 'Output Enabled'
                      : 'Output Disabled'
                  }
                  size="small"
                  color={
                    component?.configurationDescriptor?.nodeDefinition.outEnabled ? 'success' : 'default'
                  }
                />
              </Box>
            </Box>

            {component?.configurationDescriptor?.nodeDefinition.docUrl && (
              <>
                <Divider />
                <Box>
                  <Button
                    href={component.configurationDescriptor.nodeDefinition.docUrl}
                    target="_blank"
                    variant="outlined"
                    size="small"
                  >
                    View Documentation
                  </Button>
                </Box>
              </>
            )}
          </Box>
        </TabPanel>
      </DialogContent>

      <Divider />

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSave} variant="contained" color="primary">
          Save Configuration
        </Button>
      </DialogActions>
    </Dialog>
  )
}
