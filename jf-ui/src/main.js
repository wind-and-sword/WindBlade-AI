import './styles/main.css';
import 'highlight.js/styles/github-dark.css';
import DOMPurify from 'dompurify';
import { Marked } from 'marked';
import { markedHighlight } from 'marked-highlight';
import hljs from 'highlight.js/lib/core';
import plaintext from 'highlight.js/lib/languages/plaintext';
import javascript from 'highlight.js/lib/languages/javascript';
import typescript from 'highlight.js/lib/languages/typescript';
import json from 'highlight.js/lib/languages/json';
import xml from 'highlight.js/lib/languages/xml';
import css from 'highlight.js/lib/languages/css';
import bash from 'highlight.js/lib/languages/bash';
import java from 'highlight.js/lib/languages/java';
import python from 'highlight.js/lib/languages/python';
import sql from 'highlight.js/lib/languages/sql';
import yaml from 'highlight.js/lib/languages/yaml';
import markdown from 'highlight.js/lib/languages/markdown';
import { mcpApi } from './api/mcp';
import { parseSSEResponse } from './utils/sse-parser';

[
    ['plaintext', plaintext],
    ['text', plaintext],
    ['txt', plaintext],
    ['javascript', javascript],
    ['js', javascript],
    ['typescript', typescript],
    ['ts', typescript],
    ['json', json],
    ['xml', xml],
    ['html', xml],
    ['css', css],
    ['bash', bash],
    ['sh', bash],
    ['shell', bash],
    ['java', java],
    ['python', python],
    ['py', python],
    ['sql', sql],
    ['yaml', yaml],
    ['yml', yaml],
    ['markdown', markdown],
    ['md', markdown]
].forEach(([name, language]) => {
    hljs.registerLanguage(name, language);
});

const marked = new Marked(
    {
        breaks: true,
        gfm: true
    },
    markedHighlight({
        langPrefix: 'hljs language-',
        highlight(code, lang) {
            const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext';
            return hljs.highlight(code, { language }).value;
        }
    })
);

const uploadArea = document.getElementById('uploadArea');
const fileInput = document.getElementById('fileInput');
const fileList = document.getElementById('fileList');
const chatHistory = document.getElementById('chat-history');
const questionInput = document.getElementById('questionInput');
const sendBtn = document.getElementById('sendBtn');

let currentFileId = null;

function normalizeMarkdown(content) {
    return content
        .replace(/\r\n?/g, '\n')
        .replace(/^(#{1,6})(\S)/gm, '$1 $2')
        .replace(/^([ \t]*[-*+])(\S)/gm, '$1 $2')
        .replace(/^([ \t]*\d+\.)\s*(\S)/gm, '$1 $2')
        .replace(/^([ \t]*>)(\S)/gm, '$1 $2');
}

function escapeMarkdownText(content) {
    return content.replace(/([\\`*_{}[\]()#+\-.!|>])/g, '\\$1');
}

function renderMarkdown(content) {
    const html = marked.parse(normalizeMarkdown(content));
    return DOMPurify.sanitize(html, {
        USE_PROFILES: { html: true }
    });
}

function setFileItemStatus(item, { icon, text, color }) {
    item.textContent = '';

    const status = document.createElement('span');
    if (color) {
        status.style.color = color;
    }
    status.textContent = `${icon} ${text}`;

    item.appendChild(status);
}

function appendMsg(role, content) {
    const div = document.createElement('div');
    div.className = `msg ${role}`;

    if (role === 'user') {
        div.textContent = content;
    } else {
        div.innerHTML = renderMarkdown(content || '正在思考...');
    }

    chatHistory.appendChild(div);
    chatHistory.scrollTop = chatHistory.scrollHeight;
    return div;
}

function showMessageError(container, message) {
    container.textContent = `❌ 错误: ${message}`;
    container.classList.add('error');
}

async function handleUpload(event) {
    const file = event.target.files[0];
    if (!file) {
        return;
    }

    currentFileId = null;
    questionInput.disabled = true;
    sendBtn.disabled = true;

    const item = document.createElement('div');
    item.className = 'file-item';
    setFileItemStatus(item, {
        icon: '⏳',
        text: `正在分析: ${file.name}`
    });
    fileList.replaceChildren(item);

    try {
        const res = await mcpApi.uploadFile(file);
        currentFileId = res.fileId;
        questionInput.disabled = false;
        sendBtn.disabled = false;

        setFileItemStatus(item, {
            icon: '✅',
            text: `${file.name} (就绪)`,
            color: '#52c41a'
        });

        appendMsg(
            'ai',
            `文件 **${escapeMarkdownText(file.name)}** 解析成功，可以开始提问了。`
        );
    } catch (error) {
        setFileItemStatus(item, {
            icon: '❌',
            text: error.message || '上传失败',
            color: '#ff4d4f'
        });
    } finally {
        fileInput.value = '';
    }
}

function adjustInputHeight() {
    questionInput.style.height = '44px'; // 先恢复默认高度以获取准确的 scrollHeight
    const scrollHeight = questionInput.scrollHeight;
    const newHeight = Math.min(Math.max(scrollHeight, 44), 200);
    questionInput.style.height = `${newHeight}px`;
}

async function sendChat() {
    const text = questionInput.value.trim();
    if (!text || !currentFileId || sendBtn.disabled) {
        return;
    }

    appendMsg('user', text);
    questionInput.value = '';
    questionInput.style.height = '44px'; // 显式重置为初始高度
    sendBtn.disabled = true;

    const aiMsgDiv = appendMsg('ai', '');
    let fullContent = '';
    let isFirstChunk = true;

    try {
        const response = await mcpApi.chatStream(currentFileId, text);

        await parseSSEResponse(response, (chunk) => {
            // 在更新内容前，判断用户是否处于底部附近（留 100px 的阈值）
            const isAtBottom = chatHistory.scrollHeight - chatHistory.scrollTop - chatHistory.clientHeight < 100;

            if (isFirstChunk) {
                aiMsgDiv.classList.remove('error');
                aiMsgDiv.innerHTML = '';
                isFirstChunk = false;
            }

            fullContent += chunk;
            aiMsgDiv.innerHTML = renderMarkdown(fullContent);
            
            // 只有当用户原本就在底部时，才自动随内容增加滚动
            if (isAtBottom) {
                chatHistory.scrollTop = chatHistory.scrollHeight;
            }
        });

        if (!fullContent.trim()) {
            showMessageError(aiMsgDiv, '未收到有效响应');
        }
    } catch (error) {
        showMessageError(aiMsgDiv, error.message || '请求失败');
    } finally {
        sendBtn.disabled = false;
        questionInput.focus();
    }
}

uploadArea.addEventListener('click', () => fileInput.click());
fileInput.addEventListener('change', handleUpload);
sendBtn.addEventListener('click', sendChat);

// 监听输入变化以调整高度
questionInput.addEventListener('input', adjustInputHeight);

questionInput.addEventListener('keydown', (event) => {
    // 只有在按下 Enter 且没有按下 Shift 时才发送
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
        event.preventDefault(); // 阻止默认的回车换行行为
        sendChat();
    }
});
