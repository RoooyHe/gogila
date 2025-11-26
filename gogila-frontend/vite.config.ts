import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:7099',
                changeOrigin: true
            },
            '/ws': {
                target: 'ws://localhost:7099',
                ws: true
            }
        }
    }
});