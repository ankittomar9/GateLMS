import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import { Copy, Check } from 'lucide-react';

interface RichMarkdownTextProps {
  content?: string | null;
  className?: string;
}

const CodeBlock: React.FC<{ language?: string; code: string }> = ({ language, code }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="my-2 rounded-ds border border-ds-border bg-ds-surface-input overflow-hidden text-xs">
      <div className="flex items-center justify-between px-3 py-1 bg-ds-surface-raised border-b border-ds-border text-[10px] text-ds-muted font-mono">
        <span>{language || 'code'}</span>
        <button
          type="button"
          onClick={handleCopy}
          className="flex items-center gap-1 hover:text-ds-primary transition-colors cursor-pointer"
          title="Copy code"
          aria-label="Copy code to clipboard"
        >
          {copied ? (
            <>
              <Check className="w-3 h-3 text-ds-success" />
              <span>Copied</span>
            </>
          ) : (
            <>
              <Copy className="w-3 h-3" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>
      <pre className="p-3 overflow-x-auto font-mono text-[11px] leading-relaxed select-text text-ds-primary">
        <code>{code}</code>
      </pre>
    </div>
  );
};

export const RichMarkdownText: React.FC<RichMarkdownTextProps> = React.memo(({ content, className = '' }) => {
  // Normalize single-line display math $$...$$ on its own line so remarkMath parses it as display math,
  // expand inline code fences into fenced code blocks, and normalize bunched steps/lists onto separate lines.
  const processedContent = React.useMemo(() => {
    if (!content) return '';
    let normalized = content.replace(/\r\n/g, '\n');
    normalized = normalized.replace(/(^|\n)[ \t]*\$\$([^\n]+?)\$\$[ \t]*($|\n)/g, '$1\n\n$$\n$2\n$$\n\n$3');

    // 1. Any code fence not starting on a newline gets preceding newline
    normalized = normalized.replace(/([^\n])[ \t]*```/g, '$1\n```');

    // 2. Opening fence with code on same line: ```c #include -> ```c\n#include
    normalized = normalized.replace(/(^|\n)```([a-zA-Z0-9_-]+)[ \t]+([^\n]+)/g, '$1```$2\n$3');

    // 3. Closing fence with text on same line: ``` Step 1 -> ```\n\nStep 1
    normalized = normalized.replace(/(^|\n)```[ \t]+([^\n]+)/g, '$1```\n\n$2');

    // 4. Steps normalization: Step 1:, Step 2:, etc. when bunched on the same line
    normalized = normalized.replace(/([.!?'")])[ \t]+(Step\s+\d+:)/g, '$1\n\n$2');

    // 5. Numbered points normalization: 1. ..., 2. ... when bunched on the same line
    normalized = normalized.replace(/([.!?'")])[ \t]+(\d+\.\s+[A-Z])/g, '$1\n\n$2');

    // 6. Sub-bullet list normalization when bunched on the same line
    normalized = normalized.replace(/:\s+-\s+/g, ':\n\n- ');
    normalized = normalized.replace(/([.!?'")])[ \t]+-\s+/g, '$1\n- ');

    // 7. Output label normalization when bunched on the same line
    normalized = normalized.replace(/([.!?'")])[ \t]+(Output:)/g, '$1\n\n**$2**');

    return normalized;
  }, [content]);

  if (!content) return null;

  return (
    <div className={`rich-markdown ${className || 'text-xs'} leading-relaxed text-ds-primary`}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[
          [
            rehypeKatex,
            {
              throwOnError: false,
              errorColor: '#ef4444',
              trust: false,
            },
          ],
        ]}
        components={{
          h1: ({ children }) => (
            <h1 className="text-base font-bold text-ds-primary mt-3 mb-1.5 pb-1 border-b border-ds-border">
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="text-sm font-bold text-ds-primary mt-3 mb-1">
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="text-xs font-semibold text-ds-primary mt-2 mb-1">
              {children}
            </h3>
          ),
          h4: ({ children }) => (
            <h4 className="text-xs font-medium text-ds-primary mt-1.5 mb-0.5">
              {children}
            </h4>
          ),
          h5: ({ children }) => (
            <h5 className="text-xs font-medium text-ds-primary mt-1 mb-0.5">
              {children}
            </h5>
          ),
          h6: ({ children }) => (
            <h6 className="text-xs font-medium text-ds-muted mt-1 mb-0.5">
              {children}
            </h6>
          ),
          p: ({ children }) => (
            <p className="leading-relaxed text-ds-primary my-1.5">
              {children}
            </p>
          ),
          ul: ({ children }) => (
            <ul className="list-disc list-outside ml-4 my-1.5 space-y-1 text-ds-primary">
              {children}
            </ul>
          ),
          ol: ({ children }) => (
            <ol className="list-decimal list-outside ml-4 my-1.5 space-y-1 text-ds-primary">
              {children}
            </ol>
          ),
          li: ({ children }) => (
            <li className="leading-relaxed text-ds-primary">
              {children}
            </li>
          ),
          strong: ({ children }) => (
            <strong className="font-semibold text-ds-primary">{children}</strong>
          ),
          em: ({ children }) => (
            <em className="italic text-ds-primary">{children}</em>
          ),
          del: ({ children }) => (
            <del className="line-through text-ds-muted">{children}</del>
          ),
          hr: () => (
            <hr className="border-t border-ds-border my-3" />
          ),
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-ds-accent/60 pl-3 my-2 text-xs italic text-ds-muted bg-ds-accent/5 py-1 rounded-r-ds">
              {children}
            </blockquote>
          ),
          table: ({ children }) => (
            <div className="overflow-x-auto my-2 rounded-ds border border-ds-border">
              <table className="w-full border-collapse text-xs text-ds-primary">
                {children}
              </table>
            </div>
          ),
          thead: ({ children }) => (
            <thead className="bg-ds-surface-raised border-b border-ds-border text-[11px] font-semibold">
              {children}
            </thead>
          ),
          tbody: ({ children }) => (
            <tbody className="divide-y divide-ds-border/60">
              {children}
            </tbody>
          ),
          tr: ({ children }) => (
            <tr className="hover:bg-ds-surface-raised/40 transition-colors">
              {children}
            </tr>
          ),
          th: ({ children }) => (
            <th className="p-2 text-left font-semibold text-ds-primary">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="p-2 text-ds-primary">
              {children}
            </td>
          ),
          a: ({ node, href, children, ...props }) => (
            <a
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className="text-ds-accent hover:underline font-medium"
              {...props}
            >
              {children}
            </a>
          ),
          pre: ({ children }) => <>{children}</>,
          code: ({ node, className, children, ...props }) => {
            const match = /language-(\w+)/.exec(className || '');
            const codeString = String(children).replace(/\n$/, '');
            const isBlock = match || Boolean(node?.position && node.position.start.line !== node.position.end.line) || String(children).includes('\n');

            if (isBlock) {
              return <CodeBlock language={match ? match[1] : ''} code={codeString} />;
            }

            return (
              <code
                className="px-1.5 py-0.5 rounded-ds text-[0.9em] bg-ds-surface-raised text-ds-primary font-mono border border-ds-border select-all"
                {...props}
              >
                {children}
              </code>
            );
          },
        }}
      >
        {processedContent}
      </ReactMarkdown>
    </div>
  );
});

RichMarkdownText.displayName = 'RichMarkdownText';
export default RichMarkdownText;
