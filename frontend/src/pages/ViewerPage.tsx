import { useEffect, useCallback, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, CircularProgress, Alert, Button } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchFileMetadata,
  preloadPage,
  setCurrentPage,
  setZoomLevel,
  toggleFullscreen,
  clearPreloadedPages,
  resetViewer,
  fetchReadingProgress,
  updateReadingProgress,
} from '../store/slices/viewerSlice';
import { pollJobStatus } from '../store/slices/ttsSlice';
import { ViewerToolbar } from '../components/viewer/ViewerToolbar';
import { PageRenderer } from '../components/viewer/PageRenderer';
import { ViewerControls } from '../components/viewer/ViewerControls';
import { AudioPlayer } from '../components/viewer/AudioPlayer';
import { useKeyboardNavigation } from '../hooks/useKeyboardNavigation';

/**
 * Main viewer page for reading comics
 * Route: /viewer/:fileId
 */
export const ViewerPage = () => {
  const { fileId } = useParams<{ fileId: string }>();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const {
    currentFile,
    currentPage,
    totalPages,
    zoomLevel,
    preloadedPages,
    fileLoadingStatus,
    progressLoadingStatus,
    error,
  } = useAppSelector((state) => state.viewer);

  const ttsJob = useAppSelector((state) =>
    fileId ? state.tts.jobs[Number(fileId)] : null
  );

  // Debounce timer ref
  const progressUpdateTimerRef = useRef<NodeJS.Timeout | null>(null);

  // TTS polling state
  const [pollStartTime, setPollStartTime] = useState<number | null>(null);

  // Fetch file metadata on mount
  useEffect(() => {
    if (fileId) {
      dispatch(fetchFileMetadata(Number(fileId)));
    }

    // Cleanup on unmount
    return () => {
      // Clear debounce timer
      if (progressUpdateTimerRef.current) {
        clearTimeout(progressUpdateTimerRef.current);
      }
      // Revoke all blob URLs to prevent memory leaks
      Object.values(preloadedPages).forEach((url) => {
        URL.revokeObjectURL(url);
      });
      dispatch(clearPreloadedPages());
      dispatch(resetViewer());
    };
  }, [fileId, dispatch, preloadedPages]);

  // Fetch reading progress after file metadata is loaded
  useEffect(() => {
    if (fileId && fileLoadingStatus === 'succeeded' && progressLoadingStatus === 'idle') {
      dispatch(fetchReadingProgress(Number(fileId)));
    }
  }, [fileId, fileLoadingStatus, progressLoadingStatus, dispatch]);

  // Update reading progress when page changes (debounced 2 seconds)
  useEffect(() => {
    if (fileId && currentPage > 0 && progressLoadingStatus !== 'loading') {
      // Clear existing timer
      if (progressUpdateTimerRef.current) {
        clearTimeout(progressUpdateTimerRef.current);
      }

      // Set new timer to update progress after 2 seconds
      progressUpdateTimerRef.current = setTimeout(() => {
        dispatch(updateReadingProgress({ fileId: Number(fileId), currentPage }));
      }, 2000);
    }

    return () => {
      // Clear timer on cleanup
      if (progressUpdateTimerRef.current) {
        clearTimeout(progressUpdateTimerRef.current);
      }
    };
  }, [fileId, currentPage, progressLoadingStatus, dispatch]);

  // Load current page when it changes
  useEffect(() => {
    if (fileId && currentPage > 0 && totalPages > 0) {
      // Load current page if not already loaded
      if (!preloadedPages[currentPage]) {
        dispatch(preloadPage({ fileId: Number(fileId), pageNumber: currentPage }));
      }
    }
  }, [fileId, currentPage, totalPages, preloadedPages, dispatch]);

  // Preload next page when current page changes
  useEffect(() => {
    if (fileId && currentPage < totalPages) {
      const nextPage = currentPage + 1;
      if (!preloadedPages[nextPage]) {
        dispatch(preloadPage({ fileId: Number(fileId), pageNumber: nextPage }));
      }
    }
  }, [fileId, currentPage, totalPages, preloadedPages, dispatch]);

  // TTS job polling: Poll every 2 seconds while PENDING or PROCESSING
  useEffect(() => {
    const jobId = ttsJob?.jobId;
    const status = ttsJob?.status;

    // Don't poll if no job or job is finished
    if (!jobId || (status !== 'PENDING' && status !== 'PROCESSING')) {
      setPollStartTime(null);
      return;
    }

    // Set poll start time if not already set
    if (!pollStartTime) {
      setPollStartTime(Date.now());
    }

    // Check if polling timeout exceeded (5 minutes)
    if (pollStartTime && Date.now() - pollStartTime > 300000) {
      console.warn('TTS job polling timeout (5 minutes)');
      setPollStartTime(null);
      return;
    }

    // Start polling interval
    const intervalId = setInterval(() => {
      dispatch(pollJobStatus(jobId));
    }, 2000); // Poll every 2 seconds

    return () => clearInterval(intervalId);
  }, [ttsJob?.jobId, ttsJob?.status, pollStartTime, dispatch]);

  // Navigation handlers
  const handlePrevPage = useCallback(() => {
    if (currentPage > 1) {
      dispatch(setCurrentPage(currentPage - 1));
    }
  }, [currentPage, dispatch]);

  const handleNextPage = useCallback(() => {
    if (currentPage < totalPages) {
      dispatch(setCurrentPage(currentPage + 1));
    }
  }, [currentPage, totalPages, dispatch]);

  const handleZoomIn = useCallback(() => {
    dispatch(setZoomLevel(Math.min(200, zoomLevel + 10)));
  }, [zoomLevel, dispatch]);

  const handleZoomOut = useCallback(() => {
    dispatch(setZoomLevel(Math.max(50, zoomLevel - 10)));
  }, [zoomLevel, dispatch]);

  const handleToggleFullscreen = useCallback(() => {
    dispatch(toggleFullscreen());
  }, [dispatch]);

  const handleFirstPage = useCallback(() => {
    dispatch(setCurrentPage(1));
  }, [dispatch]);

  const handleLastPage = useCallback(() => {
    dispatch(setCurrentPage(totalPages));
  }, [totalPages, dispatch]);

  const handleBack = useCallback(() => {
    navigate('/library');
  }, [navigate]);

  // Bind keyboard shortcuts
  useKeyboardNavigation({
    onPrevPage: handlePrevPage,
    onNextPage: handleNextPage,
    onZoomIn: handleZoomIn,
    onZoomOut: handleZoomOut,
    onToggleFullscreen: handleToggleFullscreen,
    onFirstPage: handleFirstPage,
    onLastPage: handleLastPage,
  });

  // Show loading state
  if (fileLoadingStatus === 'loading') {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          bgcolor: 'background.default',
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  // Show error state
  if (error || fileLoadingStatus === 'failed') {
    return (
      <Box sx={{ p: 3, bgcolor: 'background.default', minHeight: '100vh' }}>
        <Alert severity="error" sx={{ mb: 2 }}>
          {error || 'Failed to load comic file'}
        </Alert>
        <Button variant="contained" onClick={handleBack}>
          Back to Library
        </Button>
      </Box>
    );
  }

  // Get current page URL (from preloaded pages)
  const currentPageUrl = preloadedPages[currentPage];

  // Determine if audio player should be shown
  const showAudioPlayer = ttsJob?.status === 'COMPLETED';

  return (
    <Box
      id="viewer-container"
      sx={{
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'background.default',
      }}
    >
      <ViewerToolbar
        fileId={Number(fileId)}
        title={currentFile?.originalFilename}
        currentPage={currentPage}
        totalPages={totalPages}
        onBack={handleBack}
      />

      {/* Audio Player - shown when TTS job is completed */}
      {showAudioPlayer && fileId && (
        <AudioPlayer fileId={Number(fileId)} pageNumber={currentPage} />
      )}

      <Box sx={{ flex: 1, overflow: 'auto', position: 'relative' }}>
        <PageRenderer imageUrl={currentPageUrl} zoomLevel={zoomLevel} />
      </Box>

      <ViewerControls />
    </Box>
  );
};
