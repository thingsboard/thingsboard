/**
 * Node Library Panel
 * Displays available rule nodes categorized by type
 */

import React, { useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  TextField,
  InputAdornment,
  Chip,
} from '@mui/material'
import {
  ExpandMore,
  Search,
  FilterList,
  PlaylistAdd,
  Transform,
  FlashOn,
  CloudUpload,
  SettingsEthernet,
} from '@mui/icons-material'
import {
  RULE_NODE_COMPONENTS,
  RuleNodeType,
  RuleNodeComponentDescriptor,
  ruleNodeTypeDescriptors,
} from '../../types/rulechain.types'

interface NodeLibraryProps {
  onNodeSelect: (component: RuleNodeComponentDescriptor) => void
}

const iconMap: Record<string, React.ElementType> = {
  FilterList,
  PlaylistAdd,
  Transform,
  FlashOn,
  CloudUpload,
  SettingsEthernet,
}

export default function NodeLibrary({ onNodeSelect }: NodeLibraryProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [expandedTypes, setExpandedTypes] = useState<RuleNodeType[]>([
    RuleNodeType.FILTER,
    RuleNodeType.ACTION,
  ])

  // Group nodes by type
  const nodesByType = RULE_NODE_COMPONENTS.reduce(
    (acc, component) => {
      if (!acc[component.type]) {
        acc[component.type] = []
      }
      acc[component.type].push(component)
      return acc
    },
    {} as Record<RuleNodeType, RuleNodeComponentDescriptor[]>
  )

  // Filter nodes based on search
  const filteredNodesByType = Object.entries(nodesByType).reduce(
    (acc, [type, nodes]) => {
      const filtered = nodes.filter(
        (node) =>
          node.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          node.configurationDescriptor?.nodeDefinition.description
            .toLowerCase()
            .includes(searchQuery.toLowerCase())
      )
      if (filtered.length > 0) {
        acc[type as RuleNodeType] = filtered
      }
      return acc
    },
    {} as Record<RuleNodeType, RuleNodeComponentDescriptor[]>
  )

  const handleAccordionChange = (type: RuleNodeType) => {
    setExpandedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    )
  }

  const onDragStart = (event: React.DragEvent, component: RuleNodeComponentDescriptor) => {
    event.dataTransfer.setData('application/reactflow', JSON.stringify(component))
    event.dataTransfer.effectAllowed = 'move'
  }

  return (
    <Paper
      sx={{
        width: 320,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: 0,
        borderRight: '1px solid',
        borderColor: 'divider',
      }}
    >
      {/* Header */}
      <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="h6" gutterBottom>
          Node Library
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Drag nodes onto the canvas to build your rule chain
        </Typography>
        <TextField
          fullWidth
          size="small"
          placeholder="Search nodes..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search fontSize="small" />
              </InputAdornment>
            ),
          }}
          sx={{ mt: 1 }}
        />
      </Box>

      {/* Node List */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {Object.entries(filteredNodesByType).map(([type, nodes]) => {
          const typeKey = type as RuleNodeType
          const descriptor = ruleNodeTypeDescriptors[typeKey]
          const IconComponent = iconMap[descriptor.icon] || FilterList

          return (
            <Accordion
              key={type}
              expanded={expandedTypes.includes(typeKey)}
              onChange={() => handleAccordionChange(typeKey)}
              disableGutters
              elevation={0}
              sx={{
                '&:before': { display: 'none' },
                borderBottom: '1px solid',
                borderColor: 'divider',
              }}
            >
              <AccordionSummary
                expandIcon={<ExpandMore />}
                sx={{
                  bgcolor: `${descriptor.color}08`,
                  '&:hover': {
                    bgcolor: `${descriptor.color}15`,
                  },
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flex: 1 }}>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      width: 36,
                      height: 36,
                      borderRadius: 1,
                      bgcolor: descriptor.color,
                      color: 'white',
                    }}
                  >
                    <IconComponent sx={{ fontSize: 20 }} />
                  </Box>
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="subtitle2" fontWeight={600}>
                      {descriptor.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {descriptor.details}
                    </Typography>
                  </Box>
                  <Chip label={nodes.length} size="small" sx={{ bgcolor: descriptor.color, color: 'white', fontSize: '0.75rem' }} />
                </Box>
              </AccordionSummary>
              <AccordionDetails sx={{ p: 0 }}>
                <List dense>
                  {nodes.map((component) => (
                    <ListItem key={component.clazz} disablePadding>
                      <ListItemButton
                        draggable
                        onDragStart={(e) => onDragStart(e, component)}
                        onClick={() => onNodeSelect(component)}
                        sx={{
                          py: 1.5,
                          px: 2,
                          '&:hover': {
                            bgcolor: `${descriptor.color}08`,
                          },
                        }}
                      >
                        <ListItemIcon sx={{ minWidth: 36 }}>
                          <Box
                            sx={{
                              width: 8,
                              height: 8,
                              borderRadius: '50%',
                              bgcolor: descriptor.color,
                            }}
                          />
                        </ListItemIcon>
                        <ListItemText
                          primary={component.name}
                          secondary={component.configurationDescriptor?.nodeDefinition.description}
                          primaryTypographyProps={{
                            variant: 'body2',
                            fontWeight: 500,
                          }}
                          secondaryTypographyProps={{
                            variant: 'caption',
                            sx: {
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              display: '-webkit-box',
                              WebkitLineClamp: 2,
                              WebkitBoxOrient: 'vertical',
                            },
                          }}
                        />
                      </ListItemButton>
                    </ListItem>
                  ))}
                </List>
              </AccordionDetails>
            </Accordion>
          )
        })}
      </Box>

      {/* Footer Stats */}
      <Box
        sx={{
          p: 2,
          borderTop: '1px solid',
          borderColor: 'divider',
          bgcolor: 'background.default',
        }}
      >
        <Typography variant="caption" color="text.secondary">
          {Object.values(filteredNodesByType).reduce((sum, nodes) => sum + nodes.length, 0)} nodes available
        </Typography>
      </Box>
    </Paper>
  )
}
