import { Typography, Paper, Box } from '@mui/material'

export default function DashboardPage() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Paper sx={{ p: 3, mt: 2 }}>
        <Typography>Welcome to ThingsBoard IoT Platform</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          Python backend + React frontend conversion
        </Typography>
      </Paper>
    </Box>
  )
}
