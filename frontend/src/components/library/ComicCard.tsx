import React, { useState } from 'react';
import {
  Card,
  CardMedia,
  CardContent,
  CardActions,
  Typography,
  IconButton,
  Skeleton,
  Box,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { ComicFile } from '../../services/fileService';
import ProgressBadge from './ProgressBadge';

interface ComicCardProps {
  file: ComicFile;
  onOpen: (fileId: number) => void;
  onDelete?: (file: ComicFile) => void;
  currentPage?: number;
}

/**
 * Format ISO timestamp to relative time
 */
const formatDate = (isoString: string): string => {
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return 'Today';
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays} days ago`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`;

  // Fallback to absolute date
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
};

const ComicCard: React.FC<ComicCardProps> = ({ file, onOpen, onDelete, currentPage }) => {
  const [imageLoaded, setImageLoaded] = useState(false);
  const [imageError, setImageError] = useState(false);

  const handleCardClick = (event: React.MouseEvent) => {
    // Don't trigger if clicking on action buttons
    if ((event.target as HTMLElement).closest('.MuiCardActions-root')) {
      return;
    }
    onOpen(file.id);
  };

  const handleDeleteClick = (event: React.MouseEvent) => {
    event.stopPropagation();
    if (onDelete) {
      onDelete(file);
    }
  };

  // Use cover image if available, otherwise use placeholder
  const coverUrl = file.coverImagePath || '/placeholder-cover.jpg';

  return (
    <Card
      onClick={handleCardClick}
      sx={{
        cursor: 'pointer',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        transition: 'all 0.2s ease-in-out',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: 6,
        },
      }}
    >
      {/* Cover Image */}
      <Box sx={{ position: 'relative', paddingTop: '133.33%', bgcolor: 'grey.900' }}>
        {currentPage && currentPage > 1 && (
          <ProgressBadge currentPage={currentPage} totalPages={file.pageCount} />
        )}
        {!imageLoaded && !imageError && (
          <Skeleton
            variant="rectangular"
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
            }}
          />
        )}
        {!imageError ? (
          <CardMedia
            component="img"
            image={coverUrl}
            alt={`Cover of ${file.originalFilename}`}
            onLoad={() => setImageLoaded(true)}
            onError={() => setImageError(true)}
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              display: imageLoaded ? 'block' : 'none',
            }}
          />
        ) : (
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'grey.800',
            }}
          >
            <Typography variant="body2" color="text.secondary">
              No Cover
            </Typography>
          </Box>
        )}
      </Box>

      {/* Metadata */}
      <CardContent sx={{ flexGrow: 1, pb: 1 }}>
        <Typography
          variant="h6"
          component="div"
          noWrap
          sx={{ fontSize: '1rem', fontWeight: 500, mb: 0.5 }}
          title={file.originalFilename}
        >
          {file.originalFilename}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {file.pageCount} pages
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {formatDate(file.createdAt)}
        </Typography>
      </CardContent>

      {/* Actions */}
      {onDelete && (
        <CardActions sx={{ pt: 0 }}>
          <IconButton
            size="small"
            color="error"
            onClick={handleDeleteClick}
            aria-label="delete"
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </CardActions>
      )}
    </Card>
  );
};

export default ComicCard;
