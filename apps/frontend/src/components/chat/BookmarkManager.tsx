import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { 
  Bookmark, 
  Search, 
  Trash2, 
  Tag, 
  Calendar,
  Filter,
  Download,
  Share,
  Eye,
  X
} from 'lucide-react';
import { exportService, BookmarkData } from '@/services/ExportService';

interface BookmarkManagerProps {
  className?: string;
  onViewBookmark?: (bookmark: BookmarkData) => void;
}

const categoryColors = {
  tariff: 'bg-blue-100 text-blue-800 border-blue-200',
  classification: 'bg-green-100 text-green-800 border-green-200',
  agreements: 'bg-purple-100 text-purple-800 border-purple-200',
  compliance: 'bg-orange-100 text-orange-800 border-orange-200',
  calculation: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  analysis: 'bg-red-100 text-red-800 border-red-200',
  general: 'bg-gray-100 text-gray-800 border-gray-200',
};

export const BookmarkManager: React.FC<BookmarkManagerProps> = ({
  className,
  onViewBookmark,
}) => {
  const [bookmarks, setBookmarks] = useState<BookmarkData[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedBookmark, setSelectedBookmark] = useState<BookmarkData | null>(null);
  const [showDetails, setShowDetails] = useState(false);

  useEffect(() => {
    loadBookmarks();
  }, []);

  const loadBookmarks = () => {
    const allBookmarks = exportService.getBookmarks();
    setBookmarks(allBookmarks);
  };

  const handleDeleteBookmark = (bookmarkId: string) => {
    if (confirm('Are you sure you want to delete this bookmark?')) {
      exportService.removeBookmark(bookmarkId);
      loadBookmarks();
    }
  };

  const handleExportBookmark = async (bookmark: BookmarkData) => {
    try {
      const content = `# ${bookmark.title}\n\n**Category:** ${bookmark.category}\n**Date:** ${bookmark.timestamp.toLocaleString()}\n**Tags:** ${bookmark.tags.join(', ')}\n\n---\n\n${bookmark.content}`;
      
      exportService.downloadFile(
        content,
        `bookmark-${bookmark.title.replace(/[^a-z0-9]/gi, '-').toLowerCase()}.md`,
        'text/markdown'
      );
    } catch (error) {
      console.error('Failed to export bookmark:', error);
    }
  };

  const handleShareBookmark = async (bookmark: BookmarkData) => {
    try {
      const shareText = `${bookmark.title}\n\n${bookmark.content}\n\n---\nBookmarked from AI Trade Copilot`;
      await navigator.clipboard.writeText(shareText);
      // You could show a toast notification here
    } catch (error) {
      console.error('Failed to share bookmark:', error);
    }
  };

  const filteredBookmarks = bookmarks.filter(bookmark => {
    const matchesSearch = searchTerm === '' || 
      bookmark.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      bookmark.content.toLowerCase().includes(searchTerm.toLowerCase()) ||
      bookmark.tags.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()));
    
    const matchesCategory = selectedCategory === 'all' || bookmark.category === selectedCategory;
    
    return matchesSearch && matchesCategory;
  });

  const categories = ['all', ...Array.from(new Set(bookmarks.map(b => b.category)))];

  const BookmarkCard: React.FC<{ bookmark: BookmarkData }> = ({ bookmark }) => (
    <Card className="hover:shadow-md transition-shadow">
      <CardContent className="p-4">
        <div className="flex items-start justify-between mb-2">
          <h3 className="font-medium text-sm truncate flex-1 mr-2">
            {bookmark.title}
          </h3>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setSelectedBookmark(bookmark);
                setShowDetails(true);
              }}
              className="h-6 w-6 p-0"
            >
              <Eye size={12} />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleShareBookmark(bookmark)}
              className="h-6 w-6 p-0"
            >
              <Share size={12} />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleExportBookmark(bookmark)}
              className="h-6 w-6 p-0"
            >
              <Download size={12} />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleDeleteBookmark(bookmark.id)}
              className="h-6 w-6 p-0 text-destructive hover:text-destructive"
            >
              <Trash2 size={12} />
            </Button>
          </div>
        </div>
        
        <p className="text-xs text-muted-foreground mb-3 line-clamp-2">
          {bookmark.content}
        </p>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Badge 
              className={cn(
                'text-xs',
                categoryColors[bookmark.category as keyof typeof categoryColors] || categoryColors.general
              )}
            >
              {bookmark.category}
            </Badge>
            {bookmark.tags.length > 0 && (
              <div className="flex items-center gap-1">
                <Tag size={10} className="text-muted-foreground" />
                <span className="text-xs text-muted-foreground">
                  {bookmark.tags.slice(0, 2).join(', ')}
                  {bookmark.tags.length > 2 && '...'}
                </span>
              </div>
            )}
          </div>
          
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <Calendar size={10} />
            <span>{bookmark.timestamp.toLocaleDateString()}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  const BookmarkDetails: React.FC<{ bookmark: BookmarkData }> = ({ bookmark }) => (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-2xl max-h-[80vh] overflow-hidden">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg">{bookmark.title}</CardTitle>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowDetails(false)}
              className="h-6 w-6 p-0"
            >
              <X size={16} />
            </Button>
          </div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Calendar size={14} />
            <span>{bookmark.timestamp.toLocaleString()}</span>
            <Badge 
              className={cn(
                'text-xs ml-2',
                categoryColors[bookmark.category as keyof typeof categoryColors] || categoryColors.general
              )}
            >
              {bookmark.category}
            </Badge>
          </div>
        </CardHeader>
        
        <CardContent className="overflow-y-auto">
          <div className="prose prose-sm max-w-none">
            <div className="whitespace-pre-wrap text-sm">
              {bookmark.content}
            </div>
          </div>
          
          {bookmark.tags.length > 0 && (
            <div className="mt-4 pt-4 border-t">
              <div className="flex items-center gap-2 mb-2">
                <Tag size={14} className="text-muted-foreground" />
                <span className="text-sm font-medium">Tags</span>
              </div>
              <div className="flex flex-wrap gap-1">
                {bookmark.tags.map((tag, index) => (
                  <Badge key={index} variant="secondary" className="text-xs">
                    {tag}
                  </Badge>
                ))}
              </div>
            </div>
          )}
          
          <div className="mt-4 pt-4 border-t flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleShareBookmark(bookmark)}
              className="flex-1"
            >
              <Share size={14} className="mr-1" />
              Share
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleExportBookmark(bookmark)}
              className="flex-1"
            >
              <Download size={14} className="mr-1" />
              Export
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );

  return (
    <>
      <Card className={className}>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <Bookmark size={20} />
              Bookmarks ({bookmarks.length})
            </CardTitle>
          </div>
          
          {/* Search and Filter */}
          <div className="space-y-3">
            <div className="relative">
              <Search size={14} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search bookmarks..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 text-sm"
              />
            </div>
            
            <div className="flex items-center gap-2 overflow-x-auto">
              <Filter size={14} className="text-muted-foreground flex-shrink-0" />
              {categories.map((category) => (
                <Button
                  key={category}
                  variant={selectedCategory === category ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => setSelectedCategory(category)}
                  className="text-xs whitespace-nowrap"
                >
                  {category === 'all' ? 'All' : category}
                </Button>
              ))}
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          {filteredBookmarks.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <Bookmark size={48} className="mx-auto mb-4 opacity-50" />
              <p>No bookmarks found</p>
              <p className="text-sm">
                {searchTerm || selectedCategory !== 'all' 
                  ? 'Try adjusting your search or filter'
                  : 'Bookmark messages to save them here'
                }
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredBookmarks.map((bookmark) => (
                <BookmarkCard key={bookmark.id} bookmark={bookmark} />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
      
      {/* Bookmark Details Modal */}
      {showDetails && selectedBookmark && (
        <BookmarkDetails bookmark={selectedBookmark} />
      )}
    </>
  );
};

BookmarkManager.displayName = 'BookmarkManager';