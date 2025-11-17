/**
 * HTML/Markdown Card Widget - Display custom HTML or Markdown content
 */
import { Box, Typography, Paper } from '@mui/material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function HTMLCard({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const htmlContent = config.settings?.htmlContent || '<h2>HTML Content</h2><p>Add your HTML here in widget settings</p>'

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 2, overflow: 'auto' }}>
      {config.showTitle && (
        <Typography variant="h6" sx={{ mb: 2, fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
          {config.title || 'HTML Card'}
        </Typography>
      )}
      <Box sx={{ flex: 1 }} dangerouslySetInnerHTML={{ __html: htmlContent }} />
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'html_card',
  name: 'HTML Card',
  description: 'Display custom HTML or Markdown content',
  type: 'static',
  tags: ['static', 'html', 'markdown', 'content'],
}

registerWidget(descriptor, HTMLCard)
export default HTMLCard
