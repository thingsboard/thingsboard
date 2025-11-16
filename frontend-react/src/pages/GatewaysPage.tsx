/**
 * Gateways Page
 * Gateway management with connectors and remote configuration
 * Matches ThingsBoard ui-ngx gateways functionality
 */

import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Chip,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Typography,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Switch,
} from '@mui/material'
import {
  Visibility,
  CheckCircle,
  Cancel,
  Settings,
  Code,
  Storage,
  Refresh,
  CloudUpload,
  Terminal,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'
import GatewayDetailsDrawer from '@/components/drawers/GatewayDetailsDrawer'

interface Gateway {
  id: string
  name: string
  label?: string
  type: string
  gatewayProfileId?: string
  gatewayProfileName?: string
  customerId?: string
  customerTitle?: string
  active?: boolean
  connected?: boolean
  createdTime: number
  lastActivityTime?: number
  additionalInfo?: any
}

interface Connector {
  name: string
  type: 'mqtt' | 'modbus' | 'opcua' | 'ble' | 'request' | 'can' | 'bacnet' | 'odbc' | 'rest'
  enabled: boolean
  configuration: any
}

const GATEWAY_TYPES = [
  'Default Gateway',
  'IoT Gateway',
  'Modbus Gateway',
  'OPC-UA Gateway',
  'MQTT Gateway',
  'BLE Gateway',
]

const CONNECTOR_TYPES = [
  { value: 'mqtt', label: 'MQTT' },
  { value: 'modbus', label: 'Modbus' },
  { value: 'opcua', label: 'OPC-UA' },
  { value: 'ble', label: 'BLE' },
  { value: 'request', label: 'Request' },
  { value: 'can', label: 'CAN' },
  { value: 'bacnet', label: 'BACnet' },
  { value: 'odbc', label: 'ODBC' },
  { value: 'rest', label: 'REST' },
]

export default function GatewaysPage() {
  const navigate = useNavigate()
  const [gateways, setGateways] = useState<Gateway[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Dialog states
  const [openDialog, setOpenDialog] = useState(false)
  const [editingGateway, setEditingGateway] = useState<Gateway | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    label: '',
    type: 'Default Gateway',
    gatewayProfileId: '',
  })

  // Configuration dialog
  const [openConfigDialog, setOpenConfigDialog] = useState(false)
  const [selectedGateway, setSelectedGateway] = useState<Gateway | null>(null)
  const [configTab, setConfigTab] = useState(0)
  const [connectors, setConnectors] = useState<Connector[]>([])

  // Drawer states
  const [openDrawer, setOpenDrawer] = useState(false)
  const [drawerGateway, setDrawerGateway] = useState<Gateway | null>(null)
  const [drawerMode, setDrawerMode] = useState<'view' | 'edit' | 'create'>('view')

  useEffect(() => {
    loadGateways()
  }, [page, pageSize, searchQuery])

  const loadGateways = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockGateways: Gateway[] = [
      {
        id: '1',
        name: 'Main Gateway',
        label: 'Building A Gateway',
        type: 'IoT Gateway',
        gatewayProfileName: 'Default',
        customerTitle: 'Customer A',
        active: true,
        connected: true,
        createdTime: Date.now() - 86400000 * 30,
        lastActivityTime: Date.now() - 60000,
      },
      {
        id: '2',
        name: 'Modbus Gateway 001',
        label: 'Factory Floor',
        type: 'Modbus Gateway',
        gatewayProfileName: 'Modbus Profile',
        customerTitle: 'Customer A',
        active: true,
        connected: true,
        createdTime: Date.now() - 86400000 * 25,
        lastActivityTime: Date.now() - 300000,
      },
      {
        id: '3',
        name: 'OPC-UA Gateway',
        label: 'SCADA Integration',
        type: 'OPC-UA Gateway',
        gatewayProfileName: 'OPC-UA Profile',
        customerTitle: 'Customer B',
        active: true,
        connected: false,
        createdTime: Date.now() - 86400000 * 20,
        lastActivityTime: Date.now() - 3600000,
      },
      {
        id: '4',
        name: 'MQTT Gateway',
        label: 'Edge Computing',
        type: 'MQTT Gateway',
        gatewayProfileName: 'MQTT Profile',
        active: true,
        connected: true,
        createdTime: Date.now() - 86400000 * 15,
        lastActivityTime: Date.now() - 120000,
      },
      {
        id: '5',
        name: 'Inactive Gateway',
        label: 'Warehouse B',
        type: 'Default Gateway',
        gatewayProfileName: 'Default',
        customerTitle: 'Customer C',
        active: false,
        connected: false,
        createdTime: Date.now() - 86400000 * 10,
        lastActivityTime: Date.now() - 86400000 * 5,
      },
    ]

    // Filter by search query
    const filtered = searchQuery
      ? mockGateways.filter(
          (g) =>
            g.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            g.label?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            g.type.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : mockGateways

    setGateways(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const loadConnectors = (gateway: Gateway) => {
    // Mock connectors for the selected gateway
    const mockConnectors: Connector[] = [
      {
        name: 'MQTT Connector',
        type: 'mqtt',
        enabled: true,
        configuration: {
          broker: 'localhost:1883',
          clientId: 'tb-gateway',
        },
      },
      {
        name: 'Modbus Connector',
        type: 'modbus',
        enabled: true,
        configuration: {
          host: '192.168.1.100',
          port: 502,
        },
      },
      {
        name: 'OPC-UA Connector',
        type: 'opcua',
        enabled: false,
        configuration: {
          url: 'opc.tcp://localhost:4840',
        },
      },
    ]
    setConnectors(mockConnectors)
  }

  const columns: EntityColumn[] = [
    {
      id: 'name',
      label: 'Name',
      minWidth: 200,
      format: (value, row: Gateway) => (
        <Box>
          <Box
            sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
            onClick={() => navigate(`/gateways/${row.id}`)}
          >
            {value}
          </Box>
          {row.label && (
            <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
              {row.label}
            </Box>
          )}
        </Box>
      ),
    },
    {
      id: 'type',
      label: 'Gateway Type',
      minWidth: 150,
      format: (value) => (
        <Chip
          label={value}
          size="small"
          sx={{
            bgcolor: '#E0F2F1',
            color: '#2E7D6F',
            fontWeight: 500,
          }}
        />
      ),
    },
    {
      id: 'gatewayProfileName',
      label: 'Gateway Profile',
      minWidth: 150,
    },
    {
      id: 'connected',
      label: 'Connection',
      minWidth: 120,
      format: (value: boolean, row: Gateway) =>
        row.active ? (
          value ? (
            <Chip
              icon={<CheckCircle />}
              label="Connected"
              size="small"
              color="success"
              sx={{ fontWeight: 500 }}
            />
          ) : (
            <Chip
              icon={<Cancel />}
              label="Disconnected"
              size="small"
              sx={{ bgcolor: '#C62828', color: 'white', fontWeight: 500 }}
            />
          )
        ) : (
          <Chip
            label="Inactive"
            size="small"
            sx={{ bgcolor: '#8C959D', color: 'white', fontWeight: 500 }}
          />
        ),
    },
    {
      id: 'customerTitle',
      label: 'Customer',
      minWidth: 150,
      format: (value) =>
        value || <Chip label="Unassigned" size="small" variant="outlined" />,
    },
    {
      id: 'lastActivityTime',
      label: 'Last Activity',
      minWidth: 150,
      format: (value?: number) =>
        value ? format(new Date(value), 'MMM dd, yyyy HH:mm:ss') : '-',
    },
  ]

  const handleAdd = () => {
    setDrawerGateway({
      id: '',
      name: '',
      label: '',
      type: 'default',
      active: true,
      connected: false,
      createdTime: Date.now(),
    })
    setDrawerMode('create')
    setOpenDrawer(true)
  }

  const handleEdit = (gateway: Gateway) => {
    setDrawerGateway(gateway)
    setDrawerMode('edit')
    setOpenDrawer(true)
  }

  const handleViewDetails = (gateway: Gateway) => {
    setDrawerGateway(gateway)
    setDrawerMode('view')
    setOpenDrawer(true)
  }

  const handleSaveGateway = (gateway: Gateway) => {
    // API call would go here
    console.log('Saving gateway:', gateway)
    setOpenDrawer(false)
    loadGateways()
  }

  const handleDeleteGateway = (gatewayId: string) => {
    // API call would go here
    console.log('Deleting gateway:', gatewayId)
    setOpenDrawer(false)
    loadGateways()
  }

  const handleDelete = async (ids: string[]) => {
    if (confirm(`Delete ${ids.length} gateway(s)?`)) {
      // API call would go here
      console.log('Deleting gateways:', ids)
      loadGateways()
      setSelectedIds([])
    }
  }

  const handleSave = async () => {
    // API call would go here
    console.log('Saving gateway:', formData)
    setOpenDialog(false)
    loadGateways()
  }

  const handleConfigure = (gateway: Gateway) => {
    setSelectedGateway(gateway)
    loadConnectors(gateway)
    setConfigTab(0)
    setOpenConfigDialog(true)
  }

  const handleRemoteShell = (gateway: Gateway) => {
    // Open remote shell dialog
    console.log('Opening remote shell for:', gateway.id)
  }

  const handleRestartGateway = (gateway: Gateway) => {
    if (confirm(`Restart gateway "${gateway.name}"?`)) {
      // API call would go here
      console.log('Restarting gateway:', gateway.id)
    }
  }

  const handleToggleConnector = (connectorName: string) => {
    setConnectors(
      connectors.map((c) =>
        c.name === connectorName ? { ...c, enabled: !c.enabled } : c
      )
    )
  }

  const rowActions = (row: Gateway) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => navigate(`/gateways/${row.id}`)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Configure">
        <IconButton size="small" onClick={() => handleConfigure(row)}>
          <Settings fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Remote shell">
        <IconButton size="small" onClick={() => handleRemoteShell(row)}>
          <Terminal fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Restart">
        <IconButton size="small" onClick={() => handleRestartGateway(row)}>
          <Refresh fontSize="small" />
        </IconButton>
      </Tooltip>
    </Box>
  )

  return (
    <MainLayout>
      <Box>
        <EntityTable
          title="Gateways"
          columns={columns}
          rows={gateways}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onRefresh={loadGateways}
          onSearch={setSearchQuery}
          onPageChange={(newPage, newPageSize) => {
            setPage(newPage)
            setPageSize(newPageSize)
          }}
          page={page}
          pageSize={pageSize}
          rowActions={rowActions}
        />

        {/* Add/Edit Dialog */}
        <Dialog
          open={openDialog}
          onClose={() => setOpenDialog(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>{editingGateway ? 'Edit Gateway' : 'Add Gateway'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Gateway Name"
                required
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
              <TextField
                label="Label"
                fullWidth
                value={formData.label}
                onChange={(e) => setFormData({ ...formData, label: e.target.value })}
                helperText="Optional gateway label"
              />
              <FormControl fullWidth>
                <InputLabel>Gateway Type</InputLabel>
                <Select
                  value={formData.type}
                  label="Gateway Type"
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                >
                  {GATEWAY_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!formData.name}
              sx={{ bgcolor: '#0F3E5C' }}
            >
              {editingGateway ? 'Save' : 'Add'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Configuration Dialog */}
        <Dialog
          open={openConfigDialog}
          onClose={() => setOpenConfigDialog(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>
            Gateway Configuration - {selectedGateway?.name}
          </DialogTitle>
          <DialogContent>
            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
              <Tabs value={configTab} onChange={(_, v) => setConfigTab(v)}>
                <Tab label="Connectors" />
                <Tab label="General Configuration" />
                <Tab label="Logs" />
              </Tabs>
            </Box>

            {configTab === 0 && (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="h6">Connectors</Typography>
                  <Button
                    variant="contained"
                    size="small"
                    startIcon={<CloudUpload />}
                    sx={{ bgcolor: '#0F3E5C' }}
                  >
                    Add Connector
                  </Button>
                </Box>
                <List>
                  {connectors.map((connector) => (
                    <ListItem
                      key={connector.name}
                      sx={{
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: 1,
                        mb: 1,
                      }}
                    >
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body1" sx={{ fontWeight: 600 }}>
                              {connector.name}
                            </Typography>
                            <Chip
                              label={connector.type.toUpperCase()}
                              size="small"
                              sx={{
                                bgcolor: '#E3F2FD',
                                color: '#0F3E5C',
                                fontWeight: 500,
                              }}
                            />
                          </Box>
                        }
                        secondary={
                          <Typography variant="body2" color="textSecondary">
                            {JSON.stringify(connector.configuration)}
                          </Typography>
                        }
                      />
                      <ListItemSecondaryAction>
                        <Switch
                          checked={connector.enabled}
                          onChange={() => handleToggleConnector(connector.name)}
                        />
                      </ListItemSecondaryAction>
                    </ListItem>
                  ))}
                </List>
              </Box>
            )}

            {configTab === 1 && (
              <Box>
                <Typography variant="body1" color="textSecondary">
                  General gateway configuration (connection settings, security, etc.)
                </Typography>
              </Box>
            )}

            {configTab === 2 && (
              <Box>
                <Typography variant="body1" color="textSecondary">
                  Gateway logs will be displayed here
                </Typography>
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenConfigDialog(false)}>Close</Button>
            <Button
              variant="contained"
              sx={{ bgcolor: '#0F3E5C' }}
              onClick={() => {
                console.log('Saving configuration:', connectors)
                setOpenConfigDialog(false)
              }}
            >
              Save Configuration
            </Button>
          </DialogActions>
        </Dialog>

        {/* Gateway Details Drawer - Right-side slide-in matching ThingsBoard */}
        <GatewayDetailsDrawer
          open={openDrawer}
          onClose={() => setOpenDrawer(false)}
          gateway={drawerGateway}
          onSave={handleSaveGateway}
          onDelete={handleDeleteGateway}
          mode={drawerMode}
        />
      </Box>
    </MainLayout>
  )
}
