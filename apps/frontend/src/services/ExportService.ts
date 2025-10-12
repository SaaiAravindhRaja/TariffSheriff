import { ChatMessage } from '@/components/chat/ChatMessage';

export interface ExportOptions {
  format: 'pdf' | 'html' | 'json' | 'csv' | 'markdown';
  includeMetadata?: boolean;
  includeTimestamps?: boolean;
  title?: string;
  author?: string;
  theme?: 'light' | 'dark';
}

export interface ShareOptions {
  platform: 'email' | 'slack' | 'teams' | 'link' | 'clipboard';
  message?: string;
  recipients?: string[];
}

export interface BookmarkData {
  id: string;
  messageId: string;
  title: string;
  content: string;
  timestamp: Date;
  tags: string[];
  category: string;
}

class ExportService {
  private bookmarks: Map<string, BookmarkData> = new Map();

  // Export single message
  async exportMessage(
    message: ChatMessage, 
    options: ExportOptions
  ): Promise<Blob | string> {
    switch (options.format) {
      case 'json':
        return this.exportAsJSON([message], options);
      case 'html':
        return this.exportAsHTML([message], options);
      case 'markdown':
        return this.exportAsMarkdown([message], options);
      case 'pdf':
        return this.exportAsPDF([message], options);
      case 'csv':
        return this.exportAsCSV([message], options);
      default:
        throw new Error(`Unsupported export format: ${options.format}`);
    }
  }

  // Export conversation
  async exportConversation(
    messages: ChatMessage[], 
    options: ExportOptions
  ): Promise<Blob | string> {
    switch (options.format) {
      case 'json':
        return this.exportAsJSON(messages, options);
      case 'html':
        return this.exportAsHTML(messages, options);
      case 'markdown':
        return this.exportAsMarkdown(messages, options);
      case 'pdf':
        return this.exportAsPDF(messages, options);
      case 'csv':
        return this.exportAsCSV(messages, options);
      default:
        throw new Error(`Unsupported export format: ${options.format}`);
    }
  }

  // JSON Export
  private exportAsJSON(messages: ChatMessage[], options: ExportOptions): string {
    const exportData = {
      title: options.title || 'Trade Copilot Conversation',
      author: options.author || 'AI Trade Copilot',
      exportDate: new Date().toISOString(),
      messageCount: messages.length,
      messages: messages.map(msg => ({
        id: msg.id,
        role: msg.role,
        content: msg.content,
        timestamp: options.includeTimestamps ? msg.timestamp : undefined,
        metadata: options.includeMetadata ? msg.metadata : undefined,
      }))
    };

    return JSON.stringify(exportData, null, 2);
  }

  // HTML Export
  private exportAsHTML(messages: ChatMessage[], options: ExportOptions): string {
    const theme = options.theme || 'light';
    const title = options.title || 'Trade Copilot Conversation';
    
    const styles = `
      <style>
        body { 
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
          line-height: 1.6; 
          max-width: 800px; 
          margin: 0 auto; 
          padding: 20px;
          background: ${theme === 'dark' ? '#1a1a1a' : '#ffffff'};
          color: ${theme === 'dark' ? '#e5e5e5' : '#333333'};
        }
        .header { 
          border-bottom: 2px solid ${theme === 'dark' ? '#333' : '#eee'}; 
          padding-bottom: 20px; 
          margin-bottom: 30px; 
        }
        .message { 
          margin-bottom: 20px; 
          padding: 15px; 
          border-radius: 8px; 
          border-left: 4px solid ${theme === 'dark' ? '#4a90e2' : '#0ea5e9'};
          background: ${theme === 'dark' ? '#2a2a2a' : '#f8f9fa'};
        }
        .user-message { 
          border-left-color: ${theme === 'dark' ? '#22c55e' : '#10b981'}; 
          background: ${theme === 'dark' ? '#1e3a2e' : '#f0fdf4'};
        }
        .message-header { 
          font-weight: bold; 
          margin-bottom: 8px; 
          color: ${theme === 'dark' ? '#4a90e2' : '#0ea5e9'};
        }
        .user-message .message-header { 
          color: ${theme === 'dark' ? '#22c55e' : '#10b981'}; 
        }
        .message-content { 
          white-space: pre-wrap; 
          word-wrap: break-word; 
        }
        .message-meta { 
          font-size: 0.8em; 
          color: ${theme === 'dark' ? '#888' : '#666'}; 
          margin-top: 10px; 
          padding-top: 10px; 
          border-top: 1px solid ${theme === 'dark' ? '#333' : '#eee'};
        }
        .footer { 
          margin-top: 40px; 
          padding-top: 20px; 
          border-top: 2px solid ${theme === 'dark' ? '#333' : '#eee'}; 
          text-align: center; 
          color: ${theme === 'dark' ? '#888' : '#666'}; 
          font-size: 0.9em; 
        }
        code {
          background: ${theme === 'dark' ? '#333' : '#f1f5f9'};
          padding: 2px 4px;
          border-radius: 4px;
          font-family: 'Monaco', 'Menlo', monospace;
        }
        pre {
          background: ${theme === 'dark' ? '#333' : '#f1f5f9'};
          padding: 12px;
          border-radius: 6px;
          overflow-x: auto;
        }
      </style>
    `;

    const messagesHTML = messages.map(msg => {
      const isUser = msg.role === 'user';
      const timestamp = options.includeTimestamps ? 
        `<div class="message-meta">
          <strong>Time:</strong> ${msg.timestamp.toLocaleString()}
          ${msg.metadata?.toolsUsed ? `<br><strong>Tools Used:</strong> ${msg.metadata.toolsUsed.join(', ')}` : ''}
          ${msg.metadata?.processingTime ? `<br><strong>Processing Time:</strong> ${(msg.metadata.processingTime / 1000).toFixed(1)}s` : ''}
        </div>` : '';

      return `
        <div class="message ${isUser ? 'user-message' : ''}">
          <div class="message-header">${isUser ? 'You' : 'AI Trade Copilot'}</div>
          <div class="message-content">${this.formatContentForHTML(msg.content)}</div>
          ${options.includeMetadata ? timestamp : ''}
        </div>
      `;
    }).join('');

    return `
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>
        ${styles}
      </head>
      <body>
        <div class="header">
          <h1>${title}</h1>
          <p><strong>Generated:</strong> ${new Date().toLocaleString()}</p>
          <p><strong>Messages:</strong> ${messages.length}</p>
          ${options.author ? `<p><strong>Author:</strong> ${options.author}</p>` : ''}
        </div>
        
        <div class="messages">
          ${messagesHTML}
        </div>
        
        <div class="footer">
          <p>Generated by AI Trade Copilot</p>
          <p>Powered by advanced AI and real-time trade data</p>
        </div>
      </body>
      </html>
    `;
  }

  // Markdown Export
  private exportAsMarkdown(messages: ChatMessage[], options: ExportOptions): string {
    const title = options.title || 'Trade Copilot Conversation';
    const header = `# ${title}\n\n**Generated:** ${new Date().toLocaleString()}\n**Messages:** ${messages.length}\n${options.author ? `**Author:** ${options.author}\n` : ''}\n---\n\n`;

    const messagesMarkdown = messages.map(msg => {
      const isUser = msg.role === 'user';
      const timestamp = options.includeTimestamps ? `\n*${msg.timestamp.toLocaleString()}*` : '';
      const metadata = options.includeMetadata && msg.metadata ? 
        `\n\n**Metadata:**\n${msg.metadata.toolsUsed ? `- Tools Used: ${msg.metadata.toolsUsed.join(', ')}\n` : ''}${msg.metadata.processingTime ? `- Processing Time: ${(msg.metadata.processingTime / 1000).toFixed(1)}s\n` : ''}` : '';

      return `## ${isUser ? 'You' : 'AI Trade Copilot'}${timestamp}\n\n${msg.content}${metadata}\n\n---\n`;
    }).join('\n');

    return header + messagesMarkdown + '\n*Generated by AI Trade Copilot*';
  }

  // CSV Export
  private exportAsCSV(messages: ChatMessage[], options: ExportOptions): string {
    const headers = ['Role', 'Content', 'Timestamp'];
    if (options.includeMetadata) {
      headers.push('Tools Used', 'Processing Time', 'Cached');
    }

    const rows = messages.map(msg => {
      const row = [
        msg.role,
        `"${msg.content.replace(/"/g, '""')}"`, // Escape quotes
        options.includeTimestamps ? msg.timestamp.toISOString() : ''
      ];

      if (options.includeMetadata) {
        row.push(
          msg.metadata?.toolsUsed?.join('; ') || '',
          msg.metadata?.processingTime?.toString() || '',
          msg.metadata?.cached?.toString() || ''
        );
      }

      return row.join(',');
    });

    return [headers.join(','), ...rows].join('\n');
  }

  // PDF Export (simplified - would need a proper PDF library in production)
  private async exportAsPDF(messages: ChatMessage[], options: ExportOptions): Promise<Blob> {
    // For now, create HTML and suggest using browser's print-to-PDF
    const htmlContent = this.exportAsHTML(messages, options);
    
    // In a real implementation, you would use a library like jsPDF or Puppeteer
    // For now, we'll create a blob that can be opened in a new window for printing
    return new Blob([htmlContent], { type: 'text/html' });
  }

  // Share functionality
  async shareMessage(message: ChatMessage, options: ShareOptions): Promise<void> {
    const content = this.formatMessageForSharing(message, options);
    
    switch (options.platform) {
      case 'clipboard':
        await navigator.clipboard.writeText(content);
        break;
      case 'email':
        this.shareViaEmail(content, options);
        break;
      case 'link':
        return this.generateShareLink(message);
      case 'slack':
        this.shareViaSlack(content, options);
        break;
      case 'teams':
        this.shareViaTeams(content, options);
        break;
      default:
        throw new Error(`Unsupported sharing platform: ${options.platform}`);
    }
  }

  // Bookmark functionality
  bookmarkMessage(message: ChatMessage, title?: string, tags: string[] = []): BookmarkData {
    const bookmark: BookmarkData = {
      id: `bookmark_${Date.now()}`,
      messageId: message.id,
      title: title || `${message.role === 'user' ? 'Query' : 'Response'} - ${new Date().toLocaleDateString()}`,
      content: message.content,
      timestamp: new Date(),
      tags,
      category: this.categorizeMessage(message)
    };

    this.bookmarks.set(bookmark.id, bookmark);
    this.saveBookmarksToStorage();
    return bookmark;
  }

  getBookmarks(): BookmarkData[] {
    return Array.from(this.bookmarks.values()).sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  removeBookmark(bookmarkId: string): void {
    this.bookmarks.delete(bookmarkId);
    this.saveBookmarksToStorage();
  }

  searchBookmarks(query: string): BookmarkData[] {
    const lowercaseQuery = query.toLowerCase();
    return this.getBookmarks().filter(bookmark =>
      bookmark.title.toLowerCase().includes(lowercaseQuery) ||
      bookmark.content.toLowerCase().includes(lowercaseQuery) ||
      bookmark.tags.some(tag => tag.toLowerCase().includes(lowercaseQuery))
    );
  }

  // Helper methods
  private formatContentForHTML(content: string): string {
    return content
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/```([^```]+)```/g, '<pre><code>$1</code></pre>');
  }

  private formatMessageForSharing(message: ChatMessage, options: ShareOptions): string {
    const prefix = options.message ? `${options.message}\n\n` : '';
    const role = message.role === 'user' ? 'Question' : 'AI Response';
    const timestamp = new Date(message.timestamp).toLocaleString();
    
    return `${prefix}**${role}** (${timestamp})\n\n${message.content}\n\n---\nShared from AI Trade Copilot`;
  }

  private shareViaEmail(content: string, options: ShareOptions): void {
    const subject = encodeURIComponent('Trade Analysis from AI Trade Copilot');
    const body = encodeURIComponent(content);
    const recipients = options.recipients?.join(',') || '';
    
    window.open(`mailto:${recipients}?subject=${subject}&body=${body}`);
  }

  private shareViaSlack(content: string, options: ShareOptions): void {
    // In a real implementation, this would use Slack's API or deep linking
    const slackContent = encodeURIComponent(content);
    window.open(`https://slack.com/intl/en-gb/help/articles/201330736-Add-apps-to-your-Slack-workspace?text=${slackContent}`);
  }

  private shareViaTeams(content: string, options: ShareOptions): void {
    // In a real implementation, this would use Microsoft Teams' API
    const teamsContent = encodeURIComponent(content);
    window.open(`https://teams.microsoft.com/share?msgText=${teamsContent}`);
  }

  private generateShareLink(message: ChatMessage): string {
    // In a real implementation, this would create a shareable link
    const messageData = btoa(JSON.stringify({
      id: message.id,
      content: message.content,
      timestamp: message.timestamp
    }));
    
    return `${window.location.origin}/shared/${messageData}`;
  }

  private categorizeMessage(message: ChatMessage): string {
    const content = message.content.toLowerCase();
    
    if (content.includes('tariff') || content.includes('duty')) return 'tariff';
    if (content.includes('hs code') || content.includes('classification')) return 'classification';
    if (content.includes('trade agreement') || content.includes('fta')) return 'agreements';
    if (content.includes('compliance') || content.includes('regulation')) return 'compliance';
    if (content.includes('cost') || content.includes('calculate')) return 'calculation';
    if (content.includes('trend') || content.includes('analysis')) return 'analysis';
    
    return 'general';
  }

  private saveBookmarksToStorage(): void {
    try {
      const bookmarksArray = Array.from(this.bookmarks.entries());
      localStorage.setItem('tradecopilot_bookmarks', JSON.stringify(bookmarksArray));
    } catch (error) {
      console.error('Failed to save bookmarks to storage:', error);
    }
  }

  private loadBookmarksFromStorage(): void {
    try {
      const stored = localStorage.getItem('tradecopilot_bookmarks');
      if (stored) {
        const bookmarksArray = JSON.parse(stored);
        this.bookmarks = new Map(bookmarksArray.map(([id, data]: [string, any]) => [
          id,
          { ...data, timestamp: new Date(data.timestamp) }
        ]));
      }
    } catch (error) {
      console.error('Failed to load bookmarks from storage:', error);
    }
  }

  // Download helper
  downloadFile(content: string | Blob, filename: string, mimeType: string): void {
    const blob = typeof content === 'string' 
      ? new Blob([content], { type: mimeType })
      : content;
    
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  // Initialize service
  constructor() {
    this.loadBookmarksFromStorage();
  }
}

// Export singleton instance
export const exportService = new ExportService();
export default exportService;