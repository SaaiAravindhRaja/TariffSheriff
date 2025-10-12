import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { 
  Download, 
  FileText, 
  Code, 
  Globe, 
  FileSpreadsheet,
  X,
  Settings,
  User,
  Calendar,
  Tag,
  Palette
} from 'lucide-react';
import { ChatMessage } from './ChatMessage';
import { exportService, ExportOptions } from '@/services/ExportService';

interface ExportDialogProps {
  isOpen: boolean;
  onClose: () => void;
  messages: ChatMessage[];
  selectedMessage?: ChatMessage;
  title?: string;
}

const exportFormats = [
  {
    format: 'html' as const,
    icon: Globe,
    label: 'HTML',
    description: 'Web page with styling',
    extension: '.html'
  },
  {
    format: 'markdown' as const,
    icon: FileText,
    label: 'Markdown',
    description: 'Plain text with formatting',
    extension: '.md'
  },
  {
    format: 'json' as const,
    icon: Code,
    label: 'JSON',
    description: 'Structured data format',
    extension: '.json'
  },
  {
    format: 'csv' as const,
    icon: FileSpreadsheet,
    label: 'CSV',
    description: 'Spreadsheet compatible',
    extension: '.csv'
  },
  {
    format: 'pdf' as const,
    icon: FileText,
    label: 'PDF',
    description: 'Printable document',
    extension: '.pdf'
  }
];

export const ExportDialog: React.FC<ExportDialogProps> = ({
  isOpen,
  onClose,
  messages,
  selectedMessage,
  title: defaultTitle,
}) => {
  const [selectedFormat, setSelectedFormat] = useState<ExportOptions['format']>('html');
  const [exportTitle, setExportTitle] = useState(
    defaultTitle || (selectedMessage ? 'Single Message Export' : 'Conversation Export')
  );
  const [author, setAuthor] = useState('');
  const [includeMetadata, setIncludeMetadata] = useState(true);
  const [includeTimestamps, setIncludeTimestamps] = useState(true);
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [isExporting, setIsExporting] = useState(false);

  if (!isOpen) return null;

  const handleExport = async () => {
    setIsExporting(true);
    
    try {
      const options: ExportOptions = {
        format: selectedFormat,
        title: exportTitle,
        author: author || undefined,
        includeMetadata,
        includeTimestamps,
        theme,
      };

      const exportData = selectedMessage 
        ? await exportService.exportMessage(selectedMessage, options)
        : await exportService.exportConversation(messages, options);

      // Generate filename
      const timestamp = new Date().toISOString().split('T')[0];
      const baseFilename = exportTitle
        .replace(/[^a-z0-9]/gi, '-')
        .toLowerCase()
        .substring(0, 50);
      const filename = `${baseFilename}-${timestamp}${exportFormats.find(f => f.format === selectedFormat)?.extension}`;

      // Determine MIME type
      const mimeTypes = {
        html: 'text/html',
        markdown: 'text/markdown',
        json: 'application/json',
        csv: 'text/csv',
        pdf: 'application/pdf'
      };

      // Download the file
      exportService.downloadFile(
        exportData,
        filename,
        mimeTypes[selectedFormat]
      );

      onClose();
    } catch (error) {
      console.error('Export failed:', error);
      // You could show an error toast here
    } finally {
      setIsExporting(false);
    }
  };

  const exportCount = selectedMessage ? 1 : messages.length;
  const selectedFormatInfo = exportFormats.find(f => f.format === selectedFormat);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-hidden">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Download size={20} />
              Export {selectedMessage ? 'Message' : 'Conversation'}
            </CardTitle>
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              className="h-6 w-6 p-0"
            >
              <X size={16} />
            </Button>
          </div>
          <p className="text-sm text-muted-foreground">
            Export {exportCount} message{exportCount !== 1 ? 's' : ''} in your preferred format
          </p>
        </CardHeader>
        
        <CardContent className="space-y-6 overflow-y-auto">
          {/* Format Selection */}
          <div>
            <Label className="text-sm font-medium mb-3 block">Export Format</Label>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {exportFormats.map((format) => {
                const Icon = format.icon;
                return (
                  <Button
                    key={format.format}
                    variant={selectedFormat === format.format ? 'default' : 'outline'}
                    onClick={() => setSelectedFormat(format.format)}
                    className="h-auto p-3 flex flex-col items-center text-center"
                  >
                    <Icon size={20} className="mb-2" />
                    <span className="font-medium text-sm">{format.label}</span>
                    <span className="text-xs text-muted-foreground">
                      {format.description}
                    </span>
                  </Button>
                );
              })}
            </div>
            {selectedFormatInfo && (
              <div className="mt-2 p-3 bg-muted/50 rounded-lg">
                <p className="text-sm text-muted-foreground">
                  <strong>{selectedFormatInfo.label}:</strong> {selectedFormatInfo.description}
                </p>
              </div>
            )}
          </div>

          {/* Export Options */}
          <div className="space-y-4">
            <Label className="text-sm font-medium flex items-center gap-2">
              <Settings size={16} />
              Export Options
            </Label>
            
            {/* Title */}
            <div>
              <Label htmlFor="export-title" className="text-sm">Title</Label>
              <Input
                id="export-title"
                value={exportTitle}
                onChange={(e) => setExportTitle(e.target.value)}
                placeholder="Enter export title..."
                className="mt-1"
              />
            </div>

            {/* Author */}
            <div>
              <Label htmlFor="export-author" className="text-sm">Author (optional)</Label>
              <Input
                id="export-author"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                placeholder="Your name..."
                className="mt-1"
              />
            </div>

            {/* Theme (for HTML exports) */}
            {selectedFormat === 'html' && (
              <div>
                <Label className="text-sm flex items-center gap-2 mb-2">
                  <Palette size={16} />
                  Theme
                </Label>
                <div className="flex gap-2">
                  <Button
                    variant={theme === 'light' ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => setTheme('light')}
                    className="flex-1"
                  >
                    Light
                  </Button>
                  <Button
                    variant={theme === 'dark' ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => setTheme('dark')}
                    className="flex-1"
                  >
                    Dark
                  </Button>
                </div>
              </div>
            )}

            {/* Include Options */}
            <div className="space-y-3">
              <Label className="text-sm">Include in Export</Label>
              
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Calendar size={16} className="text-muted-foreground" />
                  <span className="text-sm">Timestamps</span>
                </div>
                <button
                  onClick={() => setIncludeTimestamps(!includeTimestamps)}
                  className={cn(
                    "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                    includeTimestamps ? "bg-primary" : "bg-muted"
                  )}
                >
                  <span
                    className={cn(
                      "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                      includeTimestamps ? "translate-x-6" : "translate-x-1"
                    )}
                  />
                </button>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Tag size={16} className="text-muted-foreground" />
                  <span className="text-sm">Metadata (tools, processing time)</span>
                </div>
                <button
                  onClick={() => setIncludeMetadata(!includeMetadata)}
                  className={cn(
                    "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                    includeMetadata ? "bg-primary" : "bg-muted"
                  )}
                >
                  <span
                    className={cn(
                      "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                      includeMetadata ? "translate-x-6" : "translate-x-1"
                    )}
                  />
                </button>
              </div>
            </div>
          </div>

          {/* Preview */}
          <div className="p-3 bg-muted/50 rounded-lg">
            <Label className="text-sm font-medium mb-2 block">Export Preview</Label>
            <div className="text-sm text-muted-foreground space-y-1">
              <div>Format: <Badge variant="secondary">{selectedFormatInfo?.label}</Badge></div>
              <div>Messages: <strong>{exportCount}</strong></div>
              <div>Includes: {[
                includeTimestamps && 'Timestamps',
                includeMetadata && 'Metadata',
                selectedFormat === 'html' && `${theme} theme`
              ].filter(Boolean).join(', ')}</div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-4 border-t">
            <Button
              variant="outline"
              onClick={onClose}
              className="flex-1"
              disabled={isExporting}
            >
              Cancel
            </Button>
            <Button
              onClick={handleExport}
              className="flex-1"
              disabled={isExporting || !exportTitle.trim()}
            >
              {isExporting ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                  Exporting...
                </>
              ) : (
                <>
                  <Download size={16} className="mr-2" />
                  Export {selectedFormatInfo?.label}
                </>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

ExportDialog.displayName = 'ExportDialog';