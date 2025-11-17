/**
 * Funnel Chart Widget - Conversion funnel visualization
 * Shows progressive reduction in data through stages
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

interface FunnelStage {
  name: string
  value: number
  color: string
}

function FunnelChart({ widget, data }: WidgetComponentProps) {
  const config = widget.config

  // Demo funnel data
  const funnelData: FunnelStage[] = config.settings?.stages || [
    { name: 'Website Visits', value: 10000, color: '#0F3E5C' },
    { name: 'Product Views', value: 7500, color: '#2E7D6F' },
    { name: 'Add to Cart', value: 4000, color: '#4CAF50' },
    { name: 'Checkout', value: 2500, color: '#FFB300' },
    { name: 'Purchase', value: 1800, color: '#C62828' },
  ]

  const maxValue = Math.max(...funnelData.map((d) => d.value))

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2 }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'Funnel Chart'}
        </Typography>
      )}

      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 1, px: 2 }}>
        {funnelData.map((stage, index) => {
          const widthPercent = (stage.value / maxValue) * 100
          const conversionRate = index > 0 ? ((stage.value / funnelData[index - 1].value) * 100).toFixed(1) : '100.0'

          return (
            <Box key={stage.name} sx={{ position: 'relative' }}>
              {/* Funnel segment */}
              <Box
                sx={{
                  height: 50,
                  width: `${widthPercent}%`,
                  bgcolor: stage.color,
                  mx: 'auto',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  px: 2,
                  borderRadius: 1,
                  position: 'relative',
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    transform: 'scale(1.02)',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
                  },
                }}
              >
                <Typography
                  variant="body2"
                  sx={{
                    color: 'white',
                    fontWeight: 'bold',
                    fontSize: '13px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {stage.name}
                </Typography>

                <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                  <Typography variant="body2" sx={{ color: 'white', fontWeight: 'bold' }}>
                    {stage.value.toLocaleString()}
                  </Typography>
                  {index > 0 && (
                    <Typography
                      variant="caption"
                      sx={{
                        color: 'rgba(255,255,255,0.9)',
                        bgcolor: 'rgba(0,0,0,0.2)',
                        px: 1,
                        py: 0.3,
                        borderRadius: 0.5,
                        fontSize: '11px',
                      }}
                    >
                      {conversionRate}%
                    </Typography>
                  )}
                </Box>
              </Box>

              {/* Connector line */}
              {index < funnelData.length - 1 && (
                <Box
                  sx={{
                    position: 'absolute',
                    bottom: -8,
                    left: '50%',
                    transform: 'translateX(-50%)',
                    width: 2,
                    height: 8,
                    bgcolor: '#E0E0E0',
                  }}
                />
              )}
            </Box>
          )
        })}
      </Box>

      {/* Summary */}
      <Box sx={{ mt: 2, p: 1.5, bgcolor: '#F5F5F5', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography variant="caption" color="text.secondary">
            Total Conversion:
          </Typography>
          <Typography variant="caption" sx={{ fontWeight: 'bold', color: '#0F3E5C' }}>
            {((funnelData[funnelData.length - 1].value / funnelData[0].value) * 100).toFixed(1)}%
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'funnel_chart',
  name: 'Funnel Chart',
  description: 'Conversion funnel visualization with stage breakdown',
  type: 'timeseries',
  tags: ['chart', 'funnel', 'conversion', 'analytics'],
}

registerWidget(descriptor, FunnelChart)
export default FunnelChart
