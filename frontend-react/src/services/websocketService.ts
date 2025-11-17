/**
 * WebSocket Service
 * Handles real-time telemetry, attributes, and alarm subscriptions
 * Matches ThingsBoard's WebSocket protocol
 */

type SubscriptionCallback = (data: any) => void

interface Subscription {
  id: string
  entityType: string
  entityId: string
  keys: string[]
  callback: SubscriptionCallback
}

interface WebSocketCommand {
  cmdId: number
  entityType?: string
  entityId?: string
  keys?: string[]
  scope?: string
}

class WebSocketService {
  private ws: WebSocket | null = null
  private subscriptions: Map<string, Subscription> = new Map()
  private cmdId = 0
  private reconnectTimeout: NodeJS.Timeout | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 2000
  private isConnecting = false

  // WebSocket URL (configurable)
  private wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/api/ws'

  /**
   * Connect to WebSocket server
   */
  connect(token?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        console.log('[WebSocket] Already connected')
        resolve()
        return
      }

      if (this.isConnecting) {
        console.log('[WebSocket] Connection already in progress')
        return
      }

      this.isConnecting = true
      const url = token ? `${this.wsUrl}?token=${token}` : this.wsUrl

      try {
        this.ws = new WebSocket(url)

        this.ws.onopen = () => {
          console.log('[WebSocket] Connected successfully')
          this.isConnecting = false
          this.reconnectAttempts = 0
          this.resubscribeAll()
          resolve()
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.ws.onerror = (error) => {
          console.error('[WebSocket] Error:', error)
          this.isConnecting = false
          reject(error)
        }

        this.ws.onclose = () => {
          console.log('[WebSocket] Connection closed')
          this.isConnecting = false
          this.ws = null
          this.handleReconnect()
        }
      } catch (error) {
        console.error('[WebSocket] Failed to create connection:', error)
        this.isConnecting = false
        reject(error)
      }
    })
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }

    this.subscriptions.clear()
    console.log('[WebSocket] Disconnected')
  }

  /**
   * Subscribe to telemetry updates
   */
  subscribeTelemetry(
    entityType: string,
    entityId: string,
    keys: string[],
    callback: SubscriptionCallback
  ): string {
    const subscriptionId = `telemetry_${entityType}_${entityId}_${keys.join(',')}`

    const subscription: Subscription = {
      id: subscriptionId,
      entityType,
      entityId,
      keys,
      callback,
    }

    this.subscriptions.set(subscriptionId, subscription)

    // Send subscription command if connected
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.sendCommand({
        cmdId: ++this.cmdId,
        entityType,
        entityId,
        keys,
      })
    }

    console.log(`[WebSocket] Subscribed to telemetry: ${subscriptionId}`)
    return subscriptionId
  }

  /**
   * Subscribe to attribute updates
   */
  subscribeAttributes(
    entityType: string,
    entityId: string,
    keys: string[],
    scope: string,
    callback: SubscriptionCallback
  ): string {
    const subscriptionId = `attributes_${scope}_${entityType}_${entityId}_${keys.join(',')}`

    const subscription: Subscription = {
      id: subscriptionId,
      entityType,
      entityId,
      keys,
      callback,
    }

    this.subscriptions.set(subscriptionId, subscription)

    if (this.ws?.readyState === WebSocket.OPEN) {
      this.sendCommand({
        cmdId: ++this.cmdId,
        entityType,
        entityId,
        keys,
        scope,
      })
    }

    console.log(`[WebSocket] Subscribed to attributes: ${subscriptionId}`)
    return subscriptionId
  }

  /**
   * Unsubscribe from updates
   */
  unsubscribe(subscriptionId: string): void {
    if (this.subscriptions.has(subscriptionId)) {
      this.subscriptions.delete(subscriptionId)
      console.log(`[WebSocket] Unsubscribed: ${subscriptionId}`)
    }
  }

  /**
   * Unsubscribe all subscriptions
   */
  unsubscribeAll(): void {
    this.subscriptions.clear()
    console.log('[WebSocket] Unsubscribed from all')
  }

  /**
   * Handle incoming WebSocket messages
   */
  private handleMessage(data: string): void {
    try {
      const message = JSON.parse(data)

      // Match message to subscriptions and call callbacks
      this.subscriptions.forEach((subscription) => {
        // Simple matching logic - in production, match based on message structure
        if (message.data) {
          subscription.callback(message.data)
        }
      })
    } catch (error) {
      console.error('[WebSocket] Failed to parse message:', error)
    }
  }

  /**
   * Send command to WebSocket server
   */
  private sendCommand(command: WebSocketCommand): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(command))
    } else {
      console.warn('[WebSocket] Cannot send command, not connected')
    }
  }

  /**
   * Resubscribe to all active subscriptions after reconnect
   */
  private resubscribeAll(): void {
    console.log(`[WebSocket] Resubscribing to ${this.subscriptions.size} subscriptions`)

    this.subscriptions.forEach((subscription) => {
      this.sendCommand({
        cmdId: ++this.cmdId,
        entityType: subscription.entityType,
        entityId: subscription.entityId,
        keys: subscription.keys,
      })
    })
  }

  /**
   * Handle automatic reconnection
   */
  private handleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] Max reconnect attempts reached')
      return
    }

    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts)
    this.reconnectAttempts++

    console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

    this.reconnectTimeout = setTimeout(() => {
      console.log('[WebSocket] Attempting reconnection...')
      this.connect().catch((error) => {
        console.error('[WebSocket] Reconnection failed:', error)
      })
    }, delay)
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  /**
   * Get connection state
   */
  getState(): number {
    return this.ws?.readyState ?? WebSocket.CLOSED
  }

  /**
   * Get number of active subscriptions
   */
  getSubscriptionCount(): number {
    return this.subscriptions.size
  }
}

// Export singleton instance
export const websocketService = new WebSocketService()
export default websocketService
