/**
 * Rule Node Component
 * Visual representation of a rule node in the flow canvas
 */

import React, { memo } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Box, Typography, Paper, IconButton, Tooltip } from '@mui/material'
import {
  FilterList,
  PlaylistAdd,
  Transform,
  FlashOn,
  CloudUpload,
  SettingsEthernet,
  Input as InputIcon,
  HelpOutline,
  Settings,
  BugReport,
} from '@mui/icons-material'
import { RuleNodeType, ruleNodeTypeDescriptors } from '../../types/rulechain.types'

export interface RuleNodeData {
  label: string
  type: RuleNodeType
  componentName: string
  debugMode?: boolean
  onConfigure?: () => void
}

const iconMap: Record<string, React.ElementType> = {
  FilterList,
  PlaylistAdd,
  Transform,
  FlashOn,
  CloudUpload,
  SettingsEthernet,
  Input: InputIcon,
  HelpOutline,
}

function RuleNode({ data, selected }: NodeProps<RuleNodeData>) {
  const descriptor = ruleNodeTypeDescriptors[data.type]
  const IconComponent = iconMap[descriptor.icon] || HelpOutline

  return (
    <Paper
      elevation={selected ? 8 : 3}
      sx={{
        minWidth: 180,
        border: selected ? `2px solid ${descriptor.color}` : `1px solid ${descriptor.color}40`,
        borderRadius: 2,
        bgcolor: 'background.paper',
        transition: 'all 0.2s',
        '&:hover': {
          boxShadow: 6,
        },
      }}
    >
      {/* Input Handle */}
      {data.type !== RuleNodeType.INPUT && (
        <Handle
          type="target"
          position={Position.Left}
          style={{
            width: 12,
            height: 12,
            background: descriptor.color,
            border: '2px solid white',
          }}
        />
      )}

      {/* Node Header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          p: 1.5,
          borderBottom: `2px solid ${descriptor.color}`,
          bgcolor: `${descriptor.color}15`,
        }}
      >
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 32,
            height: 32,
            borderRadius: 1,
            bgcolor: descriptor.color,
            color: 'white',
          }}
        >
          <IconComponent sx={{ fontSize: 20 }} />
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            variant="caption"
            sx={{
              color: 'text.secondary',
              fontSize: '0.65rem',
              textTransform: 'uppercase',
              letterSpacing: 0.5,
            }}
          >
            {descriptor.name}
          </Typography>
          <Typography
            variant="body2"
            sx={{
              fontWeight: 600,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {data.label}
          </Typography>
        </Box>
      </Box>

      {/* Node Actions */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          p: 0.5,
          gap: 0.5,
        }}
      >
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {data.debugMode && (
            <Tooltip title="Debug Mode Enabled">
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 0.5,
                  px: 0.75,
                  py: 0.25,
                  borderRadius: 1,
                  bgcolor: '#FFB300',
                  color: 'white',
                  fontSize: '0.65rem',
                }}
              >
                <BugReport sx={{ fontSize: 14 }} />
                <Typography variant="caption" sx={{ fontSize: '0.65rem', fontWeight: 600 }}>
                  DEBUG
                </Typography>
              </Box>
            </Tooltip>
          )}
        </Box>
        <Tooltip title="Configure">
          <IconButton
            size="small"
            onClick={data.onConfigure}
            sx={{
              width: 24,
              height: 24,
              '&:hover': {
                bgcolor: `${descriptor.color}20`,
                color: descriptor.color,
              },
            }}
          >
            <Settings sx={{ fontSize: 16 }} />
          </IconButton>
        </Tooltip>
      </Box>

      {/* Output Handle */}
      <Handle
        type="source"
        position={Position.Right}
        style={{
          width: 12,
          height: 12,
          background: descriptor.color,
          border: '2px solid white',
        }}
      />
    </Paper>
  )
}

export default memo(RuleNode)
