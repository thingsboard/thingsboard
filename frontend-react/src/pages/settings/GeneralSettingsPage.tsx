import { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Switch,
  FormControlLabel,
  Grid,
  Divider,
  Alert,
} from '@mui/material'
import { Save as SaveIcon } from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

interface GeneralSettings {
  baseUrl: string
  prohibitDifferentUrl: boolean
}

interface DeviceConnectivityInfo {
  enabled: boolean
  host: string
  port: number
}

interface DeviceConnectivitySettings {
  http: DeviceConnectivityInfo
  https: DeviceConnectivityInfo
  mqtt: DeviceConnectivityInfo
  mqtts: DeviceConnectivityInfo
  coap: DeviceConnectivityInfo
  coaps: DeviceConnectivityInfo
}

export default function GeneralSettingsPage() {
  const [generalSettings, setGeneralSettings] = useState<GeneralSettings>({
    baseUrl: 'http://localhost:8080',
    prohibitDifferentUrl: false,
  })

  const [connectivitySettings, setConnectivitySettings] = useState<DeviceConnectivitySettings>({
    http: { enabled: true, host: 'localhost', port: 8080 },
    https: { enabled: false, host: '', port: 443 },
    mqtt: { enabled: true, host: 'localhost', port: 1883 },
    mqtts: { enabled: false, host: '', port: 8883 },
    coap: { enabled: false, host: '', port: 5683 },
    coaps: { enabled: false, host: '', port: 5684 },
  })

  const [saved, setSaved] = useState(false)

  const handleGeneralSettingChange = (field: keyof GeneralSettings, value: any) => {
    setGeneralSettings((prev) => ({ ...prev, [field]: value }))
  }

  const handleConnectivityChange = (
    protocol: keyof DeviceConnectivitySettings,
    field: keyof DeviceConnectivityInfo,
    value: any
  ) => {
    setConnectivitySettings((prev) => ({
      ...prev,
      [protocol]: { ...prev[protocol], [field]: value },
    }))
  }

  const handleSave = () => {
    // TODO: Call API to save settings
    console.log('Saving general settings:', generalSettings)
    console.log('Saving connectivity settings:', connectivitySettings)
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom sx={{ mb: 3, color: '#0F3E5C', fontWeight: 600 }}>
          General Settings - Payvar
        </Typography>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Settings saved successfully!
        </Alert>
      )}

      {/* General Server Settings */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          General Server Settings
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Base URL"
              value={generalSettings.baseUrl}
              onChange={(e) => handleGeneralSettingChange('baseUrl', e.target.value)}
              helperText="The base URL of the Payvar server"
              required
            />
          </Grid>
          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Switch
                  checked={generalSettings.prohibitDifferentUrl}
                  onChange={(e) => handleGeneralSettingChange('prohibitDifferentUrl', e.target.checked)}
                />
              }
              label="Prohibit different URL"
            />
            <Typography variant="caption" display="block" sx={{ ml: 4, color: 'text.secondary' }}>
              If enabled, the server will reject requests from different URLs
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      {/* Device Connectivity Settings */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          Device Connectivity Settings
        </Typography>
        <Divider sx={{ mb: 3 }} />

        {(Object.keys(connectivitySettings) as Array<keyof DeviceConnectivitySettings>).map((protocol) => (
          <Box key={protocol} sx={{ mb: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={connectivitySettings[protocol].enabled}
                  onChange={(e) => handleConnectivityChange(protocol, 'enabled', e.target.checked)}
                />
              }
              label={protocol.toUpperCase()}
            />
            {connectivitySettings[protocol].enabled && (
              <Grid container spacing={2} sx={{ mt: 1, ml: 4 }}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Host"
                    value={connectivitySettings[protocol].host}
                    onChange={(e) => handleConnectivityChange(protocol, 'host', e.target.value)}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Port"
                    type="number"
                    value={connectivitySettings[protocol].port}
                    onChange={(e) => handleConnectivityChange(protocol, 'port', parseInt(e.target.value))}
                    inputProps={{ min: 1, max: 65535 }}
                  />
                </Grid>
              </Grid>
            )}
          </Box>
        ))}
      </Paper>

      {/* Save Button */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
        <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave} sx={{ bgcolor: '#0F3E5C' }}>
          Save Settings
        </Button>
      </Box>
      </Box>
    </MainLayout>
  )
}
