/**
 * Statistics Card Widget - Multi-metric statistics display
 * Shows aggregated stats with comparisons
 */
import { Box, Typography, Paper, Grid } from '@mui/material'
import {
  TrendingUp,
  TrendingDown,
  Remove as TrendingFlat,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface Statistic {
  label: string
  value: number
  unit: string
  change: number
  period: string
}

function StatisticsCard({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo statistics data
  const stats: Statistic[] = config.settings?.statistics || [
    { label: 'Total Devices', value: 1247, unit: '', change: 12.5, period: 'vs last month' },
    { label: 'Active Users', value: 342, unit: '', change: -3.2, period: 'vs last week' },
    { label: 'Data Points', value: 45.8, unit: 'M', change: 24.1, period: 'vs yesterday' },
    { label: 'Uptime', value: 99.97, unit: '%', change: 0.03, period: 'vs last hour' },
  ]

  const getTrendIcon = (change: number) => {
    if (change > 0.5) return <TrendingUp sx={{ fontSize: 18 }} />
    if (change < -0.5) return <TrendingDown sx={{ fontSize: 18 }} />
    return <TrendingFlat sx={{ fontSize: 18 }} />
  }

  const getTrendColor = (change: number) => {
    if (change > 0.5) return '#2E7D6F'
    if (change < -0.5) return '#C62828'
    return '#757575'
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Statistics Dashboard'}
        </Typography>
      )}

      <Grid container spacing={2} sx={{ flex: 1 }}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} key={index}>
            <Box
              sx={{
                p: 2,
                borderRadius: 2,
                bgcolor: '#F5F5F5',
                border: '2px solid transparent',
                transition: 'all 0.3s ease',
                height: '100%',
                '&:hover': {
                  bgcolor: 'white',
                  borderColor: '#0F3E5C',
                  transform: 'translateY(-2px)',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                },
              }}
            >
              <Typography
                variant="caption"
                sx={{
                  color: '#757575',
                  fontSize: '11px',
                  fontWeight: 'bold',
                  textTransform: 'uppercase',
                  letterSpacing: 0.5,
                }}
              >
                {stat.label}
              </Typography>

              <Box sx={{ display: 'flex', alignItems: 'baseline', mt: 1, mb: 1 }}>
                <Typography
                  variant="h4"
                  sx={{
                    fontWeight: 'bold',
                    color: '#0F3E5C',
                    fontSize: '28px',
                  }}
                >
                  {stat.value.toLocaleString()}
                </Typography>
                {stat.unit && (
                  <Typography
                    variant="body2"
                    sx={{
                      ml: 0.5,
                      color: '#757575',
                      fontSize: '14px',
                    }}
                  >
                    {stat.unit}
                  </Typography>
                )}
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    color: getTrendColor(stat.change),
                  }}
                >
                  {getTrendIcon(stat.change)}
                  <Typography
                    variant="caption"
                    sx={{
                      fontWeight: 'bold',
                      fontSize: '12px',
                      ml: 0.3,
                    }}
                  >
                    {stat.change > 0 ? '+' : ''}{stat.change.toFixed(1)}%
                  </Typography>
                </Box>
                <Typography
                  variant="caption"
                  sx={{
                    color: '#999',
                    fontSize: '11px',
                  }}
                >
                  {stat.period}
                </Typography>
              </Box>
            </Box>
          </Grid>
        ))}
      </Grid>

      {/* Summary footer */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#E3F2FD', borderRadius: 1 }}>
        <Typography variant="caption" sx={{ color: '#0F3E5C', fontSize: '11px', fontWeight: 'bold' }}>
          📊 Overall Performance: {stats.filter(s => s.change > 0).length} metrics improved, {stats.filter(s => s.change < 0).length} declined
        </Typography>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'statistics_card',
  name: 'Statistics Card',
  description: 'Multi-metric statistics with trend indicators',
  type: 'latest',
  tags: ['statistics', 'metrics', 'aggregation', 'trends'],
}

registerWidget(descriptor, StatisticsCard)
export default StatisticsCard
