/**
 * Timeseries Table Widget - Display timeseries data in table format
 */
import { useState } from 'react'
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TablePagination } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'
import { format } from 'date-fns'

function TimeseriesTable({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)

  const timeseriesData = data?.datasources?.[0]?.data || []
  const dataKeys = widget.config.datasources?.[0]?.dataKeys || []

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Timeseries Data'}
        </Typography>
      )}
      <TableContainer sx={{ flex: 1 }}>
        <Table stickyHeader size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Timestamp</TableCell>
              {dataKeys.map((key: any, index: number) => (
                <TableCell key={index} sx={{ fontWeight: 'bold' }}>{key.label || key.name}</TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {timeseriesData.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((row: any, index: number) => (
              <TableRow key={index} hover>
                <TableCell>{format(new Date(row.ts), 'MMM dd, HH:mm:ss')}</TableCell>
                <TableCell>{row.value?.toFixed(2) || row.value}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination rowsPerPageOptions={[5, 10, 25, 50]} component="div" count={timeseriesData.length} rowsPerPage={rowsPerPage} page={page} onPageChange={(_, newPage) => setPage(newPage)} onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }} />
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'timeseries_table',
  name: 'Timeseries Table',
  description: 'Display timeseries data in table format',
  type: 'timeseries',
  tags: ['table', 'timeseries', 'data', 'history'],
}

registerWidget(descriptor, TimeseriesTable)
export default TimeseriesTable
