/**
 * Command Console Widget - Terminal-style command interface
 * Execute device commands with history and auto-complete
 */
import { useState, useRef, useEffect } from 'react'
import { Box, Typography, Paper, TextField, Chip } from '@mui/material'
import { Terminal, CheckCircle, Error as ErrorIcon } from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface CommandHistory {
  command: string
  timestamp: number
  output: string
  success: boolean
}

function CommandConsole({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [command, setCommand] = useState('')
  const [history, setHistory] = useState<CommandHistory[]>([])
  const [historyIndex, setHistoryIndex] = useState(-1)
  const outputRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to bottom
  useEffect(() => {
    if (outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight
    }
  }, [history])

  const executeCommand = (cmd: string) => {
    if (!cmd.trim()) return

    // Simulate command execution
    const commands: { [key: string]: { output: string; success: boolean } } = {
      help: {
        output: 'Available commands:\n  status - Show device status\n  restart - Restart device\n  config - Show configuration\n  telemetry - Get telemetry data\n  clear - Clear console',
        success: true,
      },
      status: {
        output: 'Device Status: ONLINE\nUptime: 72h 15m\nMemory: 45% used\nCPU: 23%',
        success: true,
      },
      restart: {
        output: 'Device restart initiated...\nRestarting services...\nDevice online',
        success: true,
      },
      config: {
        output: 'Configuration:\n  MQTT Broker: mqtt://localhost:1883\n  Report Interval: 30s\n  Debug Mode: OFF',
        success: true,
      },
      telemetry: {
        output: 'Latest Telemetry:\n  temperature: 24.5°C\n  humidity: 65%\n  pressure: 1013 hPa',
        success: true,
      },
      clear: {
        output: '',
        success: true,
      },
    }

    const result = commands[cmd.toLowerCase()] || {
      output: `Error: Unknown command '${cmd}'. Type 'help' for available commands.`,
      success: false,
    }

    if (cmd.toLowerCase() === 'clear') {
      setHistory([])
    } else {
      setHistory((prev) => [
        ...prev,
        {
          command: cmd,
          timestamp: Date.now(),
          output: result.output,
          success: result.success,
        },
      ])
    }

    setCommand('')
    setHistoryIndex(-1)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      executeCommand(command)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      const commandHistory = history.map((h) => h.command)
      if (commandHistory.length > 0) {
        const newIndex = historyIndex + 1
        if (newIndex < commandHistory.length) {
          setHistoryIndex(newIndex)
          setCommand(commandHistory[commandHistory.length - 1 - newIndex])
        }
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (historyIndex > 0) {
        const newIndex = historyIndex - 1
        setHistoryIndex(newIndex)
        const commandHistory = history.map((h) => h.command)
        setCommand(commandHistory[commandHistory.length - 1 - newIndex])
      } else {
        setHistoryIndex(-1)
        setCommand('')
      }
    }
  }

  return (
    <Paper
      elevation={1}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: '#1E1E1E',
        color: '#D4D4D4',
      }}
    >
      {config.showTitle && (
        <Box sx={{ p: 2, pb: 1, borderBottom: '1px solid #3C3C3C', display: 'flex', alignItems: 'center', gap: 1 }}>
          <Terminal sx={{ fontSize: 18, color: '#2E7D6F' }} />
          <Typography variant="h6" sx={{ fontSize: '14px', fontWeight: 'bold', color: '#D4D4D4' }}>
            {config.title || 'Command Console'}
          </Typography>
          <Box sx={{ ml: 'auto', display: 'flex', gap: 1 }}>
            <Chip label={`${history.length} commands`} size="small" sx={{ bgcolor: '#3C3C3C', color: '#D4D4D4', fontSize: '10px', height: 20 }} />
          </Box>
        </Box>
      )}

      {/* Output area */}
      <Box
        ref={outputRef}
        sx={{
          flex: 1,
          overflow: 'auto',
          p: 2,
          fontFamily: '"Fira Code", "Consolas", "Monaco", monospace',
          fontSize: '13px',
          lineHeight: 1.6,
        }}
      >
        {/* Welcome message */}
        {history.length === 0 && (
          <Box sx={{ color: '#858585', mb: 2 }}>
            <Typography sx={{ fontSize: '13px', mb: 1 }}>ThingsBoard Device Console v1.0</Typography>
            <Typography sx={{ fontSize: '12px' }}>Type 'help' for available commands</Typography>
          </Box>
        )}

        {/* Command history */}
        {history.map((item, index) => (
          <Box key={index} sx={{ mb: 2 }}>
            {/* Command line */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
              <Typography sx={{ color: '#2E7D6F', fontSize: '13px' }}>{'>'}</Typography>
              <Typography sx={{ color: '#D4D4D4', fontSize: '13px', fontWeight: 'bold' }}>{item.command}</Typography>
              <Typography sx={{ color: '#858585', fontSize: '11px', ml: 'auto' }}>
                {new Date(item.timestamp).toLocaleTimeString()}
              </Typography>
            </Box>

            {/* Output */}
            {item.output && (
              <Box
                sx={{
                  pl: 2,
                  color: item.success ? '#D4D4D4' : '#F48771',
                  whiteSpace: 'pre-wrap',
                  fontSize: '12px',
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 0.5 }}>
                  {item.success ? (
                    <CheckCircle sx={{ fontSize: 12, color: '#2E7D6F', mt: 0.3 }} />
                  ) : (
                    <ErrorIcon sx={{ fontSize: 12, color: '#F48771', mt: 0.3 }} />
                  )}
                  <Typography sx={{ fontSize: '12px', flex: 1 }}>{item.output}</Typography>
                </Box>
              </Box>
            )}
          </Box>
        ))}
      </Box>

      {/* Input area */}
      <Box sx={{ p: 2, borderTop: '1px solid #3C3C3C', bgcolor: '#252526' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography sx={{ color: '#2E7D6F', fontWeight: 'bold', fontSize: '13px' }}>{'>'}</Typography>
          <TextField
            fullWidth
            value={command}
            onChange={(e) => setCommand(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Enter command..."
            variant="standard"
            autoFocus
            sx={{
              '& .MuiInput-root': {
                color: '#D4D4D4',
                fontFamily: '"Fira Code", "Consolas", "Monaco", monospace',
                fontSize: '13px',
                '&:before': { borderBottom: 'none' },
                '&:after': { borderBottom: 'none' },
                '&:hover:not(.Mui-disabled):before': { borderBottom: 'none' },
              },
              '& .MuiInput-input': {
                padding: 0,
              },
            }}
          />
        </Box>
        <Typography sx={{ color: '#858585', fontSize: '10px', mt: 1 }}>
          Press ↑/↓ for history, Enter to execute
        </Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'command_console',
  name: 'Command Console',
  description: 'Terminal-style command interface for device control',
  type: 'rpc',
  tags: ['console', 'terminal', 'command', 'cli', 'debug'],
}

registerWidget(descriptor, CommandConsole)
export default CommandConsole
