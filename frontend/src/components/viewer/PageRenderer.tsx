import { useState } from 'react';
import { Box, CircularProgress } from '@mui/material';

interface PageRendererProps {
  imageUrl: string | undefined;
  zoomLevel: number;
}

/**
 * Component that renders a comic page image with zoom support
 * Uses CSS transform: scale() for zoom performance
 */
export const PageRenderer = ({ imageUrl, zoomLevel }: PageRendererProps) => {
  const [imageLoaded, setImageLoaded] = useState(false);

  // Reset loaded state when image URL changes
  const handleImageLoad = () => {
    setImageLoaded(true);
  };

  const handleImageError = () => {
    setImageLoaded(false);
  };

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
        height: '100%',
        overflow: 'auto',  // Enable scrolling when zoomed
        bgcolor: 'background.default',
      }}
    >
      {!imageUrl || !imageLoaded ? (
        <CircularProgress />
      ) : null}
      {imageUrl && (
        <Box
          component="img"
          src={imageUrl}
          alt="Comic page"
          role="img"
          onLoad={handleImageLoad}
          onError={handleImageError}
          sx={{
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'contain',
            transform: `scale(${zoomLevel / 100})`,  // CRITICAL: Apply zoom here
            transformOrigin: 'center center',
            transition: 'transform 0.2s ease-out',  // Smooth zoom animation
            display: imageLoaded ? 'block' : 'none',
          }}
        />
      )}
    </Box>
  );
};
