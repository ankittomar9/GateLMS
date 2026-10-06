import React from 'react';
import katex from 'katex';

interface MathTextProps {
  content?: string | null;
  className?: string;
}

interface Segment {
  type: 'text' | 'inline-math' | 'display-math';
  value: string;
}

function parseSegments(text: string): Segment[] {
  const segments: Segment[] = [];
  // Matches $$...$$ (display math) or $...$ (inline math)
  const regex = /(\$\$[\s\S]+?\$\$|\$[^\$\n]+?\$)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        type: 'text',
        value: text.substring(lastIndex, match.index),
      });
    }

    const matchedStr = match[0];
    if (matchedStr.startsWith('$$') && matchedStr.endsWith('$$')) {
      segments.push({
        type: 'display-math',
        value: matchedStr.slice(2, -2).trim(),
      });
    } else if (matchedStr.startsWith('$') && matchedStr.endsWith('$')) {
      segments.push({
        type: 'inline-math',
        value: matchedStr.slice(1, -1).trim(),
      });
    }

    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    segments.push({
      type: 'text',
      value: text.substring(lastIndex),
    });
  }

  return segments;
}

export const MathText: React.FC<MathTextProps> = ({ content, className = '' }) => {
  if (!content) return null;

  const segments = parseSegments(content);

  return (
    <span className={`inline-block ${className}`}>
      {segments.map((segment, idx) => {
        if (segment.type === 'text') {
          return <span key={idx}>{segment.value}</span>;
        }

        const isDisplay = segment.type === 'display-math';
        try {
          const renderedHtml = katex.renderToString(segment.value, {
            displayMode: isDisplay,
            throwOnError: true,
            strict: false,
          });

          return (
            <span
              key={idx}
              className={isDisplay ? 'block my-3 text-center overflow-x-auto' : 'inline'}
              dangerouslySetInnerHTML={{ __html: renderedHtml }}
            />
          );
        } catch {
          // Fallback chip: neutral styled code element, never exposing unrendered raw LaTeX text
          return (
            <code
              key={idx}
              data-testid="math-fallback"
              className="px-1.5 py-0.5 mx-0.5 rounded text-xs bg-zinc-800 text-zinc-400 font-mono border border-zinc-700 select-none inline-block"
              title="Mathematical expression could not be rendered"
            >
              [Math Error]
            </code>
          );
        }
      })}
    </span>
  );
};

export default MathText;
