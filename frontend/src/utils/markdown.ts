import DOMPurify from 'dompurify'

/**
 * 全站唯一的 Markdown → HTML 出口。
 *
 * 这是设计文档要求的收口点：笔记正文、AI 回答、文档解析结果最终都经 v-html 渲染，
 * 而后两者是不可信输入。所有产出都过一遍 DOMPurify，即便将来某个渲染分支忘了转义，
 * 也不至于把脚本带进页面。
 *
 * 注意「转义在先」这条约定不能放松：下面的渲染器都是先 escapeHtml 再拼固定标签，
 * 所以正常情况下 DOMPurify 什么都不会改；它是纵深防御，不是唯一防线。
 */

/**
 * 允许的标签即「本模块可能产出的全部结构」。
 *
 * 用显式白名单而不是 DOMPurify 的默认值：默认值很宽，而这里该有什么是确定的。
 * 将来渲染器若要产出新标签，必须同时加到这里 —— 忘了加的表现是标签被剥掉，看得见。
 *
 * span 上的 contenteditable 是富文本编辑器的双链 chip 需要的，剥掉它 chip 就散架了。
 */
const ALLOWED_TAGS = [
  'h1', 'h2', 'h3', 'p', 'br',
  'strong', 'em', 's', 'code',
  'span', 'blockquote', 'ul', 'ol', 'li',
  'mark', 'a',
]

const ALLOWED_ATTR = ['class', 'contenteditable', 'href', 'target', 'rel']

/** 清洗一段 HTML。全站所有 v-html 的输入都应经过它 */
export function sanitizeHtml(html: string): string {
  return DOMPurify.sanitize(html, { ALLOWED_TAGS, ALLOWED_ATTR })
}

export function escapeHtml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/** 行内标记：双链、粗体、删除线、行内码、斜体 */
function buildInline(value: string): string {
  return escapeHtml(value)
    .replace(/\[\[(.*?)\]\]/g, '<span class="wikilink-chip" contenteditable="false">$1</span>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/~~(.*?)~~/g, '<s>$1</s>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

export function renderInlineMarkdown(value: string): string {
  return sanitizeHtml(buildInline(value))
}

/** 富文本编辑器用：标题 / 无序列表 / 有序列表 / 引用 / 段落 */
export function renderRichHtml(value: string): string {
  const lines = value.split('\n')
  const blocks: string[] = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]
    if (!line.trim()) {
      index += 1
      continue
    }
    const heading = line.match(/^(#{1,3})\s+(.*)$/)
    if (heading) {
      const level = heading[1].length
      blocks.push(`<h${level}>${buildInline(heading[2])}</h${level}>`)
      index += 1
      continue
    }
    if (line.startsWith('- ')) {
      const items: string[] = []
      while (index < lines.length && lines[index].startsWith('- ')) {
        items.push(`<li>${buildInline(lines[index].slice(2))}</li>`)
        index += 1
      }
      blocks.push(`<ul>${items.join('')}</ul>`)
      continue
    }
    if (/^\d+\.\s/.test(line)) {
      const items: string[] = []
      while (index < lines.length && /^\d+\.\s/.test(lines[index])) {
        items.push(`<li>${buildInline(lines[index].replace(/^\d+\.\s/, ''))}</li>`)
        index += 1
      }
      blocks.push(`<ol>${items.join('')}</ol>`)
      continue
    }
    if (line.startsWith('> ')) {
      blocks.push(`<blockquote>${buildInline(line.slice(2))}</blockquote>`)
      index += 1
      continue
    }
    blocks.push(`<p>${buildInline(line)}</p>`)
    index += 1
  }
  return sanitizeHtml(blocks.join(''))
}

/** 阅读模式用：相对上面的版本更宽松，列表项直接产出裸 li */
export function renderMarkdown(value: string): string {
  return sanitizeHtml(
    escapeHtml(value)
      .replace(/^### (.*)$/gm, '<h3>$1</h3>')
      .replace(/^## (.*)$/gm, '<h2>$1</h2>')
      .replace(/^# (.*)$/gm, '<h1>$1</h1>')
      .replace(/^> (.*)$/gm, '<blockquote>$1</blockquote>')
      .replace(/^- (.*)$/gm, '<li>$1</li>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n{2,}/g, '</p><p>')
      .replace(/\n/g, '<br>')
      .replace(/^/, '<p>')
      .replace(/$/, '</p>')
  )
}
