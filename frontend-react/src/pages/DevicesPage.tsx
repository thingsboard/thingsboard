import { useEffect } from 'react'
import {
  Typography,
  Box,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
} from '@mui/material'
import { Add } from '@mui/icons-material'
import { useAppDispatch, useAppSelector } from '@/hooks/redux'
import { fetchDevices, selectDevices, selectDevicesLoading } from '@/store/devices/devicesSlice'

export default function DevicesPage() {
  const dispatch = useAppDispatch()
  const devices = useAppSelector(selectDevices)
  const loading = useAppSelector(selectDevicesLoading)

  useEffect(() => {
    dispatch(fetchDevices())
  }, [dispatch])

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Devices</Typography>
        <Button variant="contained" startIcon={<Add />}>
          Add Device
        </Button>
      </Box>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Label</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {devices.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    No devices found. Click "Add Device" to create one.
                  </TableCell>
                </TableRow>
              ) : (
                devices.map((device) => (
                  <TableRow key={device.id}>
                    <TableCell>{device.name}</TableCell>
                    <TableCell>{device.type}</TableCell>
                    <TableCell>{device.label || '-'}</TableCell>
                    <TableCell>{new Date(device.created_time).toLocaleString()}</TableCell>
                    <TableCell>
                      <Button size="small">View</Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  )
}
