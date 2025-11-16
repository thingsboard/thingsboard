/**
 * Entity Table Component
 * Reusable table for all entity types (Devices, Assets, Customers, etc.)
 * Matches ThingsBoard ui-ngx entity table functionality
 */

import { useState } from 'react'
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Checkbox,
  IconButton,
  Toolbar,
  Typography,
  Tooltip,
  TextField,
  InputAdornment,
  Button,
  Menu,
  MenuItem,
  Chip,
} from '@mui/material'
import {
  Search,
  Add,
  Delete,
  Edit,
  MoreVert,
  FilterList,
  FileDownload,
  Refresh,
} from '@mui/icons-material'

export interface EntityColumn {
  id: string
  label: string
  minWidth?: number
  align?: 'left' | 'right' | 'center'
  format?: (value: any, row: any) => React.ReactNode
  sortable?: boolean
}

export interface EntityTableProps<T = any> {
  title: string
  columns: EntityColumn[]
  rows: T[]
  totalElements?: number
  loading?: boolean
  selectedIds?: string[]
  onSelectionChange?: (ids: string[]) => void
  onAdd?: () => void
  onEdit?: (entity: T) => void
  onDelete?: (ids: string[]) => void
  onRefresh?: () => void
  onSearch?: (query: string) => void
  onPageChange?: (page: number, pageSize: number) => void
  onSort?: (column: string, direction: 'asc' | 'desc') => void
  page?: number
  pageSize?: number
  enableSelection?: boolean
  enableSearch?: boolean
  enableActions?: boolean
  customActions?: React.ReactNode
  rowActions?: (row: T) => React.ReactNode
}

export default function EntityTable<T extends { id: string }>({
  title,
  columns,
  rows,
  totalElements = 0,
  loading = false,
  selectedIds = [],
  onSelectionChange,
  onAdd,
  onEdit,
  onDelete,
  onRefresh,
  onSearch,
  onPageChange,
  onSort,
  page = 0,
  pageSize = 10,
  enableSelection = true,
  enableSearch = true,
  enableActions = true,
  customActions,
  rowActions,
}: EntityTableProps<T>) {
  const [searchQuery, setSearchQuery] = useState('')
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)

  const handleSelectAll = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.checked) {
      onSelectionChange?.(rows.map((row) => row.id))
    } else {
      onSelectionChange?.([])
    }
  }

  const handleSelectRow = (id: string) => {
    const selectedIndex = selectedIds.indexOf(id)
    let newSelected: string[] = []

    if (selectedIndex === -1) {
      newSelected = [...selectedIds, id]
    } else {
      newSelected = selectedIds.filter((selectedId) => selectedId !== id)
    }

    onSelectionChange?.(newSelected)
  }

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value)
    onSearch?.(event.target.value)
  }

  const handleChangePage = (_event: unknown, newPage: number) => {
    onPageChange?.(newPage, pageSize)
  }

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newPageSize = parseInt(event.target.value, 10)
    onPageChange?.(0, newPageSize)
  }

  const handleDeleteSelected = () => {
    if (selectedIds.length > 0) {
      onDelete?.(selectedIds)
    }
  }

  const isSelected = (id: string) => selectedIds.indexOf(id) !== -1

  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      {/* Toolbar */}
      <Toolbar
        sx={{
          pl: { sm: 2 },
          pr: { xs: 1, sm: 1 },
          bgcolor: selectedIds.length > 0 ? 'rgba(45, 107, 154, 0.08)' : undefined,
        }}
      >
        {selectedIds.length > 0 ? (
          <Typography
            sx={{ flex: '1 1 100%' }}
            color="inherit"
            variant="subtitle1"
            component="div"
          >
            {selectedIds.length} selected
          </Typography>
        ) : (
          <>
            <Typography
              sx={{ flex: '1 1 100%' }}
              variant="h6"
              component="div"
            >
              {title}
            </Typography>

            {enableSearch && (
              <TextField
                size="small"
                placeholder={`Search ${title.toLowerCase()}...`}
                value={searchQuery}
                onChange={handleSearchChange}
                sx={{ width: 300, mr: 2 }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search />
                    </InputAdornment>
                  ),
                }}
              />
            )}
          </>
        )}

        {/* Actions */}
        <Box sx={{ display: 'flex', gap: 1 }}>
          {selectedIds.length > 0 ? (
            <Tooltip title="Delete selected">
              <IconButton onClick={handleDeleteSelected}>
                <Delete />
              </IconButton>
            </Tooltip>
          ) : (
            <>
              {customActions}

              {onRefresh && (
                <Tooltip title="Refresh">
                  <IconButton onClick={onRefresh}>
                    <Refresh />
                  </IconButton>
                </Tooltip>
              )}

              {onAdd && (
                <Button
                  variant="contained"
                  startIcon={<Add />}
                  onClick={onAdd}
                  sx={{
                    bgcolor: '#0F3E5C',
                    '&:hover': { bgcolor: '#0A2D45' },
                  }}
                >
                  Add {title.replace(/s$/, '')}
                </Button>
              )}
            </>
          )}
        </Box>
      </Toolbar>

      {/* Table */}
      <TableContainer sx={{ maxHeight: 600 }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow>
              {enableSelection && (
                <TableCell padding="checkbox">
                  <Checkbox
                    indeterminate={
                      selectedIds.length > 0 && selectedIds.length < rows.length
                    }
                    checked={rows.length > 0 && selectedIds.length === rows.length}
                    onChange={handleSelectAll}
                  />
                </TableCell>
              )}
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  align={column.align}
                  style={{ minWidth: column.minWidth }}
                  sx={{ fontWeight: 600 }}
                >
                  {column.label}
                </TableCell>
              ))}
              {enableActions && <TableCell align="right">Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length + (enableSelection ? 1 : 0) + (enableActions ? 1 : 0)} align="center">
                  <Typography color="textSecondary">Loading...</Typography>
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + (enableSelection ? 1 : 0) + (enableActions ? 1 : 0)} align="center">
                  <Typography color="textSecondary">No {title.toLowerCase()} found</Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => {
                const isItemSelected = isSelected(row.id)
                return (
                  <TableRow
                    hover
                    role="checkbox"
                    tabIndex={-1}
                    key={row.id}
                    selected={isItemSelected}
                  >
                    {enableSelection && (
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={isItemSelected}
                          onChange={() => handleSelectRow(row.id)}
                        />
                      </TableCell>
                    )}
                    {columns.map((column) => {
                      const value = (row as any)[column.id]
                      return (
                        <TableCell key={column.id} align={column.align}>
                          {column.format ? column.format(value, row) : value}
                        </TableCell>
                      )
                    })}
                    {enableActions && (
                      <TableCell align="right">
                        {rowActions ? (
                          rowActions(row)
                        ) : (
                          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                            {onEdit && (
                              <Tooltip title="Edit">
                                <IconButton size="small" onClick={() => onEdit(row)}>
                                  <Edit fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            )}
                            {onDelete && (
                              <Tooltip title="Delete">
                                <IconButton
                                  size="small"
                                  onClick={() => onDelete([row.id])}
                                >
                                  <Delete fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            )}
                          </Box>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Pagination */}
      <TablePagination
        rowsPerPageOptions={[10, 25, 50, 100]}
        component="div"
        count={totalElements}
        rowsPerPage={pageSize}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
      />
    </Paper>
  )
}
