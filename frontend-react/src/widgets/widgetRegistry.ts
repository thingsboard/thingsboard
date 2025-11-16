/**
 * Widget Registry
 * Central registry for all widget types
 */

import { WidgetTypeDescriptor, WidgetTypeId } from '@/types/dashboard'
import { ComponentType } from 'react'
import type { WidgetComponentProps } from '@/types/dashboard'

class WidgetRegistry {
  private widgets = new Map<
    WidgetTypeId,
    {
      descriptor: WidgetTypeDescriptor
      component: ComponentType<WidgetComponentProps>
    }
  >()

  register(
    descriptor: WidgetTypeDescriptor,
    component: ComponentType<WidgetComponentProps>
  ) {
    this.widgets.set(descriptor.id, { descriptor, component })
  }

  getDescriptor(typeId: WidgetTypeId): WidgetTypeDescriptor | undefined {
    return this.widgets.get(typeId)?.descriptor
  }

  getComponent(
    typeId: WidgetTypeId
  ): ComponentType<WidgetComponentProps> | undefined {
    return this.widgets.get(typeId)?.component
  }

  getAllDescriptors(): WidgetTypeDescriptor[] {
    return Array.from(this.widgets.values()).map((w) => w.descriptor)
  }

  getDescriptorsByType(
    type: 'timeseries' | 'latest' | 'rpc' | 'alarm' | 'static'
  ): WidgetTypeDescriptor[] {
    return Array.from(this.widgets.values())
      .map((w) => w.descriptor)
      .filter((d) => d.type === type)
  }
}

export const widgetRegistry = new WidgetRegistry()

// Helper function to register widgets
export function registerWidget(
  descriptor: WidgetTypeDescriptor,
  component: ComponentType<WidgetComponentProps>
) {
  widgetRegistry.register(descriptor, component)
}
