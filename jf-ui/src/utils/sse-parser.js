/**
 * 按 SSE 规范解析响应流。
 * 只有遇到空行时才认为一个事件结束，并把同一事件里的多条 data 行拼接成一段文本。
 */
export async function parseSSEResponse(response, onData) {
    if (!response.body) {
        throw new Error('响应体为空，无法解析流式数据');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let currentEventData = [];

    function flushEvent() {
        if (currentEventData.length === 0) {
            return;
        }

        onData(currentEventData.join('\n'));
        currentEventData = [];
    }

    while (true) {
        const { done, value } = await reader.read();

        if (done) {
            buffer += decoder.decode();

            if (buffer) {
                const line = buffer.endsWith('\r') ? buffer.slice(0, -1) : buffer;
                if (line.startsWith('data:')) {
                    currentEventData.push(line.slice(5).replace(/^ /, ''));
                }
            }

            flushEvent();
            break;
        }

        buffer += decoder.decode(value, { stream: true });

        let lineEndIndex;
        while ((lineEndIndex = buffer.indexOf('\n')) >= 0) {
            const rawLine = buffer.slice(0, lineEndIndex);
            buffer = buffer.slice(lineEndIndex + 1);

            const line = rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine;

            if (line === '') {
                flushEvent();
                continue;
            }

            if (line.startsWith('data:')) {
                currentEventData.push(line.slice(5).replace(/^ /, ''));
            }
        }
    }
}
