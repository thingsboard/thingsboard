/**
 * Audit Logs Page
 * System audit trail with filtering and export
 * Matches ThingsBoard ui-ngx audit logs functionality
 */

import { useState, useEffect } from 'react'
import {
  Box,
  Chip,
  Typography,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Button,
  Stack,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material'
import { FileDownload, FilterList } from '@mui/icons-material'
import { format } from 'date-fns'
import MainLayout from '@/components/layout/MainLayout'
import EntityTable, { EntityColumn } from '@/components/entity/EntityTable'

type ActionType =
  | 'ADDED'
  | 'DELETED'
  | 'UPDATED'
  | 'ATTRIBUTES_UPDATED'
  | 'ATTRIBUTES_DELETED'
  | 'RPC_CALL'
  | 'CREDENTIALS_UPDATED'
  | 'ASSIGNED_TO_CUSTOMER'
  | 'UNASSIGNED_FROM_CUSTOMER'
  | 'ACTIVATED'
  | 'SUSPENDED'
  | 'CREDENTIALS_READ'
  | 'ATTRIBUTES_READ'

type EntityType = 'DEVICE' | 'ASSET' | 'CUSTOMER' | 'USER' | 'DASHBOARD' | 'RULE_CHAIN' | 'ALARM'

type ActionStatus = 'SUCCESS' | 'FAILURE'

interface AuditLog {
  id: string
  createdTime: number
  entityType: EntityType
  entityId?: string
  entityName: string
  userId: string
  userName: string
  userEmail: string
  actionType: ActionType
  actionData?: any
  actionStatus: ActionStatus
  actionFailureDetails?: string
}

const ACTION_TYPES: ActionType[] = [
  'ADDED',
  'DELETED',
  'UPDATED',
  'ATTRIBUTES_UPDATED',
  'ATTRIBUTES_DELETED',
  'RPC_CALL',
  'CREDENTIALS_UPDATED',
  'ASSIGNED_TO_CUSTOMER',
  'UNASSIGNED_FROM_CUSTOMER',
  'ACTIVATED',
  'SUSPENDED',
  'CREDENTIALS_READ',
  'ATTRIBUTES_READ',
]

const ENTITY_TYPES: EntityType[] = [
  'DEVICE',
  'ASSET',
  'CUSTOMER',
  'USER',
  'DASHBOARD',
  'RULE_CHAIN',
  'ALARM',
]

const actionTypeColors: Record<ActionType, { bgcolor: string; color: string }> = {
  ADDED: { bgcolor: '#E0F2F1', color: '#2E7D6F' },
  DELETED: { bgcolor: '#FFEBEE', color: '#C62828' },
  UPDATED: { bgcolor: '#E3F2FD', color: '#0F3E5C' },
  ATTRIBUTES_UPDATED: { bgcolor: '#E3F2FD', color: '#0F3E5C' },
  ATTRIBUTES_DELETED: { bgcolor: '#FFEBEE', color: '#C62828' },
  RPC_CALL: { bgcolor: '#FFF3E0', color: '#E65100' },
  CREDENTIALS_UPDATED: { bgcolor: '#F3E5F5', color: '#7B1FA2' },
  ASSIGNED_TO_CUSTOMER: { bgcolor: '#E0F2F1', color: '#2E7D6F' },
  UNASSIGNED_FROM_CUSTOMER: { bgcolor: '#FFF3E0', color: '#E65100' },
  ACTIVATED: { bgcolor: '#E0F2F1', color: '#2E7D6F' },
  SUSPENDED: { bgcolor: '#FFEBEE', color: '#C62828' },
  CREDENTIALS_READ: { bgcolor: '#E8F5E9', color: '#388E3C' },
  ATTRIBUTES_READ: { bgcolor: '#E8F5E9', color: '#388E3C' },
}

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')

  // Filter states
  const [actionTypeFilter, setActionTypeFilter] = useState<ActionType | ''>('')
  const [entityTypeFilter, setEntityTypeFilter] = useState<EntityType | ''>('')
  const [statusFilter, setStatusFilter] = useState<ActionStatus | ''>('')
  const [openFilters, setOpenFilters] = useState(false)

  // Detail dialog
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null)
  const [openDetails, setOpenDetails] = useState(false)

  useEffect(() => {
    loadLogs()
  }, [page, pageSize, searchQuery, actionTypeFilter, entityTypeFilter, statusFilter])

  const loadLogs = async () => {
    setLoading(true)

    // Mock data - in production, this would be an API call
    const mockLogs: AuditLog[] = [
      {
        id: '1',
        createdTime: Date.now() - 3600000,
        entityType: 'DEVICE',
        entityId: 'device-1',
        entityName: 'Temperature Sensor 001',
        userId: 'user-1',
        userName: 'John Doe',
        userEmail: 'john.doe@example.com',
        actionType: 'ATTRIBUTES_UPDATED',
        actionData: { firmwareVersion: '1.2.3' },
        actionStatus: 'SUCCESS',
      },
      {
        id: '2',
        createdTime: Date.now() - 7200000,
        entityType: 'CUSTOMER',
        entityId: 'customer-1',
        entityName: 'ABC Manufacturing Inc',
        userId: 'user-2',
        userName: 'Jane Smith',
        userEmail: 'jane.smith@example.com',
        actionType: 'ADDED',
        actionStatus: 'SUCCESS',
      },
      {
        id: '3',
        createdTime: Date.now() - 14400000,
        entityType: 'DEVICE',
        entityId: 'device-2',
        entityName: 'Gateway 001',
        userId: 'user-1',
        userName: 'John Doe',
        userEmail: 'john.doe@example.com',
        actionType: 'DELETED',
        actionStatus: 'SUCCESS',
      },
      {
        id: '4',
        createdTime: Date.now() - 21600000,
        entityType: 'USER',
        entityId: 'user-3',
        entityName: 'Bob Wilson',
        userId: 'user-2',
        userName: 'Jane Smith',
        userEmail: 'jane.smith@example.com',
        actionType: 'CREDENTIALS_UPDATED',
        actionStatus: 'SUCCESS',
      },
      {
        id: '5',
        createdTime: Date.now() - 28800000,
        entityType: 'DEVICE',
        entityId: 'device-3',
        entityName: 'Sensor 003',
        userId: 'user-1',
        userName: 'John Doe',
        userEmail: 'john.doe@example.com',
        actionType: 'RPC_CALL',
        actionData: { method: 'setValue', params: { temp: 25 } },
        actionStatus: 'FAILURE',
        actionFailureDetails: 'Device is offline',
      },
    ]

    // Apply filters
    let filtered = mockLogs

    if (searchQuery) {
      filtered = filtered.filter(
        (log) =>
          log.entityName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          log.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          log.userEmail.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }

    if (actionTypeFilter) {
      filtered = filtered.filter((log) => log.actionType === actionTypeFilter)
    }

    if (entityTypeFilter) {
      filtered = filtered.filter((log) => log.entityType === entityTypeFilter)
    }

    if (statusFilter) {
      filtered = filtered.filter((log) => log.actionStatus === statusFilter)
    }

    setLogs(filtered.slice(page * pageSize, (page + 1) * pageSize))
    setTotalElements(filtered.length)
    setLoading(false)
  }

  const columns: EntityColumn[] = [
    {
      id: 'createdTime',
      label: 'Time',
      minWidth: 180,
      format: (value: number) => format(new Date(value), 'MMM dd, yyyy HH:mm:ss'),
    },
    {
      id: 'entityName',
      label: 'Entity',
      minWidth: 200,
      format: (value, row: AuditLog) => (
        <Box>
          <Box sx={{ fontWeight: 600, color: '#0F3E5C' }}>{value}</Box>
          <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
            {row.entityType}
          </Box>
        </Box>
      ),
    },
    {
      id: 'actionType',
      label: 'Action',
      minWidth: 180,
      format: (value: ActionType) => {
        const config = actionTypeColors[value]
        return (
          <Chip
            label={value.replace(/_/g, ' ')}
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
    {
      id: 'userName',
      label: 'User',
      minWidth: 200,
      format: (value, row: AuditLog) => (
        <Box>
          <Box sx={{ fontWeight: 500 }}>{value}</Box>
          <Box sx={{ fontSize: '0.75rem', color: '#8C959D', mt: 0.5 }}>
            {row.userEmail}
          </Box>
        </Box>
      ),
    },
    {
      id: 'actionStatus',
      label: 'Status',
      minWidth: 100,
      format: (value: ActionStatus) =>
        value === 'SUCCESS' ? (
          <Chip
            label="Success"
            size="small"
            color="success"
            sx={{ fontWeight: 500 }}
          />
        ) : (
          <Chip
            label="Failure"
            size="small"
            sx={{ bgcolor: '#C62828', color: 'white', fontWeight: 500 }}
          />
        ),
    },
  ]

  const handleExport = () => {
    // Create CSV export
    const headers = ['Time', 'Entity Type', 'Entity Name', 'Action', 'User', 'Status']
    const csv = [
      headers.join(','),
      ...logs.map((log) =>
        [
          format(new Date(log.createdTime), 'yyyy-MM-dd HH:mm:ss'),
          log.entityType,
          log.entityName,
          log.actionType,
          log.userEmail,
          log.actionStatus,
        ].join(',')
      ),
    ].join('\n')

    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `audit_logs_${format(new Date(), 'yyyy-MM-dd_HH-mm-ss')}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  const rowActions = (row: AuditLog) => (
    <Button
      size="small"
      onClick={() => {
        setSelectedLog(row)
        setOpenDetails(true)
      }}
    >
      Details
    </Button>
  )

  const customActions = (
    <>
      <Button
        variant="outlined"
        startIcon={<FilterList />}
        onClick={() => setOpenFilters(!openFilters)}
      >
        Filters
      </Button>
      <Button variant="outlined" startIcon={<FileDownload />} onClick={handleExport}>
        Export
      </Button>
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
                <InputLabel>Action Type</InputLabel>
                <Select
                  value={actionTypeFilter}
                  label="Action Type"
                  onChange={(e) => setActionTypeFilter(e.target.value as ActionType | '')}
                >
                  <MenuItem value="">All</MenuItem>
                  {ACTION_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Entity Type</InputLabel>
                <Select
                  value={entityTypeFilter}
                  label="Entity Type"
                  onChange={(e) => setEntityTypeFilter(e.target.value as EntityType | '')}
                >
                  <MenuItem value="">All</MenuItem>
                  {ENTITY_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 200 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => setStatusFilter(e.target.value as ActionStatus | '')}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="SUCCESS">Success</MenuItem>
                  <MenuItem value="FAILURE">Failure</MenuItem>
                </Select>
              </FormControl>

              <Button
                variant="outlined"
                onClick={() => {
                  setActionTypeFilter('')
                  setEntityTypeFilter('')
                  setStatusFilter('')
                }}
              >
                Clear Filters
              </Button>
            </Stack>
          </Box>
        )}

        <EntityTable
          title="Audit Logs"
          columns={columns}
          rows={logs}
          totalElements={totalElements}
          loading={loading}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onRefresh={loadLogs}
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

        {/* Details Dialog */}
        <Dialog
          open={openDetails}
          onClose={() => setOpenDetails(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Audit Log Details</DialogTitle>
          <DialogContent>
            {selectedLog && (
              <Box sx={{ pt: 2 }}>
                <Stack spacing={2}>
                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      Time
                    </Typography>
                    <Typography variant="body1">
                      {format(new Date(selectedLog.createdTime), 'MMM dd, yyyy HH:mm:ss')}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      Entity
                    </Typography>
                    <Typography variant="body1">
                      {selectedLog.entityName} ({selectedLog.entityType})
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      Action
                    </Typography>
                    <Chip
                      label={selectedLog.actionType.replace(/_/g, ' ')}
                      sx={{
                        ...actionTypeColors[selectedLog.actionType],
                        mt: 0.5,
                      }}
                    />
                  </Box>
                  <Box>
                    <Typography variant="subtitle2" color="textSecondary">
                      User
                    </Typography>
                    <Typography variant="body1">
                      {selectedLog.userName} ({selectedLog.userEmail})
                    </Typography>
                  </Box>
                  {selectedLog.actionData && (
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary" sx={{ mb: 1 }}>
                        Action Data
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
                        <pre>{JSON.stringify(selectedLog.actionData, null, 2)}</pre>
                      </Box>
                    </Box>
                  )}
                  {selectedLog.actionFailureDetails && (
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary">
                        Failure Details
                      </Typography>
                      <Typography variant="body1" color="error">
                        {selectedLog.actionFailureDetails}
                      </Typography>
                    </Box>
                  )}
                </Stack>
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDetails(false)}>Close</Button>
          </DialogActions>
        </Dialog>
      </Box>
    </MainLayout>
  )
}
