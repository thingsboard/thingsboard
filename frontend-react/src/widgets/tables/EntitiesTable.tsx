/**
 * Entities Table Widget
 * Displays a table of entities with sorting and filtering
 */

import { useState } from 'react'
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Tooltip,
  Chip,
} from '@mui/material'
import {
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function EntitiesTable({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const showTitle = config.showTitle ?? true
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  // Mock entity data (in production, comes from datasource)
  const entities = [
    { id: '1', name: 'Device A', type: 'Sensor', active: true, createdTime: Date.now() },
    { id: '2', name: 'Device B', type: 'Gateway', active: true, createdTime: Date.now() },
    { id: '3', name: 'Device C', type: 'Sensor', active: false, createdTime: Date.now() },
    { id: '4', name: 'Asset X', type: 'Building', active: true, createdTime: Date.now() },
    { id: '5', name: 'Asset Y', type: 'Equipment', active: true, createdTime: Date.now() },
  ]

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage)
  }

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10))
    setPage(0)
  }

  return (
    <Paper
      elevation={1}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        p: 2,
      }}
    >
      {showTitle && (
        <Typography
          variant="h6"
          sx={{
            mb: 2,
            fontSize: '16px',
            fontWeight: 'bold',
            color: '#0F3E5C',
          }}
        >
          {config.title || 'Entities'}
        </Typography>
      )}

      <TableContainer sx={{ flex: 1 }}>
        <Table stickyHeader size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Name</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Type</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Status</TableCell>
              <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {entities
              .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
              .map((entity) => (
                <TableRow key={entity.id} hover>
                  <TableCell>{entity.name}</TableCell>
                  <TableCell>{entity.type}</TableCell>
                  <TableCell>
                    <Chip
                      label={entity.active ? 'Active' : 'Inactive'}
                      size="small"
                      color={entity.active ? 'success' : 'default'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="View">
                      <IconButton size="small">
                        <ViewIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Edit">
                      <IconButton size="small">
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton size="small" color="error">
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TablePagination
        rowsPerPageOptions={[5, 10, 25]}
        component="div"
        count={entities.length}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
      />
    </Paper>
  )
}

// Widget descriptor
const descriptor: WidgetTypeDescriptor = {
  id: 'entities_table',
  name: 'Entities Table',
  description: 'Display entities in a sortable, filterable table',
  type: 'latest',
  tags: ['table', 'entities', 'list'],
}

// Register widget
registerWidget(descriptor, EntitiesTable)

export default EntitiesTable
