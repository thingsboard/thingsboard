/**
 * Alarm List Widget
 * Displays real-time alarms with severity indicators
 * Equivalent to ThingsBoard's Alarms Table widget
 */

import {
  Card,
  CardContent,
  Typography,
  Box,
  List,
  ListItem,
  Button,
  Chip,
} from '@mui/material'
import { Error as ErrorIcon, Warning as WarningIcon } from '@mui/icons-material'
import { WidgetComponentProps } from '@/types/dashboard'
import { registerWidget } from '../widgetRegistry'
import { format } from 'date-fns'

interface Alarm {
  id: string
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR' | 'WARNING' | 'INDETERMINATE'
  type: string
  originator: {
    name: string
    type: string
  }
  status: 'ACTIVE_UNACK' | 'ACTIVE_ACK' | 'CLEARED_UNACK' | 'CLEARED_ACK'
  startTs: number
  endTs?: number
  details?: Record<string, any>
}

const severityConfig = {
  CRITICAL: {
    color: '#C62828',
    icon: <ErrorIcon />,
    label: 'Critical',
  },
  MAJOR: {
    color: '#FFB300',
    icon: <WarningIcon />,
    label: 'Major',
  },
  MINOR: {
    color: '#FFB300',
    icon: <WarningIcon />,
    label: 'Minor',
  },
  WARNING: {
    color: '#FFB300',
    icon: <WarningIcon />,
    label: 'Warning',
  },
  INDETERMINATE: {
    color: '#8C959D',
    icon: <WarningIcon />,
    label: 'Info',
  },
}

function AlarmList({ widget, data }: WidgetComponentProps) {
  const { config } = widget
  const { settings } = config

  // Mock alarms data (in production, this comes from data prop)
  const alarms: Alarm[] = [
    {
      id: '1',
      severity: 'CRITICAL',
      type: 'Pressure Exceeded',
      originator: { name: 'Tank T-102', type: 'DEVICE' },
      status: 'ACTIVE_UNACK',
      startTs: Date.now() - 125000,
    },
    {
      id: '2',
      severity: 'MAJOR',
      type: 'Temperature High',
      originator: { name: 'Pump P-201', type: 'DEVICE' },
      status: 'ACTIVE_UNACK',
      startTs: Date.now() - 192000,
    },
    {
      id: '3',
      severity: 'MINOR',
      type: 'Low Flow Rate',
      originator: { name: 'Valve V-304', type: 'DEVICE' },
      status: 'ACTIVE_UNACK',
      startTs: Date.now() - 288000,
    },
    {
      id: '4',
      severity: 'CRITICAL',
      type: 'Motor Overload',
      originator: { name: 'Motor M-101', type: 'DEVICE' },
      status: 'ACTIVE_UNACK',
      startTs: Date.now() - 386000,
    },
    {
      id: '5',
      severity: 'CRITICAL',
      type: 'Connection Lost',
      originator: { name: 'Sensor S-405', type: 'DEVICE' },
      status: 'ACTIVE_UNACK',
      startTs: Date.now() - 566000,
    },
  ]

  const handleAcknowledge = (alarmId: string) => {
    console.log('Acknowledging alarm:', alarmId)
    // In production, this would call the API
  }

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: (theme) =>
          settings.backgroundColor ??
          (theme.palette.mode === 'dark' ? '#1F2428' : '#FFFFFF'),
        borderColor: (theme) =>
          theme.palette.mode === 'dark' ? '#363C42' : '#E0E0E0',
        borderWidth: 1,
        borderStyle: 'solid',
        borderRadius: settings.borderRadius ?? 2,
      }}
    >
      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2 }}>
        {/* Title */}
        <Typography
          variant="h6"
          sx={{
            fontWeight: 600,
            mb: 2,
            color: (theme) =>
              theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
          }}
        >
          {config.title || 'Real-Time Alarms'}
        </Typography>

        {/* Alarm List */}
        <Box sx={{ flex: 1, overflowY: 'auto', pr: 1 }}>
          <List sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            {alarms.map((alarm) => {
              const severityInfo = severityConfig[alarm.severity]
              return (
                <ListItem
                  key={alarm.id}
                  sx={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 1.5,
                    borderLeft: `4px solid ${severityInfo.color}`,
                    bgcolor: `${severityInfo.color}10`,
                    borderRadius: 1,
                    p: 1.5,
                  }}
                >
                  {/* Icon */}
                  <Box sx={{ color: severityInfo.color, mt: 0.5 }}>
                    {severityInfo.icon}
                  </Box>

                  {/* Content */}
                  <Box sx={{ flex: 1 }}>
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 'bold',
                        color: severityInfo.color,
                        mb: 0.5,
                      }}
                    >
                      {severityInfo.label}: {alarm.type}
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#8C959D' }}>
                      {alarm.originator.name} @ {format(alarm.startTs, 'HH:mm:ss')}
                    </Typography>
                  </Box>

                  {/* Acknowledge Button */}
                  {alarm.status === 'ACTIVE_UNACK' && (
                    <Button
                      size="small"
                      variant="text"
                      onClick={() => handleAcknowledge(alarm.id)}
                      sx={{
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        color: '#8C959D',
                        minWidth: 'auto',
                        px: 1,
                        '&:hover': {
                          color: (theme) =>
                            theme.palette.mode === 'dark' ? '#E1E3E5' : '#121517',
                        },
                      }}
                    >
                      ACK
                    </Button>
                  )}
                </ListItem>
              )
            })}
          </List>
        </Box>
      </CardContent>
    </Card>
  )
}

// Register widget
registerWidget(
  {
    id: 'alarm_table',
    name: 'Alarm List',
    type: 'alarm',
    description: 'Display real-time alarms with severity indicators',
    icon: 'notifications_active',
    defaultConfig: {
      datasources: [],
      settings: {
        showTitle: true,
        maxAlarms: 10,
        enableAcknowledge: true,
        enableClear: true,
        alarmSeverityColors: {
          CRITICAL: '#C62828',
          MAJOR: '#FFB300',
          MINOR: '#FFB300',
          WARNING: '#FFB300',
          INDETERMINATE: '#8C959D',
        },
      },
    },
    defaultSizeX: 4,
    defaultSizeY: 6,
  },
  AlarmList
)

export default AlarmList
