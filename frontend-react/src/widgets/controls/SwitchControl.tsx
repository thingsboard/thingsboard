/**
 * Switch Control Widget - Toggle switch for device control
 */
import { Box, Typography, Paper, Switch, FormControlLabel } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'
import { useState } from 'react'

function SwitchControl({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const [value, setValue] = useState(false)

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setValue(event.target.checked)
    console.log('Switch toggled:', event.target.checked)
    // TODO: Send RPC command to device
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3, justifyContent: 'center', alignItems: 'center' }}>
      {config.showTitle && (
        <Typography variant="subtitle1" sx={{ mb: 2, fontSize: '14px', fontWeight: 'bold', color: '#757575' }}>
          {config.title || 'Switch Control'}
        </Typography>
      )}
      <FormControlLabel
        control={<Switch checked={value} onChange={handleChange} size="medium" sx={{ '& .MuiSwitch-switchBase.Mui-checked': { color: '#2E7D6F' }, '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': { backgroundColor: '#2E7D6F' } }} />}
        label={value ? 'ON' : 'OFF'}
        sx={{ '& .MuiFormControlLabel-label': { fontSize: '18px', fontWeight: 'bold', color: value ? '#2E7D6F' : '#757575' } }}
      />
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'switch_control',
  name: 'Switch Control',
  description: 'Toggle switch for device control',
  type: 'rpc',
  tags: ['control', 'switch', 'toggle', 'rpc'],
}

registerWidget(descriptor, SwitchControl)
export default SwitchControl
