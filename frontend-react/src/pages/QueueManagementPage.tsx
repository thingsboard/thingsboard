import { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Storage as QueueIcon,
} from '@mui/icons-material'
import MainLayout from '@/components/layout/MainLayout'

interface Queue {
  id: string
  name: string
  topic: string
  partitions: number
  consumerPerPartition: boolean
  packProcessingTimeout: number
  submitStrategy: string
  processingStrategy: string
}

export default function QueueManagementPage() {
  const [queues, setQueues] = useState<Queue[]>([
    {
      id: '1',
      name: 'Main',
      topic: 'tb_rule_engine.main',
      partitions: 10,
      consumerPerPartition: true,
      packProcessingTimeout: 2000,
      submitStrategy: 'BURST',
      processingStrategy: 'SKIP_ALL_FAILURES',
    },
    {
      id: '2',
      name: 'HighPriority',
      topic: 'tb_rule_engine.hp',
      partitions: 10,
      consumerPerPartition: true,
      packProcessingTimeout: 2000,
      submitStrategy: 'BURST',
      processingStrategy: 'RETRY_ALL',
    },
    {
      id: '3',
      name: 'SequentialByOriginator',
      topic: 'tb_rule_engine.sq',
      partitions: 10,
      consumerPerPartition: true,
      packProcessingTimeout: 2000,
      submitStrategy: 'SEQUENTIAL_BY_ORIGINATOR',
      processingStrategy: 'RETRY_ALL',
    },
  ])

  const handleAdd = () => {
    // TODO: Open dialog to add new queue
    console.log('Add new queue')
  }

  const handleEdit = (queue: Queue) => {
    // TODO: Open dialog to edit queue
    console.log('Edit queue:', queue)
  }

  const handleDelete = (id: string) => {
    // TODO: Confirm and delete queue
    console.log('Delete queue:', id)
  }

  return (
    <MainLayout>
      <Box sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4" sx={{ color: '#0F3E5C', fontWeight: 600 }}>
            Queue Management - Payvar
          </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleAdd} sx={{ bgcolor: '#0F3E5C' }}>
          Add Queue
        </Button>
      </Box>

      <Paper sx={{ p: 0 }}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: '#F5F5F5' }}>
                <TableCell sx={{ fontWeight: 600 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <QueueIcon fontSize="small" />
                    Name
                  </Box>
                </TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Topic</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Partitions</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Submit Strategy</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Processing Strategy</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Timeout (ms)</TableCell>
                <TableCell align="right" sx={{ fontWeight: 600 }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {queues.map((queue) => (
                <TableRow key={queue.id} hover>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      {queue.name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip label={queue.topic} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>{queue.partitions}</TableCell>
                  <TableCell>
                    <Chip label={queue.submitStrategy} size="small" color="primary" />
                  </TableCell>
                  <TableCell>
                    <Chip label={queue.processingStrategy} size="small" color="secondary" />
                  </TableCell>
                  <TableCell>{queue.packProcessingTimeout}</TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      onClick={() => handleEdit(queue)}
                      sx={{ color: '#0F3E5C', mr: 1 }}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => handleDelete(queue.id)}
                      sx={{ color: '#C62828' }}
                      disabled={queue.name === 'Main'}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h6" gutterBottom sx={{ color: '#0F3E5C', fontWeight: 600 }}>
          Queue Information
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Queues are used by the Rule Engine to process messages. Each queue has its own topic in Kafka and can have
          multiple partitions for parallel processing.
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          <strong>Submit Strategy:</strong> Defines how messages are submitted to the queue.
        </Typography>
        <Typography variant="body2" color="text.secondary" component="div">
          <ul>
            <li>
              <strong>BURST:</strong> Process messages as fast as possible
            </li>
            <li>
              <strong>BATCH:</strong> Collect messages into batches before processing
            </li>
            <li>
              <strong>SEQUENTIAL:</strong> Process messages one by one
            </li>
            <li>
              <strong>SEQUENTIAL_BY_ORIGINATOR:</strong> Process messages sequentially per device
            </li>
          </ul>
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph sx={{ mt: 2 }}>
          <strong>Processing Strategy:</strong> Defines how to handle processing failures.
        </Typography>
        <Typography variant="body2" color="text.secondary" component="div">
          <ul>
            <li>
              <strong>SKIP_ALL_FAILURES:</strong> Skip failed messages and continue
            </li>
            <li>
              <strong>RETRY_ALL:</strong> Retry failed messages
            </li>
            <li>
              <strong>RETRY_FAILED_AND_TIMED_OUT:</strong> Retry failed and timed out messages
            </li>
          </ul>
        </Typography>
      </Paper>
      </Box>
    </MainLayout>
  )
}
