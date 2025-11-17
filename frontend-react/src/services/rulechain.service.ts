/**
 * Rule Chain API Service
 * Handles all rule chain related API calls
 */

import axios from 'axios'
import {
  RuleChain,
  RuleChainMetadata,
  RuleChainImport,
} from '../types/rulechain.types'

const API_URL = 'http://localhost:8080/api'

class RuleChainService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token')
    return {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    }
  }

  /**
   * Get all rule chains
   */
  async getRuleChains(pageSize: number = 100, page: number = 0): Promise<RuleChain[]> {
    const response = await axios.get(
      `${API_URL}/ruleChains?pageSize=${pageSize}&page=${page}`,
      this.getAuthHeaders()
    )
    return response.data.data || []
  }

  /**
   * Get rule chain by ID
   */
  async getRuleChain(ruleChainId: string): Promise<RuleChain> {
    const response = await axios.get(`${API_URL}/ruleChain/${ruleChainId}`, this.getAuthHeaders())
    return response.data
  }

  /**
   * Get rule chain metadata (nodes and connections)
   */
  async getRuleChainMetadata(ruleChainId: string): Promise<RuleChainMetadata> {
    const response = await axios.get(
      `${API_URL}/ruleChain/${ruleChainId}/metadata`,
      this.getAuthHeaders()
    )
    return response.data
  }

  /**
   * Create new rule chain
   */
  async createRuleChain(ruleChain: Partial<RuleChain>): Promise<RuleChain> {
    const response = await axios.post(`${API_URL}/ruleChain`, ruleChain, this.getAuthHeaders())
    return response.data
  }

  /**
   * Update existing rule chain
   */
  async updateRuleChain(ruleChain: RuleChain): Promise<RuleChain> {
    const response = await axios.post(`${API_URL}/ruleChain`, ruleChain, this.getAuthHeaders())
    return response.data
  }

  /**
   * Save rule chain metadata (nodes and connections)
   */
  async saveRuleChainMetadata(metadata: RuleChainMetadata): Promise<RuleChainMetadata> {
    const response = await axios.post(
      `${API_URL}/ruleChain/metadata`,
      metadata,
      this.getAuthHeaders()
    )
    return response.data
  }

  /**
   * Delete rule chain
   */
  async deleteRuleChain(ruleChainId: string): Promise<void> {
    await axios.delete(`${API_URL}/ruleChain/${ruleChainId}`, this.getAuthHeaders())
  }

  /**
   * Set rule chain as root
   */
  async setRootRuleChain(ruleChainId: string): Promise<RuleChain> {
    const response = await axios.post(
      `${API_URL}/ruleChain/${ruleChainId}/root`,
      {},
      this.getAuthHeaders()
    )
    return response.data
  }

  /**
   * Export rule chain
   */
  async exportRuleChain(ruleChainId: string): Promise<RuleChainImport> {
    const [ruleChain, metadata] = await Promise.all([
      this.getRuleChain(ruleChainId),
      this.getRuleChainMetadata(ruleChainId),
    ])

    return {
      ruleChain,
      metadata,
    }
  }

  /**
   * Import rule chain
   */
  async importRuleChain(ruleChainImport: RuleChainImport): Promise<RuleChain> {
    const response = await axios.post(
      `${API_URL}/ruleChain/import`,
      ruleChainImport,
      this.getAuthHeaders()
    )
    return response.data
  }

  /**
   * Test rule chain with sample data
   */
  async testRuleChain(
    ruleChainId: string,
    msg: any,
    metadata: Record<string, string>,
    msgType: string
  ): Promise<any> {
    const response = await axios.post(
      `${API_URL}/ruleChain/${ruleChainId}/test`,
      {
        msg,
        metadata,
        msgType,
      },
      this.getAuthHeaders()
    )
    return response.data
  }

  /**
   * Get latest rule node component descriptors
   */
  async getRuleNodeComponents(): Promise<any[]> {
    const response = await axios.get(`${API_URL}/ruleNode/components`, this.getAuthHeaders())
    return response.data
  }

  /**
   * Validate rule chain
   */
  async validateRuleChain(metadata: RuleChainMetadata): Promise<{ valid: boolean; errors: string[] }> {
    // Client-side validation
    const errors: string[] = []

    // Check for orphaned nodes (nodes without input except first node)
    const connectedNodeIndices = new Set<number>()
    metadata.connections.forEach((conn) => {
      connectedNodeIndices.add(conn.toIndex)
    })

    metadata.nodes.forEach((node, index) => {
      if (index !== metadata.firstNodeIndex && !connectedNodeIndices.has(index)) {
        errors.push(`Node "${node.name}" (index ${index}) is not connected to any input`)
      }
    })

    // Check for missing first node
    if (metadata.firstNodeIndex === undefined || metadata.firstNodeIndex === null) {
      errors.push('First node index is not set')
    }

    // Check for circular dependencies (simple check)
    const visited = new Set<number>()
    const recursionStack = new Set<number>()

    const hasCycle = (nodeIndex: number): boolean => {
      visited.add(nodeIndex)
      recursionStack.add(nodeIndex)

      const outgoingConnections = metadata.connections.filter((conn) => conn.fromIndex === nodeIndex)
      for (const conn of outgoingConnections) {
        if (!visited.has(conn.toIndex)) {
          if (hasCycle(conn.toIndex)) return true
        } else if (recursionStack.has(conn.toIndex)) {
          return true
        }
      }

      recursionStack.delete(nodeIndex)
      return false
    }

    if (metadata.firstNodeIndex !== undefined && hasCycle(metadata.firstNodeIndex)) {
      errors.push('Rule chain contains circular dependencies')
    }

    return {
      valid: errors.length === 0,
      errors,
    }
  }

  /**
   * Download rule chain as JSON file
   */
  async downloadRuleChain(ruleChainId: string, ruleChainName: string): Promise<void> {
    const ruleChainImport = await this.exportRuleChain(ruleChainId)

    const blob = new Blob([JSON.stringify(ruleChainImport, null, 2)], { type: 'application/json' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${ruleChainName.replace(/\s+/g, '_')}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  /**
   * Upload and import rule chain from JSON file
   */
  async uploadRuleChain(file: File): Promise<RuleChain> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = async (e) => {
        try {
          const content = e.target?.result as string
          const ruleChainImport: RuleChainImport = JSON.parse(content)
          const imported = await this.importRuleChain(ruleChainImport)
          resolve(imported)
        } catch (error) {
          reject(error)
        }
      }
      reader.onerror = reject
      reader.readAsText(file)
    })
  }
}

export const ruleChainService = new RuleChainService()
export default ruleChainService
