import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// Vite配置文件
export default defineConfig({
  // 插件配置
  plugins: [react()],
  
  // 路径别名配置
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  
  // 开发服务器配置
  server: {
    // 端口号
    port: 5173,
    // 自动打开浏览器
    open: true,
    // 代理配置
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  
  // 构建配置
  build: {
    // 输出目录
    outDir: 'dist',
    // 生成sourcemap
    sourcemap: false,
    // 代码压缩
    minify: 'esbuild',
    // 分块策略
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          antd: ['antd', '@ant-design/icons']
        }
      }
    }
  }
})
