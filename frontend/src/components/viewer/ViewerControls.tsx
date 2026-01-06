import { Box, IconButton, Typography, Slider } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { setCurrentPage, setZoomLevel } from '../../store/slices/viewerSlice';

/**
 * Bottom controls for the viewer with navigation buttons, page counter, and zoom slider
 */
export const ViewerControls = () => {
  const dispatch = useAppDispatch();
  const { currentPage, totalPages, zoomLevel } = useAppSelector((state) => state.viewer);

  const canGoPrevious = currentPage > 1;
  const canGoNext = currentPage < totalPages;

  const handlePrevPage = () => {
    if (canGoPrevious) {
      dispatch(setCurrentPage(currentPage - 1));
    }
  };

  const handleNextPage = () => {
    if (canGoNext) {
      dispatch(setCurrentPage(currentPage + 1));
    }
  };

  const handleZoomIn = () => {
    dispatch(setZoomLevel(Math.min(200, zoomLevel + 10)));
  };

  const handleZoomOut = () => {
    dispatch(setZoomLevel(Math.max(50, zoomLevel - 10)));
  };

  const handleZoomChange = (_event: Event, value: number | number[]) => {
    dispatch(setZoomLevel(value as number));
  };

  return (
    <Box
      sx={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        bgcolor: 'background.paper',
        borderTop: 1,
        borderColor: 'divider',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 3,
        py: 1.5,
        gap: 3,
        zIndex: 1100,
      }}
    >
      {/* Previous button */}
      <IconButton
        onClick={handlePrevPage}
        disabled={!canGoPrevious}
        aria-label="Previous page"
        size="large"
      >
        <ArrowBackIcon />
      </IconButton>

      {/* Page counter */}
      <Typography variant="body1" sx={{ minWidth: '100px', textAlign: 'center' }}>
        {currentPage} / {totalPages}
      </Typography>

      {/* Next button */}
      <IconButton
        onClick={handleNextPage}
        disabled={!canGoNext}
        aria-label="Next page"
        size="large"
      >
        <ArrowForwardIcon />
      </IconButton>

      {/* Divider */}
      <Box sx={{ height: 32, width: 1, bgcolor: 'divider' }} />

      {/* Zoom out button */}
      <IconButton
        onClick={handleZoomOut}
        disabled={zoomLevel <= 50}
        aria-label="Zoom out"
        size="medium"
      >
        <ZoomOutIcon />
      </IconButton>

      {/* Zoom slider */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, minWidth: 250 }}>
        <Typography variant="body2" sx={{ minWidth: '50px', textAlign: 'center' }}>
          {zoomLevel}%
        </Typography>
        <Slider
          value={zoomLevel}
          onChange={handleZoomChange}
          min={50}
          max={200}
          step={10}
          marks={[
            { value: 50, label: '50%' },
            { value: 100, label: '100%' },
            { value: 200, label: '200%' },
          ]}
          sx={{ flex: 1 }}
          aria-label="Zoom level"
        />
      </Box>

      {/* Zoom in button */}
      <IconButton
        onClick={handleZoomIn}
        disabled={zoomLevel >= 200}
        aria-label="Zoom in"
        size="medium"
      >
        <ZoomInIcon />
      </IconButton>
    </Box>
  );
};
