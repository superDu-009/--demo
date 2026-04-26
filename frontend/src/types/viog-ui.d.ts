import type { DefineComponent } from 'vue'

declare module '@vue/runtime-core' {
  export interface GlobalComponents {
    ViButton: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
    ViTag: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
    ViCard: DefineComponent<Record<string, unknown>, Record<string, unknown>, any>
  }
}

export {}
