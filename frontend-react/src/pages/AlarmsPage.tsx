/**
 * Alarms Page
 * Alarm management with filtering, acknowledgment, and clearing
 * Matches ThingsBoard ui-ngx alarms functionality
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
  SelectChangeEvent,
  Typography,
  Divider,
  Stack,
} from '@mui/material'
import {
  CheckCircle,
  Cancel,
  Visibility,
  Done,
  DoneAll,
  Clear,
  FilterList,
} from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'

type AlarmSeverity = 'CRITICAL' | 'MAJOR' | 'MINOR' | 'WARNING' | 'INDETERMINATE'
type AlarmStatus = 'ACTIVE_UNACK' | 'ACTIVE_ACK' | 'CLEARED_UNACK' | 'CLEARED_ACK'
type EntityType = 'DEVICE' | 'ASSET' | 'ENTITY_VIEW' | 'CUSTOMER' | 'TENANT' | 'DASHBOARD'

interface AlarmOriginator {
  entityType: EntityType
  id: string
}

interface Alarm {
  id: string
  createdTime: number
  startTs: number
  endTs?: number
  ackTs?: number
  clearTs?: number
  originator: AlarmOriginator
  originatorName?: string
  type: string
  severity: AlarmSeverity
  status: AlarmStatus
  acknowledged: boolean
  cleared: boolean
  details?: any
  propagate?: boolean
  propagateToOwner?: boolean
  propagateToTenant?: boolean
  propagateRelationTypes?: string[]
}

const ALARM_SEVERITIES: AlarmSeverity[] = [
  'CRITICAL',
  'MAJOR',
  'MINOR',
  'WARNING',
  'INDETERMINATE',
]

const ALARM_STATUSES: AlarmStatus[] = [
  'ACTIVE_UNACK',
  'ACTIVE_ACK',
  'CLEARED_UNACK',
  'CLEARED_ACK',
]

const ALARM_TYPES = [
  'High Temperature',
  'Low Temperature',
  'High Humidity',
  'Connection Lost',
  'Battery Low',
  'Sensor Error',
  'Communication Failure',
  'Threshold Violation',
]

const severityConfig = {
  CRITICAL: { label: 'Critical', color: '#FFFFFF', bgcolor: '#C62828' },
  MAJOR: { label: 'Major', color: '#FFFFFF', bgcolor: '#E65100' },
  MINOR: { label: 'Minor', color: '#000000', bgcolor: '#FFB300' },
  WARNING: { label: 'Warning', color: '#000000', bgcolor: '#FDD835' },
  INDETERMINATE: { label: 'Indeterminate', color: '#000000', bgcolor: '#8C959D' },
}

const statusConfig = {
  ACTIVE_UNACK: { label: 'Active Unacknowledged', color: '#C62828', bgcolor: '#FFEBEE' },
  ACTIVE_ACK: { label: 'Active Acknowledged', color: '#E65100', bgcolor: '#FFF3E0' },
  CLEARED_UNACK: { label: 'Cleared Unacknowledged', color: '#0F3E5C', bgcolor: '#E3F2FD' },
  CLEARED_ACK: { label: 'Cleared', color: '#2E7D6F', bgcolor: '#E0F2F1' },
}

export default function AlarmsPage() {
  const navigate = useNavigate()
  const [alarms, setAlarms] = useState<Alarm[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Filter states
  const [severityFilter, setSeverityFilter] = useState<AlarmSeverity | ''>('')
  const [statusFilter, setStatusFilter] = useState<AlarmStatus | ''>('')
  const [typeFilter, setTypeFilter] = useState('')
  const [openFilters, setOpenFilters] = useState(false)

  // Dialog states
  const [openDetails, setOpenDetails] = useState(false)
  const [selectedAlarm, setSelectedAlarm] = useState<Alarm | null>(null)

  // Load alarms (mock data for now)
  useEffect(() => {
    loadAlarms()
  }, [page, pageSize, searchQuery, severityFilter, statusFilter, typeFilter])

  const loadAlarms = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockAlarms: Alarm[] = [
      {
        id: '1',
        createdTime: Date.now() - 3600000,
        startTs: Date.now() - 3600000,
        originator: { entityType: 'DEVICE', id: 'device-1' },
        originatorName: 'Temperature Sensor 001',
        type: 'High Temperature',
        severity: 'CRITICAL',
        status: 'ACTIVE_UNACK',
        acknowledged: false,
        cleared: false,
        details: { threshold: 80, currentValue: 95 },
        propagate: true,
        propagateToOwner: true,
      },
      {
        id: '2',
        createdTime: Date.now() - 7200000,
        startTs: Date.now() - 7200000,
        originator: { entityType: 'DEVICE', id: 'device-2' },
        originatorName: 'Gateway 001',
        type: 'Connection Lost',
        severity: 'MAJOR',
        status: 'ACTIVE_UNACK',
        acknowledged: false,
        cleared: false,
        details: { lastSeen: Date.now() - 7200000 },
      },
      {
        id: '3',
        createdTime: Date.now() - 14400000,
        startTs: Date.now() - 14400000,
        ackTs: Date.now() - 10800000,
        originator: { entityType: 'DEVICE', id: 'device-3' },
        originatorName: 'Humidity Sensor 001',
        type: 'High Humidity',
        severity: 'MINOR',
        status: 'ACTIVE_ACK',
        acknowledged: true,
        cleared: false,
        details: { threshold: 70, currentValue: 75 },
      },
      {
        id: '4',
        createdTime: Date.now() - 86400000,
        startTs: Date.now() - 86400000,
        clearTs: Date.now() - 43200000,
        originator: { entityType: 'DEVICE', id: 'device-4' },
        originatorName: 'Controller 001',
        type: 'Battery Low',
        severity: 'WARNING',
        status: 'CLEARED_ACK',
        acknowledged: true,
        cleared: true,
        details: { batteryLevel: 15 },
      },
      {
        id: '5',
        createdTime: Date.now() - 172800000,
        startTs: Date.now() - 172800000,
        clearTs: Date.now() - 86400000,
        originator: { entityType: 'ASSET', id: 'asset-1' },
        originatorName: 'Production Line 1',
        type: 'Sensor Error',
        severity: 'INDETERMINATE',
        status: 'CLEARED_ACK',
        acknowledged: true,
        cleared: true,
        details: { errorCode: 'ERR_SENSOR_TIMEOUT' },
      },
      {
        id: '6',
        createdTime: Date.now() - 1800000,
        startTs: Date.now() - 1800000,
        originator: { entityType: 'DEVICE', id: 'device-5' },
        originatorName: 'Flow Meter 001',
        type: 'Threshold Violation',
        severity: 'MAJOR',
        status: 'ACTIVE_UNACK',
        acknowledged: false,
        cleared: false,
        details: { metric: 'flow_rate', threshold: 100, currentValue: 125 },
      },
    ]

    // Apply filters
    let filtered = mockAlarms

    if (searchQuery) {
      filtered = filtered.filter(
        (a) =>
          a.originatorName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          a.type.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }

    if (severityFilter) {
      filtered = filtered.filter((a) => a.severity === severityFilter)
    }

    if (statusFilter) {
      filtered = filtered.filter((a) => a.status === statusFilter)
    }

    if (typeFilter) {
      filtered = filtered.filter((a) => a.type === typeFilter)
    }

    setAlarms(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'createdTime',
      label: 'Created Time',
      minWidth: 150,
      format: (value: number) => format(new Date(value), 'MMM dd, yyyy HH:mm:ss'),
    },
    {
      id: 'originatorName',
      label: 'Originator',
      minWidth: 200,
      format: (value, row: Alarm) => (
        <Box>
          <Box
            sx={{ fontWeight: 600, color: '#0F3E5C', cursor: 'pointer' }}
            onClick={() => navigate(`/${row.originator.entityType.toLowerCase()}s/${row.originator.id}`)}
          >
            {value || 'Unknown'}
          </Box>
          <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
            {row.originator.entityType}
          </Box>
        </Box>
      ),
    },
    {
      id: 'type',
      label: 'Alarm Type',
      minWidth: 180,
      format: (value) => (
        <Typography sx={{ fontWeight: 500, color: '#0F3E5C' }}>
          {value}
        </Typography>
      ),
    },
    {
      id: 'severity',
      label: 'Severity',
      minWidth: 130,
      format: (value: AlarmSeverity) => {
        const config = severityConfig[value]
        return (
          <Chip
            label={config.label}
            size="small"
            sx={{
              bgcolor: config.bgcolor,
              color: config.color,
              fontWeight: 600,
            }}
          />
        )
      },
    },
    {
      id: 'status',
      label: 'Status',
      minWidth: 200,
      format: (value: AlarmStatus) => {
        const config = statusConfig[value]
        return (
          <Chip
            label={config.label}
            size="small"
            sx={{
              bgcolor: config.bgcolor,
              color: config.color,
              fontWeight: 500,
            }}
          />
        )
      },
    },
  ]

  const handleAcknowledge = async (alarm: Alarm) => {
    // API call would go here
    console.log('Acknowledging alarm:', alarm.id)
    loadAlarms()
  }

  const handleClear = async (alarm: Alarm) => {
    // API call would go here
    console.log('Clearing alarm:', alarm.id)
    loadAlarms()
  }

  const handleAcknowledgeSelected = async () => {
    if (selectedIds.length > 0) {
      // API call would go here
      console.log('Acknowledging alarms:', selectedIds)
      loadAlarms()
      setSelectedIds([])
    }
  }

  const handleClearSelected = async () => {
    if (selectedIds.length > 0) {
      // API call would go here
      console.log('Clearing alarms:', selectedIds)
      loadAlarms()
      setSelectedIds([])
    }
  }

  const handleViewDetails = (alarm: Alarm) => {
    setSelectedAlarm(alarm)
    setOpenDetails(true)
  }

  const rowActions = (row: Alarm) => (
    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
      <Tooltip title="View details">
        <IconButton size="small" onClick={() => handleViewDetails(row)}>
          <Visibility fontSize="small" />
        </IconButton>
      </Tooltip>
      {!row.acknowledged && (
        <Tooltip title="Acknowledge">
          <IconButton size="small" onClick={() => handleAcknowledge(row)}>
            <Done fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
      {!row.cleared && (
        <Tooltip title="Clear">
          <IconButton size="small" onClick={() => handleClear(row)}>
            <Clear fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
    </Box>
  )

  const customActions = (
    <>
      {selectedIds.length > 0 && (
        <>
          <Tooltip title="Acknowledge selected">
            <IconButton onClick={handleAcknowledgeSelected}>
              <DoneAll />
            </IconButton>
          </Tooltip>
          <Tooltip title="Clear selected">
            <IconButton onClick={handleClearSelected}>
              <Clear />
            </IconButton>
          </Tooltip>
        </>
      )}
      <Tooltip title="Filters">
        <IconButton onClick={() => setOpenFilters(!openFilters)}>
          <FilterList />
        </IconButton>
      </Tooltip>
    </>
  )

  return (
    <MainLayout>
      <Box>
        {/* Filters Panel */}
        {openFilters && (
          <Box
            sx={{
              bgcolor: 'white',
              p: 2,
              mb: 2,
              borderRadius: 1,
              boxShadow: 1,
            }}
          >
            <Typography variant="h6" sx={{ mb: 2 }}>
              Filters
            </Typography>
            <Stack direction="row" spacing={2}>
              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Severity</InputLabel>
                <Select
                  value={severityFilter}
                  label="Severity"
                  onChange={(e) => setSeverityFilter(e.target.value as AlarmSeverity | '')}
                >
                  <MenuItem value="">All</MenuItem>
                  {ALARM_SEVERITIES.map((severity) => (
                    <MenuItem key={severity} value={severity}>
                      {severityConfig[severity].label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => setStatusFilter(e.target.value as AlarmStatus | '')}
                >
                  <MenuItem value="">All</MenuItem>
                  {ALARM_STATUSES.map((status) => (
                    <MenuItem key={status} value={status}>
                      {statusConfig[status].label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Alarm Type</InputLabel>
                <Select
                  value={typeFilter}
                  label="Alarm Type"
                  onChange={(e) => setTypeFilter(e.target.value)}
                >
                  <MenuItem value="">All</MenuItem>
                  {ALARM_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Button
                variant="outlined"
                onClick={() => {
                  setSeverityFilter('')
                  setStatusFilter('')
                  setTypeFilter('')
                }}
              >
                Clear Filters
              </Button>
            </Stack>
          </Box>
        )}

        <EntityTable
          title="Alarms"
          columns={columns}
          rows={alarms}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onRefresh={loadAlarms}
          onSearch={setSearchQuery}
          onPageChange={(newPage, newPageSize) => {
            setPage(newPage)
            setPageSize(newPageSize)
          }}
          page={page}
          pageSize={pageSize}
          rowActions={rowActions}
          customActions={customActions}
          enableActions={true}
        />

        {/* Alarm Details Dialog */}
        <Dialog
          open={openDetails}
          onClose={() => setOpenDetails(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Alarm Details</DialogTitle>
          <DialogContent>
            {selectedAlarm && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
                <Box>
                  <Typography variant="subtitle2" color="textSecondary">
                    Alarm Type
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {selectedAlarm.type}
                  </Typography>
                </Box>

                <Divider />

                <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      Severity
                    </Typography>
                    <Chip
                      label={severityConfig[selectedAlarm.severity].label}
                      size="small"
                      sx={{
                        bgcolor: severityConfig[selectedAlarm.severity].bgcolor,
                        color: severityConfig[selectedAlarm.severity].color,
                        fontWeight: 600,
                        mt: 0.5,
                      }}
                    />
                  </Box>

                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      Status
                    </Typography>
                    <Chip
                      label={statusConfig[selectedAlarm.status].label}
                      size="small"
                      sx={{
                        bgcolor: statusConfig[selectedAlarm.status].bgcolor,
                        color: statusConfig[selectedAlarm.status].color,
                        fontWeight: 500,
                        mt: 0.5,
                      }}
                    />
                  </Box>
                </Box>

                <Divider />

                <Box>
                  <Typography variant="subtitle2" color="textSecondary">
                    Originator
                  </Typography>
                  <Typography variant="body1">
                    {selectedAlarm.originatorName || 'Unknown'} ({selectedAlarm.originator.entityType})
                  </Typography>
                </Box>

                <Divider />

                <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      Created Time
                    </Typography>
                    <Typography variant="body2">
                      {format(new Date(selectedAlarm.createdTime), 'MMM dd, yyyy HH:mm:ss')}
                    </Typography>
                  </Box>

                  {selectedAlarm.ackTs && (
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary">
                        Acknowledged Time
                      </Typography>
                      <Typography variant="body2">
                        {format(new Date(selectedAlarm.ackTs), 'MMM dd, yyyy HH:mm:ss')}
                      </Typography>
                    </Box>
                  )}

                  {selectedAlarm.clearTs && (
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary">
                        Cleared Time
                      </Typography>
                      <Typography variant="body2">
                        {format(new Date(selectedAlarm.clearTs), 'MMM dd, yyyy HH:mm:ss')}
                      </Typography>
                    </Box>
                  )}
                </Box>

                {selectedAlarm.details && (
                  <>
                    <Divider />
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary" sx={{ mb: 1 }}>
                        Details
                      </Typography>
                      <Box
                        sx={{
                          bgcolor: '#F3F4F6',
                          p: 2,
                          borderRadius: 1,
                          fontFamily: 'monospace',
                          fontSize: '0.875rem',
                        }}
                      >
                        <pre>{JSON.stringify(selectedAlarm.details, null, 2)}</pre>
                      </Box>
                    </Box>
                  </>
                )}

                {selectedAlarm.propagate && (
                  <>
                    <Divider />
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary" sx={{ mb: 1 }}>
                        Propagation
                      </Typography>
                      <Stack direction="row" spacing={1}>
                        {selectedAlarm.propagateToOwner && (
                          <Chip label="To Owner" size="small" color="primary" variant="outlined" />
                        )}
                        {selectedAlarm.propagateToTenant && (
                          <Chip label="To Tenant" size="small" color="primary" variant="outlined" />
                        )}
                      </Stack>
                    </Box>
                  </>
                )}
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDetails(false)}>Close</Button>
            {selectedAlarm && !selectedAlarm.acknowledged && (
              <Button
                variant="contained"
                onClick={() => {
                  handleAcknowledge(selectedAlarm)
                  setOpenDetails(false)
                }}
                sx={{ bgcolor: '#FFB300' }}
              >
                Acknowledge
              </Button>
            )}
            {selectedAlarm && !selectedAlarm.cleared && (
              <Button
                variant="contained"
                onClick={() => {
                  handleClear(selectedAlarm)
                  setOpenDetails(false)
                }}
                sx={{ bgcolor: '#2E7D6F' }}
              >
                Clear
              </Button>
            )}
          </DialogActions>
        </Dialog>
      </Box>
    </MainLayout>
  )
}
