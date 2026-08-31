import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

/**
 * Pipe that converts markdown text to safe HTML.
 * Handles **bold**, *italic*, lists, code blocks, links, headers, etc.
 * Uses DomSanitizer to prevent XSS while allowing rich formatting.
 */
@Pipe({
  name: 'markdown',
  standalone: true,
})
export class MarkdownPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    if (!value) return '';

    const html = marked.parse(value, { breaks: true, gfm: true }) as string;

    // Sanitize the HTML to prevent XSS
    const sanitized = this.sanitizer.sanitize(SecurityContext.HTML, html);
    return sanitized ?? '';
  }
}
