async function readErrorMessage(response) {
    const contentType = response.headers.get('content-type') || '';

    try {
        if (contentType.includes('application/json')) {
            const payload = await response.json();
            return payload.message || payload.msg || `请求失败: ${response.status}`;
        }

        const text = await response.text();
        return text || `请求失败: ${response.status}`;
    } catch {
        return `请求失败: ${response.status}`;
    }
}

async function requestJson(url, options) {
    const response = await fetch(url, options);

    if (!response.ok) {
        throw new Error(await readErrorMessage(response));
    }

    return response.json();
}

export const mcpApi = {
    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);

        return requestJson('/api/mcp/upload', {
            method: 'POST',
            body: formData
        });
    },

    async chatStream(fileId, text) {
        const formData = new FormData();
        formData.append('fileId', fileId);
        formData.append('text', text);

        const response = await fetch('/api/mcp/chat/stream', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(await readErrorMessage(response));
        }

        if (!response.body) {
            throw new Error('当前环境不支持流式响应');
        }

        return response;
    }
};
