/// <reference types="vite/client" />

// Vite客户端类型声明
interface ImportMetaEnv {
  // 应用标题
  readonly VITE_APP_TITLE: string
  // API基础地址
  readonly VITE_API_BASE_URL: string
  // 应用环境
  readonly VITE_APP_ENV: 'development' | 'production' | 'test'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// 声明模块
declare module '*.svg' {
  import * as React from 'react'
  export const ReactComponent: React.FC<React.SVGProps<SVGSVGElement>>
  const src: string
  export default src
}

declare module '*.png' {
  const src: string
  export default src
}

declare module '*.jpg' {
  const src: string
  export default src
}

declare module '*.jpeg' {
  const src: string
  export default src
}

declare module '*.gif' {
  const src: string
  export default src
}

declare module '*.webp' {
  const src: string
  export default src
}

declare module '*.json' {
  const value: any
  export default value
}
