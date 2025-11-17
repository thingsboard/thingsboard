/**
 * Rule Node Component
 * Visual representation of a rule node in the flow canvas
 */

import React, { memo } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Box, Typography, IconButton } from '@mui/material'
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
    <Box
      sx={{
        position: 'relative',
        minWidth: 150,
        maxWidth: 150,
        height: 42,
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        px: '10px',
        py: '5px',
        borderRadius: '5px',
        border: '1px solid #777',
        bgcolor: descriptor.color,
        color: '#333',
        fontSize: '12px',
        lineHeight: '16px',
        boxShadow: selected ? '0 0 10px 6px #51cbee' : 'none',
        transition: 'all 0.2s',
        '&:hover': {
          opacity: 0.85,
        },
      }}
    >
      {/* Input Handle */}
      {data.type !== RuleNodeType.INPUT && (
        <Handle
          type="target"
          position={Position.Left}
          style={{
            width: 14,
            height: 14,
            background: '#ccc',
            border: '1px solid #333',
            borderRadius: '5px',
            left: '-20px',
          }}
        />
      )}

      {/* Icon */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 20,
          minWidth: 20,
          height: 20,
          color: '#333',
        }}
      >
        <IconComponent sx={{ fontSize: 20 }} />
      </Box>

      {/* Text Content */}
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          minWidth: 0,
          maxWidth: '85%',
        }}
      >
        <Typography
          sx={{
            fontSize: '12px',
            lineHeight: '16px',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            color: '#333',
          }}
        >
          {descriptor.name}
        </Typography>
        {data.label && data.label !== descriptor.name && (
          <Typography
            sx={{
              fontSize: '12px',
              lineHeight: '16px',
              fontWeight: 500,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              color: '#333',
            }}
          >
            {data.label}
          </Typography>
        )}
      </Box>

      {/* Edit/Configure Button (appears on hover in Angular) */}
      {data.onConfigure && (
        <IconButton
          size="small"
          onClick={data.onConfigure}
          sx={{
            position: 'absolute',
            top: -10,
            right: -10,
            width: 20,
            height: 20,
            bgcolor: '#f83e05',
            border: '2px solid #fff',
            color: 'white',
            '&:hover': {
              bgcolor: '#d63404',
            },
            '& .MuiSvgIcon-root': {
              fontSize: 14,
            },
          }}
        >
          <Settings />
        </IconButton>
      )}

      {/* Output Handle */}
      <Handle
        type="source"
        position={Position.Right}
        style={{
          width: 14,
          height: 14,
          background: '#ccc',
          border: '1px solid #333',
          borderRadius: '5px',
          right: '-20px',
        }}
      />
    </Box>
  )
}

export default memo(RuleNode)
