import { useEffect, useCallback } from 'react';
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
} from '../store/slices/viewerSlice';
import { ViewerToolbar } from '../components/viewer/ViewerToolbar';
import { PageRenderer } from '../components/viewer/PageRenderer';
import { ViewerControls } from '../components/viewer/ViewerControls';
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
    error,
  } = useAppSelector((state) => state.viewer);

  // Fetch file metadata on mount
  useEffect(() => {
    if (fileId) {
      dispatch(fetchFileMetadata(Number(fileId)));
    }

    // Cleanup on unmount
    return () => {
      // Revoke all blob URLs to prevent memory leaks
      Object.values(preloadedPages).forEach((url) => {
        URL.revokeObjectURL(url);
      });
      dispatch(clearPreloadedPages());
      dispatch(resetViewer());
    };
  }, [fileId, dispatch]);

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
        title={currentFile?.originalFilename}
        currentPage={currentPage}
        totalPages={totalPages}
        onBack={handleBack}
      />

      <Box sx={{ flex: 1, overflow: 'auto', position: 'relative' }}>
        <PageRenderer imageUrl={currentPageUrl} zoomLevel={zoomLevel} />
      </Box>

      <ViewerControls />
    </Box>
  );
};
