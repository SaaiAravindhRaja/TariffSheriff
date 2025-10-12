import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { 
  Share, 
  Mail, 
  Link, 
  Copy,
  MessageSquare,
  Users,
  X,
  Check,
  ExternalLink
} from 'lucide-react';
import { ChatMessage } from './ChatMessage';
import { exportService, ShareOptions } from '@/services/ExportService';

interface ShareDialogProps {
  isOpen: boolean;
  onClose: () => void;
  message: ChatMessage;
}

const sharePlatforms = [
  {
    platform: 'clipboard' as const,
    icon: Copy,
    label: 'Copy to Clipboard',
    description: 'Copy message text to clipboard',
    color: 'bg-gray-100 text-gray-800 hover:bg-gray-200'
  },
  {
    platform: 'link' as const,
    icon: Link,
    label: 'Generate Link',
    description: 'Create a shareable link',
    color: 'bg-blue-100 text-blue-800 hover:bg-blue-200'
  },
  {
    platform: 'email' as const,
    icon: Mail,
    label: 'Email',
    description: 'Share via email',
    color: 'bg-green-100 text-green-800 hover:bg-green-200'
  },
  {
    platform: 'slack' as const,
    icon: MessageSquare,
    label: 'Slack',
    description: 'Share to Slack workspace',
    color: 'bg-purple-100 text-purple-800 hover:bg-purple-200'
  },
  {
    platform: 'teams' as const,
    icon: Users,
    label: 'Microsoft Teams',
    description: 'Share to Teams channel',
    color: 'bg-indigo-100 text-indigo-800 hover:bg-indigo-200'
  }
];

export const ShareDialog: React.FC<ShareDialogProps> = ({
  isOpen,
  onClose,
  message,
}) => {
  const [selectedPlatform, setSelectedPlatform] = useState<ShareOptions['platform']>('clipboard');
  const [customMessage, setCustomMessage] = useState('');
  const [recipients, setRecipients] = useState('');
  const [isSharing, setIsSharing] = useState(false);
  const [shareSuccess, setShareSuccess] = useState(false);
  const [generatedLink, setGeneratedLink] = useState('');

  if (!isOpen) return null;

  const handleShare = async () => {
    setIsSharing(true);
    setShareSuccess(false);
    
    try {
      const options: ShareOptions = {
        platform: selectedPlatform,
        message: customMessage || undefined,
        recipients: recipients ? recipients.split(',').map(r => r.trim()) : undefined,
      };

      if (selectedPlatform === 'link') {
        await exportService.shareMessage(message, options);
        // For now, generate a simple link - in production this would be a real shareable URL
        const link = `${window.location.origin}/shared/${btoa(JSON.stringify({ id: message.id, content: message.content.substring(0, 200) }))}`;
        setGeneratedLink(link);
        await navigator.clipboard.writeText(link);
      } else {
        await exportService.shareMessage(message, options);
      }

      setShareSuccess(true);
      
      // Auto-close after success (except for link generation)
      if (selectedPlatform !== 'link') {
        setTimeout(() => {
          onClose();
        }, 1500);
      }
    } catch (error) {
      console.error('Share failed:', error);
      // You could show an error toast here
    } finally {
      setIsSharing(false);
    }
  };

  const selectedPlatformInfo = sharePlatforms.find(p => p.platform === selectedPlatform);
  const messagePreview = message.content.length > 200 
    ? message.content.substring(0, 200) + '...' 
    : message.content;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-hidden">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Share size={20} />
              Share Message
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
            Share this {message.role === 'user' ? 'question' : 'AI response'} with others
          </p>
        </CardHeader>
        
        <CardContent className="space-y-6 overflow-y-auto">
          {/* Message Preview */}
          <div className="p-3 bg-muted/50 rounded-lg">
            <Label className="text-sm font-medium mb-2 block">Message Preview</Label>
            <div className="text-sm">
              <div className="flex items-center gap-2 mb-2">
                <Badge variant={message.role === 'user' ? 'secondary' : 'default'}>
                  {message.role === 'user' ? 'Your Question' : 'AI Response'}
                </Badge>
                <span className="text-xs text-muted-foreground">
                  {message.timestamp.toLocaleString()}
                </span>
              </div>
              <div className="text-muted-foreground whitespace-pre-wrap">
                {messagePreview}
              </div>
            </div>
          </div>

          {/* Platform Selection */}
          <div>
            <Label className="text-sm font-medium mb-3 block">Share Method</Label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {sharePlatforms.map((platform) => {
                const Icon = platform.icon;
                return (
                  <Button
                    key={platform.platform}
                    variant={selectedPlatform === platform.platform ? 'default' : 'outline'}
                    onClick={() => setSelectedPlatform(platform.platform)}
                    className="h-auto p-3 flex items-center text-left justify-start"
                  >
                    <Icon size={16} className="mr-3 flex-shrink-0" />
                    <div className="flex-1">
                      <div className="font-medium text-sm">{platform.label}</div>
                      <div className="text-xs text-muted-foreground">
                        {platform.description}
                      </div>
                    </div>
                  </Button>
                );
              })}
            </div>
          </div>

          {/* Platform-specific Options */}
          {selectedPlatform === 'email' && (
            <div>
              <Label htmlFor="recipients" className="text-sm">Email Recipients</Label>
              <Input
                id="recipients"
                value={recipients}
                onChange={(e) => setRecipients(e.target.value)}
                placeholder="email1@example.com, email2@example.com"
                className="mt-1"
              />
              <p className="text-xs text-muted-foreground mt-1">
                Separate multiple emails with commas
              </p>
            </div>
          )}

          {/* Custom Message */}
          <div>
            <Label htmlFor="custom-message" className="text-sm">
              Add a Message (optional)
            </Label>
            <Textarea
              id="custom-message"
              value={customMessage}
              onChange={(e) => setCustomMessage(e.target.value)}
              placeholder="Add context or explanation for the shared message..."
              className="mt-1 min-h-[80px]"
              maxLength={500}
            />
            <p className="text-xs text-muted-foreground mt-1">
              {customMessage.length}/500 characters
            </p>
          </div>

          {/* Generated Link Display */}
          {generatedLink && (
            <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
              <Label className="text-sm font-medium text-green-800 mb-2 block">
                Shareable Link Generated
              </Label>
              <div className="flex items-center gap-2">
                <Input
                  value={generatedLink}
                  readOnly
                  className="flex-1 text-sm bg-white"
                />
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigator.clipboard.writeText(generatedLink)}
                  className="flex-shrink-0"
                >
                  <Copy size={14} />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => window.open(generatedLink, '_blank')}
                  className="flex-shrink-0"
                >
                  <ExternalLink size={14} />
                </Button>
              </div>
              <p className="text-xs text-green-700 mt-2">
                Link copied to clipboard! Anyone with this link can view the message.
              </p>
            </div>
          )}

          {/* Success Message */}
          {shareSuccess && selectedPlatform !== 'link' && (
            <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2">
              <Check size={16} className="text-green-600" />
              <span className="text-sm text-green-800">
                Message shared successfully via {selectedPlatformInfo?.label}!
              </span>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-4 border-t">
            <Button
              variant="outline"
              onClick={onClose}
              className="flex-1"
              disabled={isSharing}
            >
              {shareSuccess && selectedPlatform !== 'link' ? 'Close' : 'Cancel'}
            </Button>
            
            {(!shareSuccess || selectedPlatform === 'link') && (
              <Button
                onClick={handleShare}
                className="flex-1"
                disabled={isSharing || (selectedPlatform === 'email' && !recipients.trim())}
              >
                {isSharing ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                    Sharing...
                  </>
                ) : (
                  <>
                    {selectedPlatformInfo?.icon && (
                      <selectedPlatformInfo.icon size={16} className="mr-2" />
                    )}
                    {selectedPlatform === 'link' ? 'Generate Link' : `Share via ${selectedPlatformInfo?.label}`}
                  </>
                )}
              </Button>
            )}
          </div>

          {/* Privacy Notice */}
          <div className="text-xs text-muted-foreground p-3 bg-muted/30 rounded-lg">
            <strong>Privacy Notice:</strong> When sharing messages, ensure you comply with your organization's 
            data sharing policies. Shared content may contain sensitive trade information.
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

ShareDialog.displayName = 'ShareDialog';