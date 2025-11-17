/**
 * Scheduler Widget - Time-based automation scheduling
 * Configure scheduled actions for devices
 */
import { useState } from 'react'
import { Box, Typography, Paper, Button, Chip, IconButton } from '@mui/material'
import {
  Add,
  Delete,
  PlayArrow,
  Pause,
  Schedule as ScheduleIcon,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface ScheduledTask {
  id: string
  name: string
  time: string
  days: string[]
  action: string
  enabled: boolean
}

function Scheduler({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  const [tasks, setTasks] = useState<ScheduledTask[]>([
    {
      id: '1',
      name: 'Morning Lights ON',
      time: '07:00',
      days: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
      action: 'Turn ON',
      enabled: true,
    },
    {
      id: '2',
      name: 'Evening Lights OFF',
      time: '22:00',
      days: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      action: 'Turn OFF',
      enabled: true,
    },
    {
      id: '3',
      name: 'Weekend Temperature',
      time: '09:00',
      days: ['Sat', 'Sun'],
      action: 'Set 72°F',
      enabled: false,
    },
  ])

  const dayAbbreviations = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

  const handleToggle = (taskId: string) => {
    setTasks((prevTasks) =>
      prevTasks.map((task) =>
        task.id === taskId ? { ...task, enabled: !task.enabled } : task
      )
    )
    console.log('Task toggled:', taskId)
  }

  const handleDelete = (taskId: string) => {
    setTasks((prevTasks) => prevTasks.filter((task) => task.id !== taskId))
    console.log('Task deleted:', taskId)
  }

  const handleAddNew = () => {
    console.log('Add new schedule')
    // TODO: Open schedule creation dialog
  }

  const getNextRun = (task: ScheduledTask): string => {
    const now = new Date()
    const [hours, minutes] = task.time.split(':').map(Number)

    // Simple calculation (actual implementation would be more complex)
    const today = dayAbbreviations[now.getDay() === 0 ? 6 : now.getDay() - 1]
    if (task.days.includes(today)) {
      const taskTime = new Date(now)
      taskTime.setHours(hours, minutes, 0, 0)

      if (taskTime > now) {
        const diff = taskTime.getTime() - now.getTime()
        const hoursLeft = Math.floor(diff / 3600000)
        const minutesLeft = Math.floor((diff % 3600000) / 60000)
        return `in ${hoursLeft}h ${minutesLeft}m`
      }
    }

    return 'Tomorrow'
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'Scheduler'}
          </Typography>
          <Button size="small" startIcon={<Add />} variant="contained" onClick={handleAddNew} sx={{ bgcolor: '#0F3E5C' }}>
            Add
          </Button>
        </Box>
      )}

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {tasks.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <ScheduleIcon sx={{ fontSize: 48, color: '#E0E0E0', mb: 1 }} />
            <Typography variant="body2" color="text.secondary">
              No scheduled tasks
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {tasks.map((task) => (
              <Box
                key={task.id}
                sx={{
                  p: 2,
                  borderRadius: 2,
                  border: `2px solid ${task.enabled ? '#0F3E5C' : '#E0E0E0'}`,
                  bgcolor: task.enabled ? 'rgba(15, 62, 92, 0.05)' : '#F5F5F5',
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                  },
                }}
              >
                {/* Header */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: '#0F3E5C', mb: 0.5 }}>
                      {task.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
                      Action: {task.action}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    <IconButton
                      size="small"
                      onClick={() => handleToggle(task.id)}
                      sx={{ color: task.enabled ? '#2E7D6F' : '#757575' }}
                    >
                      {task.enabled ? <Pause fontSize="small" /> : <PlayArrow fontSize="small" />}
                    </IconButton>
                    <IconButton size="small" onClick={() => handleDelete(task.id)} sx={{ color: '#C62828' }}>
                      <Delete fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>

                {/* Time and Days */}
                <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 1 }}>
                  <Chip
                    label={task.time}
                    size="small"
                    sx={{
                      bgcolor: '#0F3E5C',
                      color: 'white',
                      fontWeight: 'bold',
                      fontSize: '12px',
                    }}
                  />
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                    {dayAbbreviations.map((day) => (
                      <Chip
                        key={day}
                        label={day}
                        size="small"
                        sx={{
                          fontSize: '9px',
                          height: 20,
                          bgcolor: task.days.includes(day) ? '#2E7D6F' : '#E0E0E0',
                          color: task.days.includes(day) ? 'white' : '#757575',
                          fontWeight: task.days.includes(day) ? 'bold' : 'normal',
                        }}
                      />
                    ))}
                  </Box>
                </Box>

                {/* Next Run */}
                {task.enabled && (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <ScheduleIcon sx={{ fontSize: 12, color: '#757575' }} />
                    <Typography variant="caption" color="text.secondary" sx={{ fontSize: '10px' }}>
                      Next run: {getNextRun(task)}
                    </Typography>
                  </Box>
                )}
              </Box>
            ))}
          </Box>
        )}
      </Box>

      {/* Footer Stats */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '11px' }}>
            Total: {tasks.length} tasks
          </Typography>
          <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold', color: '#2E7D6F' }}>
            Active: {tasks.filter((t) => t.enabled).length}
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'scheduler',
  name: 'Scheduler',
  description: 'Time-based automation and task scheduling',
  type: 'rpc',
  tags: ['control', 'scheduler', 'automation', 'timer', 'cron'],
}

registerWidget(descriptor, Scheduler)
export default Scheduler
