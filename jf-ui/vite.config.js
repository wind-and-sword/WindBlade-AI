import { defineConfig } from 'vite';

export default defineConfig({
    server: {
        port: 3000,
        open: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/profile': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
    build: {
        outDir: 'src/main/resources/static',
        emptyOutDir: true,
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('highlight.js')) {
                        return 'highlight';
                    }

                    if (id.includes('marked') || id.includes('dompurify')) {
                        return 'markdown';
                    }
                }
            }
        }
    }
});
