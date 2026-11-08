/**
 * NewsReader 注入 Shadow DOM 的基础阅读样式。
 * 颜色全部取语义 token：CSS 变量会从宿主继承进 Shadow DOM，随资讯中心皮肤取值。
 * 正文保留的行内排版样式只在 Shadow DOM 内生效；文字颜色和字号由这里统一给出。
 */
export const NEWS_READER_STYLE = `
:host {
  display: block;
  min-width: 0;
  color: var(--main-color);
  font-size: 15px;
  line-height: 26px;
  letter-spacing: 0.5px;
  overflow-wrap: anywhere;
}

.reader > :first-child { margin-top: 0; }
.reader > :last-child { margin-bottom: 0; }

p, ul, ol, blockquote, pre, table, figure, video, audio, iframe { margin: 0 0 12px; }

h1, h2, h3, h4, h5, h6 { margin: 24px 0 12px; color: var(--main-color); letter-spacing: 0; }
h1 { font-size: 22px; line-height: 30px; font-weight: 600; }
h2 { font-size: 20px; line-height: 28px; font-weight: 600; }
h3 { font-size: 18px; line-height: 26px; font-weight: 600; }
h4 { font-size: 17px; line-height: 26px; font-weight: 500; }
h5, h6 { font-size: 15px; line-height: 26px; font-weight: 600; }

strong, b { font-weight: 600; }

a { color: var(--brand-indigo); text-decoration: none; }
a:hover { color: var(--on-brand-container); text-decoration: underline; }
a:focus-visible { outline: 2px solid var(--focus-ring); outline-offset: 1px; border-radius: 2px; }

ul, ol { padding-left: 24px; }
li + li { margin-top: 4px; }
li > ul, li > ol { margin: 4px 0 0; }

blockquote {
  padding: 4px 0 4px 16px;
  border-left: 3px solid var(--surface-border);
  color: var(--second-color);
}

code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.9em;
  letter-spacing: 0;
}
:not(pre) > code {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--surface-container-high);
}
pre {
  padding: 12px 16px;
  overflow-x: auto;
  border-radius: 12px;
  background: var(--surface-container);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 20px;
  letter-spacing: 0;
  white-space: pre;
}
pre code { font-size: inherit; }
.hljs { color: inherit; background: transparent; }
pre code.hljs, pre.hljs { padding: 0; overflow-x: visible; }

table {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
  font-size: 14px;
  line-height: 22px;
}
th, td {
  padding: 8px 12px;
  border: 1px solid var(--surface-border);
  text-align: left;
  vertical-align: top;
}
th { background: var(--surface-container); font-weight: 500; }

img {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  vertical-align: middle;
  cursor: zoom-in;
}
figcaption {
  margin-top: 8px;
  color: var(--second-color);
  font-size: 13px;
  line-height: 20px;
  text-align: center;
}

video, audio { display: block; width: 100%; }
video {
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  background: var(--media-backdrop);
}
iframe {
  display: block;
  width: 100% !important;
  height: auto;
  aspect-ratio: 16 / 9;
  border: 0;
  border-radius: 12px;
  background: var(--media-backdrop);
}

hr { margin: 24px 0; border: 0; border-top: 1px solid var(--surface-border); }
`
