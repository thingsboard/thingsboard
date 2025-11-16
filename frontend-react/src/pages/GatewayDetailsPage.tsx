/**
 * Gateway Details Page
 * Gateway configuration with MQTT/Modbus/OPC-UA connectors
 * Matches ThingsBoard gateway configuration exactly
 */

import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  Paper,
  Typography,
  Tabs,
  Tab,
  Button,
  IconButton,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Switch,
  FormControlLabel,
  Divider,
  Tooltip,
  Alert,
} from '@mui/material'
import {
  ArrowBack,
  Add,
  Edit,
  Delete,
  PlayArrow,
  Stop,
  Refresh,
  Cable,
  CheckCircle,
  Error as ErrorIcon,
} from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

interface Connector {
  id: string
  name: string
  type: 'mqtt' | 'modbus' | 'opcua' | 'ble' | 'can' | 'bacnet' | 'rest'
  enabled: boolean
  status: 'connected' | 'disconnected' | 'error'
  configuration: MqttConnectorConfig | ModbusConnectorConfig | OpcuaConnectorConfig
}

interface MqttConnectorConfig {
  broker: string
  port: number
  clientId: string
  username?: string
  password?: string
  qos: 0 | 1 | 2
  cleanSession: boolean
  topicFilters: TopicFilter[]
}

interface TopicFilter {
  topicFilter: string
  converter: DataConverter
}

interface DataConverter {
  type: 'json' | 'bytes' | 'custom'
  deviceNameJsonExpression?: string
  deviceTypeJsonExpression?: string
  timeout?: number
  attributes?: AttributeMapping[]
  timeseries?: TimeseriesMapping[]
}

interface AttributeMapping {
  key: string
  type: 'string' | 'long' | 'double' | 'boolean'
  value: string
}

interface TimeseriesMapping {
  key: string
  type: 'string' | 'long' | 'double' | 'boolean'
  value: string
}

interface ModbusConnectorConfig {
  host: string
  port: number
  unitId: number
  pollPeriod: number
  registers: any[]
}

interface OpcuaConnectorConfig {
  url: string
  securityMode: string
  securityPolicy: string
  mappings: any[]
}

export default function GatewayDetailsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [tabValue, setTabValue] = useState(0)

  // Gateway state
  const [gateway, setGateway] = useState({
    id: id || '1',
    name: 'Main Gateway',
    label: 'Production Floor Gateway',
    type: 'Default Gateway',
    active: true,
    connected: true,
    lastActivityTime: Date.now(),
  })

  // Connectors state
  const [connectors, setConnectors] = useState<Connector[]>([
    {
      id: '1',
      name: 'MQTT Broker Connector',
      type: 'mqtt',
      enabled: true,
      status: 'connected',
      configuration: {
        broker: 'mqtt.example.com',
        port: 1883,
        clientId: 'tb-gateway-001',
        username: 'gateway',
        qos: 1,
        cleanSession: true,
        topicFilters: [
          {
            topicFilter: 'sensors/+/temperature',
            converter: {
              type: 'json',
              deviceNameJsonExpression: '${topic[1]}',
              deviceTypeJsonExpression: 'sensor',
              timeout: 60000,
              attributes: [],
              timeseries: [
                { key: 'temperature', type: 'double', value: '${temperature}' },
                { key: 'humidity', type: 'double', value: '${humidity}' },
              ],
            },
          },
        ],
      } as MqttConnectorConfig,
    },
  ])

  // Dialog states
  const [openConnectorDialog, setOpenConnectorDialog] = useState(false)
  const [editingConnector, setEditingConnector] = useState<Connector | null>(null)
  const [connectorForm, setConnectorForm] = useState<Partial<Connector>>({
    name: '',
    type: 'mqtt',
    enabled: true,
    status: 'disconnected',
  })

  // MQTT Config Dialog
  const [openMqttConfigDialog, setOpenMqttConfigDialog] = useState(false)
  const [mqttConfig, setMqttConfig] = useState<MqttConnectorConfig>({
    broker: '',
    port: 1883,
    clientId: '',
    qos: 1,
    cleanSession: true,
    topicFilters: [],
  })

  // Topic Filter Dialog
  const [openTopicDialog, setOpenTopicDialog] = useState(false)
  const [topicForm, setTopicForm] = useState<TopicFilter>({
    topicFilter: '',
    converter: {
      type: 'json',
      deviceNameJsonExpression: '',
      deviceTypeJsonExpression: '',
      timeout: 60000,
      attributes: [],
      timeseries: [],
    },
  })

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue)
  }

  const handleAddConnector = () => {
    setEditingConnector(null)
    setConnectorForm({
      name: '',
      type: 'mqtt',
      enabled: true,
      status: 'disconnected',
    })
    setOpenConnectorDialog(true)
  }

  const handleEditConnector = (connector: Connector) => {
    setEditingConnector(connector)
    setConnectorForm(connector)
    if (connector.type === 'mqtt') {
      setMqttConfig(connector.configuration as MqttConnectorConfig)
      setOpenMqttConfigDialog(true)
    }
  }

  const handleDeleteConnector = (connectorId: string) => {
    if (confirm('Delete this connector?')) {
      setConnectors(connectors.filter((c) => c.id !== connectorId))
    }
  }

  const handleToggleConnector = (connectorId: string) => {
    setConnectors(
      connectors.map((c) =>
        c.id === connectorId ? { ...c, enabled: !c.enabled } : c
      )
    )
  }

  const handleSaveConnector = () => {
    // Save connector logic
    console.log('Saving connector:', connectorForm)
    setOpenConnectorDialog(false)
  }

  const handleSaveMqttConfig = () => {
    // Save MQTT config logic
    console.log('Saving MQTT config:', mqttConfig)
    setOpenMqttConfigDialog(false)
  }

  const handleAddTopicFilter = () => {
    setTopicForm({
      topicFilter: '',
      converter: {
        type: 'json',
        deviceNameJsonExpression: '',
        deviceTypeJsonExpression: '',
        timeout: 60000,
        attributes: [],
        timeseries: [],
      },
    })
    setOpenTopicDialog(true)
  }

  const handleSaveTopicFilter = () => {
    setMqttConfig({
      ...mqttConfig,
      topicFilters: [...mqttConfig.topicFilters, topicForm],
    })
    setOpenTopicDialog(false)
  }

  const handleAddTimeseries = () => {
    setTopicForm({
      ...topicForm,
      converter: {
        ...topicForm.converter,
        timeseries: [
          ...(topicForm.converter.timeseries || []),
          { key: '', type: 'double', value: '' },
        ],
      },
    })
  }

  const handleAddAttribute = () => {
    setTopicForm({
      ...topicForm,
      converter: {
        ...topicForm.converter,
        attributes: [
          ...(topicForm.converter.attributes || []),
          { key: '', type: 'string', value: '' },
        ],
      },
    })
  }

  const getConnectorIcon = (type: string) => {
    switch (type) {
      case 'mqtt':
        return '📡'
      case 'modbus':
        return '🔌'
      case 'opcua':
        return '🏭'
      case 'ble':
        return '📱'
      case 'can':
        return '🚗'
      default:
        return '🔗'
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected':
        return '#2E7D6F'
      case 'disconnected':
        return '#8C959D'
      case 'error':
        return '#C62828'
      default:
        return '#8C959D'
    }
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <IconButton onClick={() => navigate('/gateways')} sx={{ mr: 2 }}>
            <ArrowBack />
          </IconButton>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h5" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
              {gateway.name}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              {gateway.label}
            </Typography>
          </Box>
          <Chip
            icon={gateway.connected ? <CheckCircle /> : <ErrorIcon />}
            label={gateway.connected ? 'Connected' : 'Disconnected'}
            color={gateway.connected ? 'success' : 'error'}
            sx={{ mr: 2 }}
          />
          <Button variant="contained" startIcon={<Cable />} sx={{ bgcolor: '#0F3E5C' }}>
            Test Connection
          </Button>
        </Box>

        {/* Tabs */}
        <Paper>
          <Tabs value={tabValue} onChange={handleTabChange}>
            <Tab label="Connectors" />
            <Tab label="Configuration" />
            <Tab label="Logs" />
            <Tab label="Statistics" />
          </Tabs>

          {/* Connectors Tab */}
          <TabPanel value={tabValue} index={0}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">Gateway Connectors</Typography>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={handleAddConnector}
                sx={{ bgcolor: '#0F3E5C' }}
              >
                Add Connector
              </Button>
            </Box>

            <Alert severity="info" sx={{ mb: 2 }}>
              Connectors allow the gateway to communicate with different protocols (MQTT, Modbus,
              OPC-UA, etc.) and forward data to ThingsBoard.
            </Alert>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Type</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Enabled</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {connectors.map((connector) => (
                    <TableRow key={connector.id}>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography sx={{ fontSize: '1.5rem' }}>
                            {getConnectorIcon(connector.type)}
                          </Typography>
                          <Typography sx={{ textTransform: 'uppercase', fontWeight: 600 }}>
                            {connector.type}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>{connector.name}</TableCell>
                      <TableCell>
                        <Chip
                          label={connector.status}
                          size="small"
                          sx={{
                            bgcolor: getStatusColor(connector.status),
                            color: 'white',
                            textTransform: 'capitalize',
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Switch
                          checked={connector.enabled}
                          onChange={() => handleToggleConnector(connector.id)}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <Tooltip title="Configure">
                            <IconButton
                              size="small"
                              onClick={() => handleEditConnector(connector)}
                            >
                              <Edit fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Restart">
                            <IconButton size="small">
                              <Refresh fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              onClick={() => handleDeleteConnector(connector.id)}
                              color="error"
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </TabPanel>

          {/* Configuration Tab */}
          <TabPanel value={tabValue} index={1}>
            <Typography variant="h6" gutterBottom>
              Gateway Configuration
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 600 }}>
              <TextField label="Gateway Name" value={gateway.name} fullWidth />
              <TextField label="Gateway Label" value={gateway.label} fullWidth />
              <FormControlLabel
                control={<Switch checked={gateway.active} />}
                label="Active"
              />
            </Box>
          </TabPanel>

          {/* Logs Tab */}
          <TabPanel value={tabValue} index={2}>
            <Typography variant="h6" gutterBottom>
              Gateway Logs
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Real-time gateway logs will appear here.
            </Typography>
          </TabPanel>

          {/* Statistics Tab */}
          <TabPanel value={tabValue} index={3}>
            <Typography variant="h6" gutterBottom>
              Gateway Statistics
            </Typography>
            <Typography variant="body2" color="textSecondary">
              Messages processed, errors, uptime, etc.
            </Typography>
          </TabPanel>
        </Paper>

        {/* MQTT Configuration Dialog */}
        <Dialog open={openMqttConfigDialog} onClose={() => setOpenMqttConfigDialog(false)} maxWidth="md" fullWidth>
          <DialogTitle>MQTT Connector Configuration</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                Broker Settings
              </Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 2 }}>
                <TextField
                  label="Broker Host"
                  value={mqttConfig.broker}
                  onChange={(e) => setMqttConfig({ ...mqttConfig, broker: e.target.value })}
                  placeholder="mqtt.example.com"
                  fullWidth
                />
                <TextField
                  label="Port"
                  type="number"
                  value={mqttConfig.port}
                  onChange={(e) =>
                    setMqttConfig({ ...mqttConfig, port: parseInt(e.target.value) })
                  }
                  fullWidth
                />
              </Box>

              <TextField
                label="Client ID"
                value={mqttConfig.clientId}
                onChange={(e) => setMqttConfig({ ...mqttConfig, clientId: e.target.value })}
                placeholder="tb-gateway-001"
                fullWidth
              />

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  label="Username (optional)"
                  value={mqttConfig.username || ''}
                  onChange={(e) => setMqttConfig({ ...mqttConfig, username: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Password (optional)"
                  type="password"
                  value={mqttConfig.password || ''}
                  onChange={(e) => setMqttConfig({ ...mqttConfig, password: e.target.value })}
                  fullWidth
                />
              </Box>

              <FormControl fullWidth>
                <InputLabel>QoS Level</InputLabel>
                <Select
                  value={mqttConfig.qos}
                  onChange={(e) =>
                    setMqttConfig({ ...mqttConfig, qos: e.target.value as 0 | 1 | 2 })
                  }
                >
                  <MenuItem value={0}>0 - At most once</MenuItem>
                  <MenuItem value={1}>1 - At least once</MenuItem>
                  <MenuItem value={2}>2 - Exactly once</MenuItem>
                </Select>
              </FormControl>

              <FormControlLabel
                control={
                  <Switch
                    checked={mqttConfig.cleanSession}
                    onChange={(e) =>
                      setMqttConfig({ ...mqttConfig, cleanSession: e.target.checked })
                    }
                  />
                }
                label="Clean Session"
              />

              <Divider />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                  Topic Filters & Data Converters
                </Typography>
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<Add />}
                  onClick={handleAddTopicFilter}
                >
                  Add Topic Filter
                </Button>
              </Box>

              {mqttConfig.topicFilters.map((filter, index) => (
                <Paper key={index} sx={{ p: 2, bgcolor: '#F5F5F5' }}>
                  <Typography variant="body2" sx={{ fontWeight: 600, mb: 1 }}>
                    Topic: {filter.topicFilter}
                  </Typography>
                  <Typography variant="caption" color="textSecondary">
                    Converter: {filter.converter.type.toUpperCase()} | Timeseries:{' '}
                    {filter.converter.timeseries?.length || 0} | Attributes:{' '}
                    {filter.converter.attributes?.length || 0}
                  </Typography>
                </Paper>
              ))}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenMqttConfigDialog(false)}>Cancel</Button>
            <Button variant="contained" onClick={handleSaveMqttConfig} sx={{ bgcolor: '#0F3E5C' }}>
              Save Configuration
            </Button>
          </DialogActions>
        </Dialog>

        {/* Topic Filter Dialog */}
        <Dialog open={openTopicDialog} onClose={() => setOpenTopicDialog(false)} maxWidth="md" fullWidth>
          <DialogTitle>Configure Topic Filter & Data Converter</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <Alert severity="info">
                Define how to extract device name, attributes, and telemetry from MQTT messages.
              </Alert>

              <TextField
                label="Topic Filter"
                value={topicForm.topicFilter}
                onChange={(e) =>
                  setTopicForm({ ...topicForm, topicFilter: e.target.value })
                }
                placeholder="sensors/+/temperature"
                helperText="Use + for single-level wildcard, # for multi-level wildcard"
                fullWidth
              />

              <Divider />

              <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                Data Converter
              </Typography>

              <FormControl fullWidth>
                <InputLabel>Converter Type</InputLabel>
                <Select
                  value={topicForm.converter.type}
                  onChange={(e) =>
                    setTopicForm({
                      ...topicForm,
                      converter: { ...topicForm.converter, type: e.target.value as any },
                    })
                  }
                >
                  <MenuItem value="json">JSON</MenuItem>
                  <MenuItem value="bytes">Bytes</MenuItem>
                  <MenuItem value="custom">Custom</MenuItem>
                </Select>
              </FormControl>

              <TextField
                label="Device Name Expression"
                value={topicForm.converter.deviceNameJsonExpression || ''}
                onChange={(e) =>
                  setTopicForm({
                    ...topicForm,
                    converter: {
                      ...topicForm.converter,
                      deviceNameJsonExpression: e.target.value,
                    },
                  })
                }
                placeholder="${topic[1]} or ${deviceName}"
                helperText="JSONPath expression to extract device name"
                fullWidth
              />

              <TextField
                label="Device Type Expression"
                value={topicForm.converter.deviceTypeJsonExpression || ''}
                onChange={(e) =>
                  setTopicForm({
                    ...topicForm,
                    converter: {
                      ...topicForm.converter,
                      deviceTypeJsonExpression: e.target.value,
                    },
                  })
                }
                placeholder="sensor"
                helperText="JSONPath expression to extract device type"
                fullWidth
              />

              <Divider />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                  Timeseries Mappings
                </Typography>
                <Button variant="outlined" size="small" startIcon={<Add />} onClick={handleAddTimeseries}>
                  Add Timeseries
                </Button>
              </Box>

              {topicForm.converter.timeseries?.map((ts, index) => (
                <Box key={index} sx={{ display: 'grid', gridTemplateColumns: '2fr 1fr 2fr auto', gap: 1 }}>
                  <TextField
                    label="Key"
                    value={ts.key}
                    onChange={(e) => {
                      const newTimeseries = [...(topicForm.converter.timeseries || [])]
                      newTimeseries[index].key = e.target.value
                      setTopicForm({
                        ...topicForm,
                        converter: { ...topicForm.converter, timeseries: newTimeseries },
                      })
                    }}
                    placeholder="temperature"
                    size="small"
                  />
                  <FormControl size="small">
                    <InputLabel>Type</InputLabel>
                    <Select
                      value={ts.type}
                      onChange={(e) => {
                        const newTimeseries = [...(topicForm.converter.timeseries || [])]
                        newTimeseries[index].type = e.target.value as any
                        setTopicForm({
                          ...topicForm,
                          converter: { ...topicForm.converter, timeseries: newTimeseries },
                        })
                      }}
                    >
                      <MenuItem value="double">Double</MenuItem>
                      <MenuItem value="long">Long</MenuItem>
                      <MenuItem value="string">String</MenuItem>
                      <MenuItem value="boolean">Boolean</MenuItem>
                    </Select>
                  </FormControl>
                  <TextField
                    label="Value Expression"
                    value={ts.value}
                    onChange={(e) => {
                      const newTimeseries = [...(topicForm.converter.timeseries || [])]
                      newTimeseries[index].value = e.target.value
                      setTopicForm({
                        ...topicForm,
                        converter: { ...topicForm.converter, timeseries: newTimeseries },
                      })
                    }}
                    placeholder="${temperature}"
                    size="small"
                  />
                  <IconButton
                    size="small"
                    onClick={() => {
                      const newTimeseries = topicForm.converter.timeseries?.filter((_, i) => i !== index)
                      setTopicForm({
                        ...topicForm,
                        converter: { ...topicForm.converter, timeseries: newTimeseries },
                      })
                    }}
                  >
                    <Delete fontSize="small" />
                  </IconButton>
                </Box>
              ))}

              <Divider />

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                  Attribute Mappings
                </Typography>
                <Button variant="outlined" size="small" startIcon={<Add />} onClick={handleAddAttribute}>
                  Add Attribute
                </Button>
              </Box>

              {topicForm.converter.attributes?.map((attr, index) => (
                <Box key={index} sx={{ display: 'grid', gridTemplateColumns: '2fr 1fr 2fr auto', gap: 1 }}>
                  <TextField
                    label="Key"
                    value={attr.key}
                    onChange={(e) => {
                      const newAttributes = [...(topicForm.converter.attributes || [])]
                      newAttributes[index].key = e.target.value
                      setTopicForm({
                        ...topicForm,
                        converter: { ...topicForm.converter, attributes: newAttributes },
                      })
                    }}
                    placeholder="model"
                    size="small"
                  />
                  <FormControl size="small">
                    <InputLabel>Type</InputLabel>
                    <Select
                      value={attr.type}
                      onChange={(e) => {
                        const newAttributes = [...(topicForm.converter.attributes || [])]
                        newAttributes[index].type = e.target.value as any
                        setTopicForm({
                          ...topicForm,
                          converter: { ...topicForm.converter, attributes: newAttributes },
                        })
                      }}
                    >
                      <MenuItem value="string">String</MenuItem>
                      <MenuItem value="long">Long</MenuItem>
                      <MenuItem value="double">Double</MenuItem>
                      <MenuItem value="boolean">Boolean</MenuItem>
                    </Select>
                  </FormControl>
                  <TextField
                    label="Value Expression"
                    value={attr.value}
                    onChange={(e) => {
                      const newAttributes = [...(topicForm.converter.attributes || [])]
                      newAttributes[index].value = e.target.value
                      setTopicForm({
                        ...topicForm,
                        converter: { ...topicForm.converter, attributes: newAttributes },
                      })
                    }}
                    placeholder="${model}"
                    size="small"
                  />
                  <IconButton
                    size="small"
                    onClick={() => {
                      const newAttributes = topicForm.converter.attributes?.filter((_, i) => i !== index)
                      setTopicForm({
                        ...topicForm,
                        converter: { ...topicForm.converter, attributes: newAttributes },
                      })
                    }}
                  >
                    <Delete fontSize="small" />
                  </IconButton>
                </Box>
              ))}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenTopicDialog(false)}>Cancel</Button>
            <Button variant="contained" onClick={handleSaveTopicFilter} sx={{ bgcolor: '#0F3E5C' }}>
              Save Topic Filter
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </MainLayout>
  )
}
