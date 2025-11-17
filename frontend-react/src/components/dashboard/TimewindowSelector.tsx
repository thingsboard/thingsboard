/**
 * Timewindow Selector Component
 * Allows users to select time ranges for dashboard data
 * Matches ThingsBoard's timewindow functionality
 */

import { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Tabs,
  Tab,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Grid,
  Typography,
  ButtonGroup,
  IconButton,
} from '@mui/material'
import {
  AccessTime as TimeIcon,
  Close as CloseIcon,
  History as HistoryIcon,
  Update as RealtimeIcon,
} from '@mui/icons-material'
import { format, subHours, subDays, subWeeks, subMonths } from 'date-fns'

interface TimewindowSelectorProps {
  open: boolean
  onClose: () => void
  onApply: (timewindow: any) => void
  currentTimewindow?: any
}

export default function TimewindowSelector({
  open,
  onClose,
  onApply,
  currentTimewindow,
}: TimewindowSelectorProps) {
  const [mode, setMode] = useState<'realtime' | 'history'>('realtime')
  const [realtimeWindow, setRealtimeWindow] = useState(300000) // 5 minutes
  const [historyStart, setHistoryStart] = useState(format(subHours(new Date(), 1), "yyyy-MM-dd'T'HH:mm"))
  const [historyEnd, setHistoryEnd] = useState(format(new Date(), "yyyy-MM-dd'T'HH:mm"))

  const quickSelects = [
    { label: 'Last 5 min', value: 300000 },
    { label: 'Last 15 min', value: 900000 },
    { label: 'Last 30 min', value: 1800000 },
    { label: 'Last 1 hour', value: 3600000 },
    { label: 'Last 6 hours', value: 21600000 },
    { label: 'Last 12 hours', value: 43200000 },
    { label: 'Last 24 hours', value: 86400000 },
  ]

  const historyQuickSelects = [
    { label: 'Last Hour', start: subHours(new Date(), 1) },
    { label: 'Last Day', start: subDays(new Date(), 1) },
    { label: 'Last Week', start: subWeeks(new Date(), 1) },
    { label: 'Last Month', start: subMonths(new Date(), 1) },
  ]

  const handleApply = () => {
    const timewindow = mode === 'realtime'
      ? {
          realtime: {
            timewindowMs: realtimeWindow,
            interval: 1000,
          },
        }
      : {
          history: {
            startTimeMs: new Date(historyStart).getTime(),
            endTimeMs: new Date(historyEnd).getTime(),
          },
        }

    onApply(timewindow)
    onClose()
  }

  const handleQuickSelect = (value: number) => {
    setRealtimeWindow(value)
  }

  const handleHistoryQuickSelect = (start: Date) => {
    setHistoryStart(format(start, "yyyy-MM-dd'T'HH:mm"))
    setHistoryEnd(format(new Date(), "yyyy-MM-dd'T'HH:mm"))
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ bgcolor: '#0F3E5C', color: 'white' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <TimeIcon />
            <Typography variant="h6">Timewindow</Typography>
          </Box>
          <IconButton onClick={onClose} sx={{ color: 'white' }}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={mode} onChange={(_, v) => setMode(v)}>
          <Tab icon={<RealtimeIcon />} label="Realtime" value="realtime" iconPosition="start" />
          <Tab icon={<HistoryIcon />} label="History" value="history" iconPosition="start" />
        </Tabs>
      </Box>

      <DialogContent>
        {mode === 'realtime' ? (
          <Box sx={{ py: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
              Quick Select
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 3 }}>
              {quickSelects.map((item) => (
                <Button
                  key={item.value}
                  variant={realtimeWindow === item.value ? 'contained' : 'outlined'}
                  onClick={() => handleQuickSelect(item.value)}
                  sx={{
                    bgcolor: realtimeWindow === item.value ? '#0F3E5C' : 'transparent',
                    '&:hover': {
                      bgcolor: realtimeWindow === item.value ? '#2E7D6F' : 'rgba(15, 62, 92, 0.1)',
                    },
                  }}
                >
                  {item.label}
                </Button>
              ))}
            </Box>

            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
              Custom Range
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  type="number"
                  label="Time window (milliseconds)"
                  value={realtimeWindow}
                  onChange={(e) => setRealtimeWindow(parseInt(e.target.value))}
                  helperText={`Show data from last ${Math.floor(realtimeWindow / 60000)} minute(s)`}
                />
              </Grid>
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Update Interval</InputLabel>
                  <Select value={1000} label="Update Interval">
                    <MenuItem value={1000}>1 second</MenuItem>
                    <MenuItem value={5000}>5 seconds</MenuItem>
                    <MenuItem value={10000}>10 seconds</MenuItem>
                    <MenuItem value={30000}>30 seconds</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </Box>
        ) : (
          <Box sx={{ py: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
              Quick Select
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 3 }}>
              {historyQuickSelects.map((item) => (
                <Button
                  key={item.label}
                  variant="outlined"
                  onClick={() => handleHistoryQuickSelect(item.start)}
                >
                  {item.label}
                </Button>
              ))}
            </Box>

            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 'bold' }}>
              Custom Range
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  type="datetime-local"
                  label="Start Time"
                  value={historyStart}
                  onChange={(e) => setHistoryStart(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  type="datetime-local"
                  label="End Time"
                  value={historyEnd}
                  onChange={(e) => setHistoryEnd(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </Grid>
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} color="inherit">
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleApply}
          sx={{ bgcolor: '#0F3E5C', '&:hover': { bgcolor: '#2E7D6F' } }}
        >
          Apply
        </Button>
      </DialogActions>
    </Dialog>
  )
}
