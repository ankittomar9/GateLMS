import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RichMarkdownText } from '../RichMarkdownText';

describe('RichMarkdownText Component', () => {
  it('G1 renders headings, lists, bold text, code blocks, tables, and blockquotes into real DOM elements', () => {
    const fixture = `
### Concept & Algorithmic Foundation

In computer network routing:
1. **Link-State Routing:**
   - Reliable flooding
   - Synchronized LSDB
2. **Dijkstra's Algorithm:**
   * Single-source shortest path

Here is inline \`route_table\` and a code snippet:

\`\`\`python
def dijkstra(graph, start):
    return distances
\`\`\`

> Note: OSPF is an interior gateway protocol.

| Protocol | Type | Algorithm |
| :--- | :--- | :--- |
| OSPF | Link-State | Dijkstra |
| BGP | Path-Vector | Policy-based |

---
End of summary.
`;

    const { container } = render(<RichMarkdownText content={fixture} />);

    // Check headings
    const heading = screen.getByRole('heading', { level: 3, name: /Concept & Algorithmic Foundation/i });
    expect(heading).toBeDefined();

    // Check strong elements
    expect(screen.getByText('Link-State Routing:')).toBeDefined();
    expect(screen.getByText("Dijkstra's Algorithm:")).toBeDefined();

    // Check lists
    const orderedList = container.querySelector('ol');
    expect(orderedList).not.toBeNull();
    const unorderedLists = container.querySelectorAll('ul');
    expect(unorderedLists.length).toBeGreaterThanOrEqual(2);

    // Check inline code
    const inlineCode = screen.getByText('route_table');
    expect(inlineCode.tagName.toLowerCase()).toBe('code');

    // Check code block
    expect(screen.getByText('python')).toBeDefined();
    expect(screen.getByText(/def dijkstra/)).toBeDefined();

    // Check blockquote
    const quote = container.querySelector('blockquote');
    expect(quote).not.toBeNull();
    expect(quote?.textContent).toContain('Note: OSPF is an interior gateway protocol.');

    // Check table
    const table = container.querySelector('table');
    expect(table).not.toBeNull();
    expect(screen.getByText('OSPF')).toBeDefined();
    expect(screen.getByText('Dijkstra')).toBeDefined();

    // Check horizontal rule
    const hr = container.querySelector('hr');
    expect(hr).not.toBeNull();

    // Assert no raw Markdown artifacts in text
    expect(container.textContent).not.toContain('###');
    expect(container.textContent).not.toContain('|---|');
    expect(container.textContent).not.toContain('**Link-State');
  });

  it('G2 math parity: renders inline math and display math via KaTeX within text and tables', () => {
    const mathFixture = `
The time complexity is $O(|E| + |V| \\log |V|)$ using a Fibonacci heap.

Display equation:

$$
w(u, v) \\ge 0
$$

List with math:
* Step 1: Initialize $d[v] = \\infty$

| Symbol | Meaning |
| :--- | :--- |
| $V$ | Vertices |
| $E$ | Edges |
`;

    const { container } = render(<RichMarkdownText content={mathFixture} />);

    // KaTeX outputs elements with class .katex
    const katexElements = container.querySelectorAll('.katex');
    expect(katexElements.length).toBeGreaterThanOrEqual(4);

    // Display math renders with .katex-display
    const displayMath = container.querySelector('.katex-display');
    expect(displayMath).not.toBeNull();
  });

  it('G3 security (hard gate): escapes raw HTML, neutralizes javascript: URLs, applies noopener noreferrer, and handles malformed math safely', () => {
    const maliciousFixture = `
<script>window.pwned = true;</script>
<img src="x" onerror="alert(1)" />

[Malicious Link](javascript:alert('xss'))
[Safe Link](https://gatelms.org/docs)

Malformed formula: $\\frac{$ invalid math
`;

    const { container } = render(<RichMarkdownText content={maliciousFixture} />);

    // 1. Raw <script> and <img> must NOT be inserted into DOM as executable HTML tags
    expect(container.querySelector('script')).toBeNull();
    expect(container.querySelector('img')).toBeNull();
    // Raw script text is escaped
    expect(container.textContent).toContain('<script>');

    // 2. javascript: URL must be neutralized (href stripped or not executable)
    const links = container.querySelectorAll('a');
    for (const link of links) {
      const href = link.getAttribute('href') || '';
      expect(href.toLowerCase().startsWith('javascript:')).toBe(false);
    }

    // 3. Safe link carries target="_blank" and rel="noopener noreferrer"
    const safeLink = screen.getByRole('link', { name: /Safe Link/i });
    expect(safeLink.getAttribute('target')).toBe('_blank');
    expect(safeLink.getAttribute('rel')).toContain('noopener');
    expect(safeLink.getAttribute('rel')).toContain('noreferrer');

    // 4. Malformed math does NOT crash or blank the message
    expect(container.textContent).toContain('invalid math');
  });

  it('Code block includes copy functionality', async () => {
    const user = userEvent.setup();
    const copySpy = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();

    const codeFixture = `
\`\`\`sql
SELECT * FROM questions;
\`\`\`
`;

    render(<RichMarkdownText content={codeFixture} />);

    const copyBtn = screen.getByRole('button', { name: /Copy code/i });
    expect(copyBtn).toBeDefined();

    await user.click(copyBtn);
    expect(copySpy).toHaveBeenCalledWith('SELECT * FROM questions;');
    expect(screen.getByText('Copied')).toBeDefined();
  });

  it('G4 normalizes inline code fences, bunched steps, and numbered points into distinct blocks', () => {
    const bunchedFixture = `Consider program allocated at base address 0x2000: \`\`\`c #include <stdio.h> int main() { return 0; } \`\`\` Step 1: Evaluate 'val1 = *p1++'. Postfix has higher precedence. Step 2: Evaluate 'val2'. Output: '10 20'. 1. First trap item: - sub-bullet one - sub-bullet two. 2. Second trap item.`;

    const { container } = render(<RichMarkdownText content={bunchedFixture} className="text-sm" />);

    // Assert container receives custom font-size class
    const root = container.querySelector('.rich-markdown');
    expect(root?.className).toContain('text-sm');

    // Code block was split out of the line into a real code block element with language 'c'
    expect(screen.getByText('c')).toBeDefined();
    expect(screen.getByText(/int main\(\)/)).toBeDefined();

    // Steps are separated into distinct paragraphs
    expect(screen.getByText(/Step 1: Evaluate/)).toBeDefined();
    expect(screen.getByText(/Step 2: Evaluate/)).toBeDefined();

    // Output is bolded
    expect(screen.getByText('Output:')).toBeDefined();

    // Numbered traps and bullet points are split into real list elements
    const orderedList = container.querySelector('ol');
    expect(orderedList).not.toBeNull();
    const unorderedList = container.querySelector('ul');
    expect(unorderedList).not.toBeNull();
    expect(screen.getByText(/sub-bullet one/)).toBeDefined();
    expect(screen.getByText(/sub-bullet two/)).toBeDefined();
  });
});
