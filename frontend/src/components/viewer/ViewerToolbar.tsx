import { useEffect } from 'react';
import { Box, IconButton, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import FullscreenIcon from '@mui/icons-material/Fullscreen';
import FullscreenExitIcon from '@mui/icons-material/FullscreenExit';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { setFullscreen } from '../../store/slices/viewerSlice';
import { TtsJobButton } from './TtsJobButton';

interface ViewerToolbarProps {
  fileId: number;
  title: string | undefined;
  currentPage: number;
  totalPages: number;
  onBack: () => void;
}

/**
 * Top toolbar for the viewer with back button, title, page counter, TTS button, and fullscreen toggle
 */
export const ViewerToolbar = ({ fileId, title, currentPage, totalPages, onBack }: ViewerToolbarProps) => {
  const dispatch = useAppDispatch();
  const isFullscreen = useAppSelector((state) => state.viewer.isFullscreen);

  const handleToggleFullscreen = async () => {
    const viewerElement = document.getElementById('viewer-container');

    if (!document.fullscreenElement) {
      // Enter fullscreen
      try {
        await viewerElement?.requestFullscreen();
        dispatch(setFullscreen(true));
      } catch (error) {
        console.error('Fullscreen request failed:', error);
      }
    } else {
      // Exit fullscreen
      try {
        await document.exitFullscreen();
        dispatch(setFullscreen(false));
      } catch (error) {
        console.error('Fullscreen exit failed:', error);
      }
    }
  };

  // Listen to fullscreen change events (user can also press Escape to exit)
  useEffect(() => {
    const handleFullscreenChange = () => {
      const isNowFullscreen = !!document.fullscreenElement;
      dispatch(setFullscreen(isNowFullscreen));
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, [dispatch]);

  return (
    <Box
      sx={{
        position: 'sticky',
        top: 0,
        left: 0,
        right: 0,
        bgcolor: 'background.paper',
        borderBottom: 1,
        borderColor: 'divider',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        px: 2,
        py: 1,
        zIndex: 1100,
      }}
    >
      {/* Left: Back button */}
      <IconButton onClick={onBack} aria-label="Back to library" size="large" edge="start">
        <ArrowBackIcon />
      </IconButton>

      {/* Center: Title and page counter */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flex: 1, justifyContent: 'center' }}>
        <Typography variant="h6" noWrap sx={{ maxWidth: 400 }}>
          {title || 'Loading...'}
        </Typography>
        {totalPages > 0 && (
          <Typography variant="body2" color="text.secondary">
            Page {currentPage} / {totalPages}
          </Typography>
        )}
      </Box>

      {/* Right: TTS button and Fullscreen toggle */}
      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
        <TtsJobButton fileId={fileId} />
        <IconButton onClick={handleToggleFullscreen} aria-label="Toggle fullscreen" size="large" edge="end">
          {isFullscreen ? <FullscreenExitIcon /> : <FullscreenIcon />}
        </IconButton>
      </Box>
    </Box>
  );
};
