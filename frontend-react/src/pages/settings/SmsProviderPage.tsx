import { useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Divider,
  Alert,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Tabs,
  Tab,
} from '@mui/material'
import { Save as SaveIcon, Send as SendIcon } from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

type SmsProviderType = 'aws-sns' | 'twilio' | 'smpp'

interface AwsSnsSettings {
  accessKeyId: string
  secretAccessKey: string
  region: string
}

interface TwilioSettings {
  accountSid: string
  authToken: string
  phoneNumber: string
}

interface SmppSettings {
  host: string
  port: number
  systemId: string
  password: string
  systemType: string
}

export default function SmsProviderPage() {
  const [providerType, setProviderType] = useState<SmsProviderType>('twilio')
  const [awsSettings, setAwsSettings] = useState<AwsSnsSettings>({
    accessKeyId: '',
    secretAccessKey: '',
    region: 'us-east-1',
  })
  const [twilioSettings, setTwilioSettings] = useState<TwilioSettings>({
    accountSid: '',
    authToken: '',
    phoneNumber: '',
  })
  const [smppSettings, setSmppSettings] = useState<SmppSettings>({
    host: '',
    port: 2775,
    systemId: '',
    password: '',
    systemType: '',
  })

  const [testPhone, setTestPhone] = useState('')
  const [saved, setSaved] = useState(false)
  const [testSent, setTestSent] = useState(false)

  const handleSave = () => {
    const settings = {
      providerType,
      ...(providerType === 'aws-sns' && { awsSettings }),
      ...(providerType === 'twilio' && { twilioSettings }),
      ...(providerType === 'smpp' && { smppSettings }),
    }
    console.log('Saving SMS provider settings:', settings)
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  const handleSendTest = () => {
    if (!testPhone) return
    console.log('Sending test SMS to:', testPhone)
    setTestSent(true)
    setTimeout(() => setTestSent(false), 3000)
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom sx={{ mb: 3, color: '#0F3E5C', fontWeight: 600 }}>
          SMS Provider - Payvar
        </Typography>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          SMS provider settings saved successfully!
        </Alert>
      )}

      {testSent && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Test SMS sent successfully!
        </Alert>
      )}

      <Paper sx={{ p: 3, mb: 3 }}>
        <FormControl fullWidth sx={{ mb: 3 }}>
          <InputLabel>SMS Provider</InputLabel>
          <Select
            value={providerType}
            label="SMS Provider"
            onChange={(e) => setProviderType(e.target.value as SmsProviderType)}
          >
            <MenuItem value="twilio">Twilio</MenuItem>
            <MenuItem value="aws-sns">AWS SNS</MenuItem>
            <MenuItem value="smpp">SMPP</MenuItem>
          </Select>
        </FormControl>

        <Divider sx={{ mb: 3 }} />

        {providerType === 'twilio' && (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" sx={{ color: '#0F3E5C', mb: 2 }}>
                Twilio Configuration
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Account SID"
                value={twilioSettings.accountSid}
                onChange={(e) => setTwilioSettings({ ...twilioSettings, accountSid: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Auth Token"
                type="password"
                value={twilioSettings.authToken}
                onChange={(e) => setTwilioSettings({ ...twilioSettings, authToken: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Phone Number"
                value={twilioSettings.phoneNumber}
                onChange={(e) => setTwilioSettings({ ...twilioSettings, phoneNumber: e.target.value })}
                helperText="Twilio phone number (e.g., +12345678900)"
                required
              />
            </Grid>
          </Grid>
        )}

        {providerType === 'aws-sns' && (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" sx={{ color: '#0F3E5C', mb: 2 }}>
                AWS SNS Configuration
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Access Key ID"
                value={awsSettings.accessKeyId}
                onChange={(e) => setAwsSettings({ ...awsSettings, accessKeyId: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Secret Access Key"
                type="password"
                value={awsSettings.secretAccessKey}
                onChange={(e) => setAwsSettings({ ...awsSettings, secretAccessKey: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Region"
                value={awsSettings.region}
                onChange={(e) => setAwsSettings({ ...awsSettings, region: e.target.value })}
                helperText="AWS region (e.g., us-east-1)"
                required
              />
            </Grid>
          </Grid>
        )}

        {providerType === 'smpp' && (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" sx={{ color: '#0F3E5C', mb: 2 }}>
                SMPP Configuration
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Host"
                value={smppSettings.host}
                onChange={(e) => setSmppSettings({ ...smppSettings, host: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Port"
                type="number"
                value={smppSettings.port}
                onChange={(e) => setSmppSettings({ ...smppSettings, port: parseInt(e.target.value) })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="System ID"
                value={smppSettings.systemId}
                onChange={(e) => setSmppSettings({ ...smppSettings, systemId: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Password"
                type="password"
                value={smppSettings.password}
                onChange={(e) => setSmppSettings({ ...smppSettings, password: e.target.value })}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="System Type"
                value={smppSettings.systemType}
                onChange={(e) => setSmppSettings({ ...smppSettings, systemType: e.target.value })}
              />
            </Grid>
          </Grid>
        )}
      </Paper>

      {/* Test SMS */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          Test SMS
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={8}>
            <TextField
              fullWidth
              label="Test Phone Number"
              value={testPhone}
              onChange={(e) => setTestPhone(e.target.value)}
              placeholder="+1234567890"
              type="tel"
            />
          </Grid>
          <Grid item xs={12} md={4}>
            <Button
              variant="outlined"
              startIcon={<SendIcon />}
              onClick={handleSendTest}
              disabled={!testPhone}
              fullWidth
              sx={{ color: '#0F3E5C', borderColor: '#0F3E5C' }}
            >
              Send Test SMS
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
