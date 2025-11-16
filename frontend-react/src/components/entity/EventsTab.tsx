/**
 * Events Tab Component
 * Displays lifecycle events, errors, and statistics for entities
 * Matches ThingsBoard's exact event tracking functionality
 *
 * Event Types:
 * - Lifecycle: Entity creation, updates, deletion
 * - Error: Connection errors, validation errors
 * - Statistics: Message counts, data processing stats
 */

import { useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip,
  Tabs,
  Tab,
  Chip,
  Alert,
  TablePagination,
} from '@mui/material'
import {
  Refresh as RefreshIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  CheckCircle as SuccessIcon,
  Timeline as StatsIcon,
} from '@mui/icons-material'
import { format } from 'date-fns'

export type EventType = 'LIFECYCLE' | 'ERROR' | 'STATS' | 'ALL'

export interface Event {
  id: string
  type: EventType
  severity?: 'INFO' | 'WARNING' | 'ERROR'
  message: string
  details?: string
  timestamp: number
  entityId?: string
  entityType?: string
}

interface EventsTabProps {
  entityId: string
  entityType: 'DEVICE' | 'ASSET' | 'CUSTOMER' | 'TENANT' | 'USER'
  events?: Event[]
  onRefresh?: () => void
  readOnly?: boolean
}

export default function EventsTab({
  entityId,
  entityType,
  events = [],
  onRefresh,
  readOnly = false,
}: EventsTabProps) {
  const [selectedType, setSelectedType] = useState<EventType>('ALL')
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  // Filter events by type
  const filteredEvents = selectedType === 'ALL'
    ? events
    : events.filter((event) => event.type === selectedType)

  const handleTypeChange = (_event: React.SyntheticEvent, newValue: EventType) => {
    setSelectedType(newValue)
    setPage(0)
  }

  const handleChangePage = (_event: unknown, newPage: number) => {
    setPage(newPage)
  }

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10))
    setPage(0)
  }

  const getSeverityIcon = (severity?: 'INFO' | 'WARNING' | 'ERROR') => {
    switch (severity) {
      case 'ERROR':
        return <ErrorIcon sx={{ color: 'error.main', fontSize: 18 }} />
      case 'WARNING':
        return <InfoIcon sx={{ color: 'warning.main', fontSize: 18 }} />
      case 'INFO':
      default:
        return <SuccessIcon sx={{ color: 'success.main', fontSize: 18 }} />
    }
  }

  const getSeverityColor = (severity?: 'INFO' | 'WARNING' | 'ERROR') => {
    switch (severity) {
      case 'ERROR':
        return 'error'
      case 'WARNING':
        return 'warning'
      case 'INFO':
      default:
        return 'success'
    }
  }

  const getEventTypeLabel = (type: EventType) => {
    switch (type) {
      case 'LIFECYCLE':
        return 'Lifecycle'
      case 'ERROR':
        return 'Errors'
      case 'STATS':
        return 'Statistics'
      case 'ALL':
        return 'All Events'
    }
  }

  const paginatedEvents = filteredEvents.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage
  )

  return (
    <Box>
      <Paper sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" sx={{ color: '#0F3E5C' }}>
            Events
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {onRefresh && (
              <IconButton color="primary" onClick={onRefresh} title="Refresh">
                <RefreshIcon />
              </IconButton>
            )}
          </Box>
        </Box>

        {/* Event Type Tabs */}
        <Tabs
          value={selectedType}
          onChange={handleTypeChange}
          sx={{
            mb: 2,
            borderBottom: 1,
            borderColor: 'divider',
          }}
        >
          <Tab label="All" value="ALL" />
          <Tab label="Lifecycle" value="LIFECYCLE" />
          <Tab label="Errors" value="ERROR" />
          <Tab label="Statistics" value="STATS" />
        </Tabs>

        {/* Events Table */}
        {filteredEvents.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography color="text.secondary">
              No {getEventTypeLabel(selectedType).toLowerCase()} found
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              Events will appear here as the entity performs actions
            </Typography>
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell width="50px"></TableCell>
                    <TableCell width="100px">Type</TableCell>
                    <TableCell>Message</TableCell>
                    <TableCell width="180px">Timestamp</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {paginatedEvents.map((event) => (
                    <TableRow key={event.id} hover>
                      <TableCell>
                        {getSeverityIcon(event.severity)}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={event.type}
                          size="small"
                          color={getSeverityColor(event.severity) as any}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {event.message}
                        </Typography>
                        {event.details && (
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{
                              display: 'block',
                              mt: 0.5,
                              maxWidth: 600,
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                          >
                            {event.details}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {format(new Date(event.timestamp), 'MMM dd, yyyy HH:mm:ss')}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {/* Pagination */}
            <TablePagination
              rowsPerPageOptions={[5, 10, 25, 50]}
              component="div"
              count={filteredEvents.length}
              rowsPerPage={rowsPerPage}
              page={page}
              onPageChange={handleChangePage}
              onRowsPerPageChange={handleChangeRowsPerPage}
            />
          </>
        )}

        {/* Stats */}
        <Box sx={{ mt: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <Chip
            label={`${events.length} total events`}
            size="small"
            color="primary"
          />
          <Chip
            label={`${events.filter(e => e.type === 'ERROR').length} errors`}
            size="small"
            color="error"
            variant={selectedType === 'ERROR' ? 'filled' : 'outlined'}
          />
          <Chip
            label={`${events.filter(e => e.type === 'LIFECYCLE').length} lifecycle`}
            size="small"
            color="info"
            variant={selectedType === 'LIFECYCLE' ? 'filled' : 'outlined'}
          />
          <Chip
            label={`${events.filter(e => e.type === 'STATS').length} stats`}
            size="small"
            color="success"
            variant={selectedType === 'STATS' ? 'filled' : 'outlined'}
          />
        </Box>

        {/* Info Alert */}
        <Alert severity="info" sx={{ mt: 2 }}>
          Events are automatically generated when the {entityType.toLowerCase()} performs actions.
          They cannot be manually created or deleted.
        </Alert>
      </Paper>
    </Box>
  )
}
