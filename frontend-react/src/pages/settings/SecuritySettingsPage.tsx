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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import { Save as SaveIcon } from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

interface SecuritySettings {
  maxFailedLoginAttempts: number
  userLockoutNotificationEmail: string
  baseUrl: string
  jwtTokenExpirationTime: number
  jwtRefreshTokenExpirationTime: number
  allowedOrigins: string
  passwordPolicy: {
    minimumLength: number
    minimumUppercaseLetters: number
    minimumLowercaseLetters: number
    minimumDigits: number
    minimumSpecialCharacters: number
    passwordExpirationPeriodDays: number
    passwordReuseFrequencyDays: number
  }
}

export default function SecuritySettingsPage() {
  const [settings, setSettings] = useState<SecuritySettings>({
    maxFailedLoginAttempts: 5,
    userLockoutNotificationEmail: 'admin@payvar.com',
    baseUrl: 'http://localhost:8080',
    jwtTokenExpirationTime: 9000,
    jwtRefreshTokenExpirationTime: 604800,
    allowedOrigins: '*',
    passwordPolicy: {
      minimumLength: 8,
      minimumUppercaseLetters: 1,
      minimumLowercaseLetters: 1,
      minimumDigits: 1,
      minimumSpecialCharacters: 1,
      passwordExpirationPeriodDays: 0,
      passwordReuseFrequencyDays: 0,
    },
  })

  const [saved, setSaved] = useState(false)

  const handleChange = (field: keyof SecuritySettings, value: any) => {
    setSettings((prev) => ({ ...prev, [field]: value }))
  }

  const handlePasswordPolicyChange = (
    field: keyof SecuritySettings['passwordPolicy'],
    value: any
  ) => {
    setSettings((prev) => ({
      ...prev,
      passwordPolicy: { ...prev.passwordPolicy, [field]: value },
    }))
  }

  const handleSave = () => {
    console.log('Saving security settings:', settings)
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom sx={{ mb: 3, color: '#0F3E5C', fontWeight: 600 }}>
          Security Settings - Payvar
        </Typography>

      {saved && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Security settings saved successfully!
        </Alert>
      )}

      {/* General Security */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          General Security
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Max Failed Login Attempts"
              type="number"
              value={settings.maxFailedLoginAttempts}
              onChange={(e) => handleChange('maxFailedLoginAttempts', parseInt(e.target.value))}
              helperText="Maximum number of failed login attempts before account lockout"
              inputProps={{ min: 1 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="User Lockout Notification Email"
              value={settings.userLockoutNotificationEmail}
              onChange={(e) => handleChange('userLockoutNotificationEmail', e.target.value)}
              helperText="Email address to notify when user account is locked"
              type="email"
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Base URL"
              value={settings.baseUrl}
              onChange={(e) => handleChange('baseUrl', e.target.value)}
              helperText="The base URL for password reset links"
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Allowed Origins"
              value={settings.allowedOrigins}
              onChange={(e) => handleChange('allowedOrigins', e.target.value)}
              helperText="CORS allowed origins (comma-separated or * for all)"
            />
          </Grid>
        </Grid>
      </Paper>

      {/* JWT Settings */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          JWT Token Settings
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="JWT Token Expiration Time (seconds)"
              type="number"
              value={settings.jwtTokenExpirationTime}
              onChange={(e) => handleChange('jwtTokenExpirationTime', parseInt(e.target.value))}
              helperText="Access token expiration time in seconds (default: 9000 = 2.5 hours)"
              inputProps={{ min: 60 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="JWT Refresh Token Expiration Time (seconds)"
              type="number"
              value={settings.jwtRefreshTokenExpirationTime}
              onChange={(e) => handleChange('jwtRefreshTokenExpirationTime', parseInt(e.target.value))}
              helperText="Refresh token expiration time in seconds (default: 604800 = 7 days)"
              inputProps={{ min: 3600 }}
            />
          </Grid>
        </Grid>
      </Paper>

      {/* Password Policy */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          Password Policy
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Minimum Length"
              type="number"
              value={settings.passwordPolicy.minimumLength}
              onChange={(e) => handlePasswordPolicyChange('minimumLength', parseInt(e.target.value))}
              helperText="Minimum password length"
              inputProps={{ min: 6 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Minimum Uppercase Letters"
              type="number"
              value={settings.passwordPolicy.minimumUppercaseLetters}
              onChange={(e) => handlePasswordPolicyChange('minimumUppercaseLetters', parseInt(e.target.value))}
              helperText="Minimum number of uppercase letters"
              inputProps={{ min: 0 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Minimum Lowercase Letters"
              type="number"
              value={settings.passwordPolicy.minimumLowercaseLetters}
              onChange={(e) => handlePasswordPolicyChange('minimumLowercaseLetters', parseInt(e.target.value))}
              helperText="Minimum number of lowercase letters"
              inputProps={{ min: 0 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Minimum Digits"
              type="number"
              value={settings.passwordPolicy.minimumDigits}
              onChange={(e) => handlePasswordPolicyChange('minimumDigits', parseInt(e.target.value))}
              helperText="Minimum number of digits"
              inputProps={{ min: 0 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Minimum Special Characters"
              type="number"
              value={settings.passwordPolicy.minimumSpecialCharacters}
              onChange={(e) => handlePasswordPolicyChange('minimumSpecialCharacters', parseInt(e.target.value))}
              helperText="Minimum number of special characters"
              inputProps={{ min: 0 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Password Expiration Period (days)"
              type="number"
              value={settings.passwordPolicy.passwordExpirationPeriodDays}
              onChange={(e) => handlePasswordPolicyChange('passwordExpirationPeriodDays', parseInt(e.target.value))}
              helperText="Password expiration period in days (0 = never expires)"
              inputProps={{ min: 0 }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Password Reuse Frequency (days)"
              type="number"
              value={settings.passwordPolicy.passwordReuseFrequencyDays}
              onChange={(e) => handlePasswordPolicyChange('passwordReuseFrequencyDays', parseInt(e.target.value))}
              helperText="Minimum days before password can be reused (0 = no restriction)"
              inputProps={{ min: 0 }}
            />
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
