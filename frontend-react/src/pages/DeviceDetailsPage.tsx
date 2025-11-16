/**
 * Device Details Page
 * Comprehensive device view with tabs for attributes, telemetry, alarms, etc.
 * Matches ThingsBoard ui-ngx device details functionality
 */

import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  Paper,
  Tabs,
  Tab,
  Typography,
  IconButton,
  Tooltip,
  Breadcrumbs,
  Link,
  Chip,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Card,
  CardContent,
  Grid,
} from '@mui/material'
import {
  ArrowBack,
  Edit,
  Delete,
  CheckCircle,
  Cancel,
  Add,
  Refresh,
  Save,
} from '@mui/icons-material'
import { format } from 'date-fns'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer } from 'recharts'
import MainLayout from '@/components/layout/MainLayout'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`device-tabpanel-${index}`}
      aria-labelledby={`device-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

interface Device {
  id: string
  name: string
  label?: string
  type: string
  deviceProfileId?: string
  deviceProfileName?: string
  customerId?: string
  customerTitle?: string
  active?: boolean
  createdTime: number
  additionalInfo?: any
}

interface Attribute {
  key: string
  value: any
  lastUpdateTs: number
  scope: 'SERVER_SCOPE' | 'SHARED_SCOPE' | 'CLIENT_SCOPE'
}

interface TelemetryData {
  ts: number
  value: number
}

interface Telemetry {
  key: string
  latestValue: any
  latestTs: number
  data: TelemetryData[]
}

export default function DeviceDetailsPage() {
  const { deviceId } = useParams<{ deviceId: string }>()
  const navigate = useNavigate()
  const [device, setDevice] = useState<Device | null>(null)
  const [activeTab, setActiveTab] = useState(0)
  const [loading, setLoading] = useState(false)

  // Attributes state
  const [attributes, setAttributes] = useState<Attribute[]>([])
  const [attributeScope, setAttributeScope] = useState<Attribute['scope']>('SERVER_SCOPE')
  const [openAttributeDialog, setOpenAttributeDialog] = useState(false)
  const [newAttribute, setNewAttribute] = useState({ key: '', value: '' })

  // Telemetry state
  const [telemetry, setTelemetry] = useState<Telemetry[]>([])
  const [selectedTelemetryKeys, setSelectedTelemetryKeys] = useState<string[]>([])

  useEffect(() => {
    loadDevice()
    loadAttributes()
    loadTelemetry()
  }, [deviceId])

  const loadDevice = async () => {
    setLoading(true)
    // Mock data - in production, this would be an API call
    const mockDevice: Device = {
      id: deviceId || '1',
      name: 'Temperature Sensor 001',
      label: 'Building A - Room 101',
      type: 'temperature_sensor',
      deviceProfileName: 'Default',
      customerTitle: 'Customer A',
      active: true,
      createdTime: Date.now() - 86400000 * 30,
      additionalInfo: {
        description: 'Temperature sensor in main building',
        gateway: false,
      },
    }
    setDevice(mockDevice)
    setLoading(false)
  }

  const loadAttributes = async () => {
    // Mock attributes
    const mockAttributes: Attribute[] = [
      {
        key: 'firmwareVersion',
        value: '1.2.3',
        lastUpdateTs: Date.now() - 86400000,
        scope: 'SERVER_SCOPE',
      },
      {
        key: 'serialNumber',
        value: 'SN-12345678',
        lastUpdateTs: Date.now() - 86400000 * 30,
        scope: 'SERVER_SCOPE',
      },
      {
        key: 'updateInterval',
        value: 60,
        lastUpdateTs: Date.now() - 3600000,
        scope: 'SHARED_SCOPE',
      },
      {
        key: 'targetTemperature',
        value: 22,
        lastUpdateTs: Date.now() - 7200000,
        scope: 'SHARED_SCOPE',
      },
      {
        key: 'batteryLevel',
        value: 87,
        lastUpdateTs: Date.now() - 1800000,
        scope: 'CLIENT_SCOPE',
      },
      {
        key: 'rssi',
        value: -65,
        lastUpdateTs: Date.now() - 1800000,
        scope: 'CLIENT_SCOPE',
      },
    ]
    setAttributes(mockAttributes)
  }

  const loadTelemetry = async () => {
    // Mock telemetry data
    const now = Date.now()
    const mockTelemetry: Telemetry[] = [
      {
        key: 'temperature',
        latestValue: 23.5,
        latestTs: now,
        data: Array.from({ length: 24 }, (_, i) => ({
          ts: now - (23 - i) * 3600000,
          value: 20 + Math.random() * 10,
        })),
      },
      {
        key: 'humidity',
        latestValue: 65,
        latestTs: now,
        data: Array.from({ length: 24 }, (_, i) => ({
          ts: now - (23 - i) * 3600000,
          value: 50 + Math.random() * 30,
        })),
      },
      {
        key: 'pressure',
        latestValue: 1013,
        latestTs: now,
        data: Array.from({ length: 24 }, (_, i) => ({
          ts: now - (23 - i) * 3600000,
          value: 1000 + Math.random() * 30,
        })),
      },
    ]
    setTelemetry(mockTelemetry)
    setSelectedTelemetryKeys(['temperature', 'humidity'])
  }

  const handleAddAttribute = () => {
    // API call would go here
    console.log('Adding attribute:', newAttribute, 'to scope:', attributeScope)
    setOpenAttributeDialog(false)
    setNewAttribute({ key: '', value: '' })
    loadAttributes()
  }

  const handleDeleteAttribute = (attribute: Attribute) => {
    if (confirm(`Delete attribute "${attribute.key}"?`)) {
      // API call would go here
      console.log('Deleting attribute:', attribute)
      loadAttributes()
    }
  }

  const filteredAttributes = attributes.filter((attr) => attr.scope === attributeScope)

  const chartData = selectedTelemetryKeys.length > 0
    ? telemetry
        .find((t) => t.key === selectedTelemetryKeys[0])
        ?.data.map((point) => {
          const dataPoint: any = {
            time: format(new Date(point.ts), 'HH:mm'),
            timestamp: point.ts,
          }
          selectedTelemetryKeys.forEach((key) => {
            const telemetryItem = telemetry.find((t) => t.key === key)
            const valueAtTs = telemetryItem?.data.find((d) => d.ts === point.ts)
            if (valueAtTs) {
              dataPoint[key] = valueAtTs.value
            }
          })
          return dataPoint
        }) || []
    : []

  if (!device) {
    return (
      <MainLayout>
        <Box sx={{ p: 3 }}>
          <Typography>Loading device...</Typography>
        </Box>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        {/* Breadcrumbs */}
        <Breadcrumbs sx={{ mb: 2 }}>
          <Link
            color="inherit"
            sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
            onClick={() => navigate('/devices')}
          >
            Devices
          </Link>
          <Typography color="text.primary">{device.name}</Typography>
        </Breadcrumbs>

        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <IconButton onClick={() => navigate('/devices')} sx={{ mr: 2 }}>
            <ArrowBack />
          </IconButton>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h5" sx={{ fontWeight: 600, color: '#0F3E5C' }}>
              {device.name}
            </Typography>
            {device.label && (
              <Typography variant="body2" color="textSecondary">
                {device.label}
              </Typography>
            )}
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Chip
              label={device.type.replace(/_/g, ' ').toUpperCase()}
              size="small"
              sx={{
                bgcolor: '#E3F2FD',
                color: '#0F3E5C',
                fontWeight: 500,
              }}
            />
            {device.active ? (
              <Chip
                icon={<CheckCircle />}
                label="Active"
                size="small"
                color="success"
                sx={{ fontWeight: 500 }}
              />
            ) : (
              <Chip
                icon={<Cancel />}
                label="Inactive"
                size="small"
                sx={{ bgcolor: '#8C959D', color: 'white', fontWeight: 500 }}
              />
            )}
          </Box>
        </Box>

        {/* Tabs */}
        <Paper>
          <Tabs
            value={activeTab}
            onChange={(_, newValue) => setActiveTab(newValue)}
            sx={{
              borderBottom: 1,
              borderColor: 'divider',
              '& .MuiTab-root': {
                textTransform: 'none',
                fontWeight: 500,
              },
            }}
          >
            <Tab label="Details" />
            <Tab label="Attributes" />
            <Tab label="Latest Telemetry" />
            <Tab label="Timeseries" />
            <Tab label="Alarms" />
            <Tab label="Events" />
            <Tab label="Relations" />
          </Tabs>

          {/* Details Tab */}
          <TabPanel value={activeTab} index={0}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                      Basic Information
                    </Typography>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Device Name
                        </Typography>
                        <Typography variant="body1">{device.name}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Label
                        </Typography>
                        <Typography variant="body1">{device.label || '-'}</Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Device Type
                        </Typography>
                        <Typography variant="body1">
                          {device.type.replace(/_/g, ' ').toUpperCase()}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Device Profile
                        </Typography>
                        <Typography variant="body1">
                          {device.deviceProfileName || 'Default'}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                      System Information
                    </Typography>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Device ID
                        </Typography>
                        <Typography
                          variant="body2"
                          sx={{ fontFamily: 'monospace', fontSize: '0.875rem' }}
                        >
                          {device.id}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Customer
                        </Typography>
                        <Typography variant="body1">
                          {device.customerTitle || 'Unassigned'}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Created Time
                        </Typography>
                        <Typography variant="body1">
                          {format(new Date(device.createdTime), 'MMM dd, yyyy HH:mm:ss')}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="subtitle2" color="textSecondary">
                          Status
                        </Typography>
                        <Typography variant="body1">
                          {device.active ? 'Active' : 'Inactive'}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </TabPanel>

          {/* Attributes Tab */}
          <TabPanel value={activeTab} index={1}>
            <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Attribute Scope</InputLabel>
                <Select
                  value={attributeScope}
                  label="Attribute Scope"
                  onChange={(e) => setAttributeScope(e.target.value as Attribute['scope'])}
                >
                  <MenuItem value="SERVER_SCOPE">Server Attributes</MenuItem>
                  <MenuItem value="SHARED_SCOPE">Shared Attributes</MenuItem>
                  <MenuItem value="CLIENT_SCOPE">Client Attributes</MenuItem>
                </Select>
              </FormControl>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={() => setOpenAttributeDialog(true)}
                sx={{ bgcolor: '#0F3E5C' }}
              >
                Add Attribute
              </Button>
            </Box>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Key</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Value</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Last Update</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>
                      Actions
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredAttributes.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="textSecondary">No attributes found</Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredAttributes.map((attr) => (
                      <TableRow key={attr.key}>
                        <TableCell sx={{ fontFamily: 'monospace' }}>{attr.key}</TableCell>
                        <TableCell>
                          {typeof attr.value === 'object'
                            ? JSON.stringify(attr.value)
                            : String(attr.value)}
                        </TableCell>
                        <TableCell>
                          {format(new Date(attr.lastUpdateTs), 'MMM dd, yyyy HH:mm:ss')}
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              onClick={() => handleDeleteAttribute(attr)}
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </TabPanel>

          {/* Latest Telemetry Tab */}
          <TabPanel value={activeTab} index={2}>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Key</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Latest Value</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Last Update</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {telemetry.map((item) => (
                    <TableRow key={item.key}>
                      <TableCell sx={{ fontFamily: 'monospace' }}>{item.key}</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#0F3E5C' }}>
                        {typeof item.latestValue === 'number'
                          ? item.latestValue.toFixed(2)
                          : String(item.latestValue)}
                      </TableCell>
                      <TableCell>
                        {format(new Date(item.latestTs), 'MMM dd, yyyy HH:mm:ss')}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </TabPanel>

          {/* Timeseries Tab */}
          <TabPanel value={activeTab} index={3}>
            <Box sx={{ mb: 2 }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                Select telemetry keys to display:
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {telemetry.map((item) => (
                  <Chip
                    key={item.key}
                    label={item.key}
                    onClick={() => {
                      if (selectedTelemetryKeys.includes(item.key)) {
                        setSelectedTelemetryKeys(
                          selectedTelemetryKeys.filter((k) => k !== item.key)
                        )
                      } else {
                        setSelectedTelemetryKeys([...selectedTelemetryKeys, item.key])
                      }
                    }}
                    color={selectedTelemetryKeys.includes(item.key) ? 'primary' : 'default'}
                    sx={{ cursor: 'pointer' }}
                  />
                ))}
              </Box>
            </Box>

            {selectedTelemetryKeys.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <RechartsTooltip />
                  <Legend />
                  {selectedTelemetryKeys.map((key, index) => (
                    <Line
                      key={key}
                      type="monotone"
                      dataKey={key}
                      stroke={['#0F3E5C', '#FFB300', '#2E7D6F', '#C62828'][index % 4]}
                      strokeWidth={2}
                      dot={false}
                    />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Box
                sx={{
                  height: 400,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Typography color="textSecondary">
                  Select telemetry keys to display chart
                </Typography>
              </Box>
            )}
          </TabPanel>

          {/* Alarms Tab */}
          <TabPanel value={activeTab} index={4}>
            <Typography color="textSecondary">
              Device-specific alarms will be displayed here
            </Typography>
          </TabPanel>

          {/* Events Tab */}
          <TabPanel value={activeTab} index={5}>
            <Typography color="textSecondary">
              Device lifecycle events will be displayed here
            </Typography>
          </TabPanel>

          {/* Relations Tab */}
          <TabPanel value={activeTab} index={6}>
            <Typography color="textSecondary">
              Device relations will be displayed here
            </Typography>
          </TabPanel>
        </Paper>

        {/* Add Attribute Dialog */}
        <Dialog
          open={openAttributeDialog}
          onClose={() => setOpenAttributeDialog(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>Add Attribute</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Key"
                required
                fullWidth
                value={newAttribute.key}
                onChange={(e) => setNewAttribute({ ...newAttribute, key: e.target.value })}
              />
              <TextField
                label="Value"
                required
                fullWidth
                value={newAttribute.value}
                onChange={(e) => setNewAttribute({ ...newAttribute, value: e.target.value })}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenAttributeDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleAddAttribute}
              disabled={!newAttribute.key || !newAttribute.value}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              Add
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </MainLayout>
  )
}
