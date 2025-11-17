/**
 * Calendar Widget - Event calendar with schedule visualization
 * Shows events, schedules, and device maintenance
 */
import { useState } from 'react'
import { Box, Typography, Paper, IconButton, Chip } from '@mui/material'
import {
  ChevronLeft,
  ChevronRight,
  Today,
  Event as EventIcon,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface CalendarEvent {
  id: string
  title: string
  date: Date
  type: 'maintenance' | 'alert' | 'meeting' | 'scheduled'
  color: string
}

function CalendarWidget({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [currentDate, setCurrentDate] = useState(new Date())

  // Demo events
  const events: CalendarEvent[] = [
    {
      id: '1',
      title: 'Gateway Maintenance',
      date: new Date(currentDate.getFullYear(), currentDate.getMonth(), 5),
      type: 'maintenance',
      color: '#FFB300',
    },
    {
      id: '2',
      title: 'Temperature Alert',
      date: new Date(currentDate.getFullYear(), currentDate.getMonth(), 12),
      type: 'alert',
      color: '#C62828',
    },
    {
      id: '3',
      title: 'Team Review',
      date: new Date(currentDate.getFullYear(), currentDate.getMonth(), 18),
      type: 'meeting',
      color: '#1E88E5',
    },
    {
      id: '4',
      title: 'Scheduled Backup',
      date: new Date(currentDate.getFullYear(), currentDate.getMonth(), 25),
      type: 'scheduled',
      color: '#2E7D6F',
    },
  ]

  const daysInMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0).getDate()
  const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1).getDay()

  const monthNames = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ]

  const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

  const previousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1))
  }

  const nextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1))
  }

  const goToToday = () => {
    setCurrentDate(new Date())
  }

  const getEventsForDate = (day: number): CalendarEvent[] => {
    return events.filter(
      (event) =>
        event.date.getDate() === day &&
        event.date.getMonth() === currentDate.getMonth() &&
        event.date.getFullYear() === currentDate.getFullYear()
    )
  }

  const isToday = (day: number): boolean => {
    const today = new Date()
    return (
      day === today.getDate() &&
      currentDate.getMonth() === today.getMonth() &&
      currentDate.getFullYear() === today.getFullYear()
    )
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Calendar'}
        </Typography>
        <IconButton size="small" onClick={goToToday} sx={{ color: '#2E7D6F' }}>
          <Today fontSize="small" />
        </IconButton>
      </Box>

      {/* Month navigation */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <IconButton size="small" onClick={previousMonth}>
          <ChevronLeft />
        </IconButton>
        <Typography variant="subtitle1" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
          {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
        </Typography>
        <IconButton size="small" onClick={nextMonth}>
          <ChevronRight />
        </IconButton>
      </Box>

      {/* Calendar grid */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {/* Day names */}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 1fr)',
            gap: 0.5,
            mb: 0.5,
          }}
        >
          {dayNames.map((day) => (
            <Box
              key={day}
              sx={{
                p: 0.5,
                textAlign: 'center',
                fontSize: '10px',
                fontWeight: 'bold',
                color: '#757575',
              }}
            >
              {day}
            </Box>
          ))}
        </Box>

        {/* Calendar days */}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 1fr)',
            gap: 0.5,
          }}
        >
          {/* Empty cells for days before month starts */}
          {Array.from({ length: firstDayOfMonth }).map((_, i) => (
            <Box key={`empty-${i}`} sx={{ aspectRatio: '1', bgcolor: '#F5F5F5', borderRadius: 1 }} />
          ))}

          {/* Calendar days */}
          {Array.from({ length: daysInMonth }).map((_, i) => {
            const day = i + 1
            const dayEvents = getEventsForDate(day)
            const today = isToday(day)

            return (
              <Box
                key={day}
                sx={{
                  aspectRatio: '1',
                  border: today ? '2px solid #2E7D6F' : '1px solid #E0E0E0',
                  borderRadius: 1,
                  p: 0.5,
                  bgcolor: today ? 'rgba(46, 125, 111, 0.1)' : 'white',
                  display: 'flex',
                  flexDirection: 'column',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    bgcolor: 'rgba(15, 62, 92, 0.05)',
                    transform: 'scale(1.05)',
                  },
                }}
              >
                <Typography
                  variant="caption"
                  sx={{
                    fontSize: '11px',
                    fontWeight: today ? 'bold' : 'normal',
                    color: today ? '#2E7D6F' : '#0F3E5C',
                  }}
                >
                  {day}
                </Typography>
                <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 0.3, mt: 0.3 }}>
                  {dayEvents.slice(0, 2).map((event) => (
                    <Box
                      key={event.id}
                      sx={{
                        width: '100%',
                        height: 3,
                        bgcolor: event.color,
                        borderRadius: 1,
                      }}
                      title={event.title}
                    />
                  ))}
                  {dayEvents.length > 2 && (
                    <Typography variant="caption" sx={{ fontSize: '8px', color: '#757575' }}>
                      +{dayEvents.length - 2}
                    </Typography>
                  )}
                </Box>
              </Box>
            )
          })}
        </Box>
      </Box>

      {/* Events legend */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold', display: 'block', mb: 1 }}>
          Upcoming Events:
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
          {events.slice(0, 4).map((event) => (
            <Chip
              key={event.id}
              icon={<EventIcon sx={{ fontSize: 12 }} />}
              label={`${event.title} (${event.date.getDate()})`}
              size="small"
              sx={{
                fontSize: '9px',
                height: 20,
                bgcolor: event.color,
                color: 'white',
              }}
            />
          ))}
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'calendar_widget',
  name: 'Calendar',
  description: 'Event calendar with schedule and maintenance tracking',
  type: 'static',
  tags: ['calendar', 'events', 'schedule', 'planning', 'maintenance'],
}

registerWidget(descriptor, CalendarWidget)
export default CalendarWidget
