/**
 * Rule Chain Editor Page
 * Full-page editor for a single rule chain
 */

import { useState } from 'react'
import { Box, IconButton, Tooltip } from '@mui/material'
import { ArrowBack } from '@mui/icons-material'
import { useNavigate, useParams } from 'react-router-dom'
import { Node, Edge } from 'reactflow'
import RuleChainEditor from '../../components/rulechain/RuleChainEditor'

export default function RuleChainEditorPage() {
  const navigate = useNavigate()
  const { ruleChainId } = useParams<{ ruleChainId: string }>()
  const [ruleChainName] = useState('My Rule Chain')

  const handleSave = (nodes: Node[], edges: Edge[]) => {
    console.log('Saving rule chain:', { id: ruleChainId, nodes, edges })
    // TODO: Implement API call to save rule chain
    // Example:
    // const metadata = {
    //   ruleChainId,
    //   nodes: nodes.map((node, index) => ({
    //     ...node.data,
    //     additionalInfo: { layoutX: node.position.x, layoutY: node.position.y }
    //   })),
    //   connections: edges.map(edge => ({
    //     fromIndex: nodes.findIndex(n => n.id === edge.source),
    //     toIndex: nodes.findIndex(n => n.id === edge.target),
    //     type: edge.label || 'Success'
    //   }))
    // }
    // await api.saveRuleChainMetadata(metadata)
  }

  const handleTest = () => {
    console.log('Testing rule chain:', ruleChainId)
    // TODO: Implement rule chain testing
  }

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Back Button */}
      <Box
        sx={{
          position: 'absolute',
          top: 16,
          left: 16,
          zIndex: 1000,
        }}
      >
        <Tooltip title="Back to Rule Chains">
          <IconButton
            onClick={() => navigate('/rulechains')}
            sx={{
              bgcolor: 'background.paper',
              boxShadow: 2,
              '&:hover': {
                boxShadow: 4,
              },
            }}
          >
            <ArrowBack />
          </IconButton>
        </Tooltip>
      </Box>

      {/* Editor */}
      <Box sx={{ flex: 1 }}>
        <RuleChainEditor
          ruleChainName={ruleChainName}
          debugMode={false}
          onSave={handleSave}
          onTest={handleTest}
        />
      </Box>
    </Box>
  )
}
