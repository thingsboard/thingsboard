import { useState } from 'react'
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
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from '@mui/material'
import { Save as SaveIcon, Send as SendIcon } from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

interface MailServerSettings {
  mailFrom: string
  smtpProtocol: 'smtp' | 'smtps'
  smtpHost: string
  smtpPort: number
  timeout: number
  enableTls: boolean
  username: string
  password: string
  tlsVersion: string
  enableProxy: boolean
}

export default function MailServerPage() {
  const [settings, setSettings] = useState<MailServerSettings>({
    mailFrom: 'noreply@payvar.com',
    smtpProtocol: 'smtp',
    smtpHost: 'localhost',
    smtpPort: 25,
    timeout: 10000,
    enableTls: false,
    username: '',
    password: '',
    tlsVersion: 'TLSv1.2',
    enableProxy: false,
  })

  const [testEmail, setTestEmail] = useState('')
  const [saved, setSaved] = useState(false)
  const [testSent, setTestSent] = useState(false)

  const handleChange = (field: keyof MailServerSettings, value: any) => {
    setSettings((prev) => ({ ...prev, [field]: value }))
  }

  const handleSave = () => {
    // TODO: Call API to save settings
    console.log('Saving mail server settings:', settings)
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  const handleSendTest = () => {
    if (!testEmail) return
    // TODO: Call API to send test email
    console.log('Sending test email to:', testEmail)
    setTestSent(true)
    setTimeout(() => setTestSent(false), 3000)
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom sx={{ mb: 3, color: '#0F3E5C', fontWeight: 600 }}>
          Mail Server - Payvar
        </Typography>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Mail server settings saved successfully!
        </Alert>
      )}

      {testSent && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Test email sent successfully!
        </Alert>
      )}

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          SMTP Configuration
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Mail From"
              value={settings.mailFrom}
              onChange={(e) => handleChange('mailFrom', e.target.value)}
              helperText="Sender email address"
              required
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <FormControl fullWidth>
              <InputLabel>SMTP Protocol</InputLabel>
              <Select
                value={settings.smtpProtocol}
                label="SMTP Protocol"
                onChange={(e) => handleChange('smtpProtocol', e.target.value)}
              >
                <MenuItem value="smtp">SMTP</MenuItem>
                <MenuItem value="smtps">SMTPS</MenuItem>
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="SMTP Host"
              value={settings.smtpHost}
              onChange={(e) => handleChange('smtpHost', e.target.value)}
              required
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="SMTP Port"
              type="number"
              value={settings.smtpPort}
              onChange={(e) => handleChange('smtpPort', parseInt(e.target.value))}
              inputProps={{ min: 1, max: 65535 }}
              required
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Timeout (ms)"
              type="number"
              value={settings.timeout}
              onChange={(e) => handleChange('timeout', parseInt(e.target.value))}
              helperText="Connection timeout in milliseconds"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="TLS Version"
              value={settings.tlsVersion}
              onChange={(e) => handleChange('tlsVersion', e.target.value)}
              helperText="e.g., TLSv1.2, TLSv1.3"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Username"
              value={settings.username}
              onChange={(e) => handleChange('username', e.target.value)}
              helperText="SMTP authentication username"
            />
          </Grid>

          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Password"
              type="password"
              value={settings.password}
              onChange={(e) => handleChange('password', e.target.value)}
              helperText="SMTP authentication password"
            />
          </Grid>

          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Switch checked={settings.enableTls} onChange={(e) => handleChange('enableTls', e.target.checked)} />
              }
              label="Enable TLS"
            />
          </Grid>

          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Switch
                  checked={settings.enableProxy}
                  onChange={(e) => handleChange('enableProxy', e.target.checked)}
                />
              }
              label="Enable Proxy"
            />
          </Grid>
        </Grid>
      </Paper>

      {/* Test Email */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          Test Email
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={8}>
            <TextField
              fullWidth
              label="Test Email Address"
              value={testEmail}
              onChange={(e) => setTestEmail(e.target.value)}
              placeholder="test@example.com"
              type="email"
            />
          </Grid>
          <Grid item xs={12} md={4}>
            <Button
              variant="outlined"
              startIcon={<SendIcon />}
              onClick={handleSendTest}
              disabled={!testEmail}
              fullWidth
              sx={{ color: '#0F3E5C', borderColor: '#0F3E5C' }}
            >
              Send Test Email
            </Button>
          </Grid>
        </Grid>
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
