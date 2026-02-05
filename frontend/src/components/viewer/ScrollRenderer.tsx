import { useEffect, useRef, useCallback, useState, useMemo } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { preloadPage, setCurrentPage } from '../../store/slices/viewerSlice';
import type { RootState } from '../../store/store';

interface ScrollRendererProps {
  fileId: number;
  zoomLevel: number;
}

/**
 * Infinite scroll renderer that displays all comic pages in a scrollable container.
 * Uses intersection observer to track current page and lazy-load images.
 */
export const ScrollRenderer = ({ fileId, zoomLevel }: ScrollRendererProps) => {
  const dispatch = useAppDispatch();
  const { totalPages, preloadedPages, currentPage } = useAppSelector((state) => state.viewer);
  const progressLoadingStatus = useAppSelector((state: RootState) => state.viewer.progressLoadingStatus);
  const containerRef = useRef<HTMLDivElement>(null);
  const pageRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const [visiblePages, setVisiblePages] = useState<Set<number>>(new Set([1]));
  const hasRestoredScrollRef = useRef(false);

  // Generate array of page numbers
  const pageNumbers = useMemo(() =>
    Array.from({ length: totalPages }, (_, i) => i + 1),
    [totalPages]
  );

  // Preload visible pages and adjacent pages
  useEffect(() => {
    visiblePages.forEach((pageNum) => {
      // Load current page
      if (!preloadedPages[pageNum]) {
        dispatch(preloadPage({ fileId, pageNumber: pageNum }));
      }
      // Preload next page
      if (pageNum < totalPages && !preloadedPages[pageNum + 1]) {
        dispatch(preloadPage({ fileId, pageNumber: pageNum + 1 }));
      }
    });
  }, [visiblePages, fileId, totalPages, preloadedPages, dispatch]);

  // Intersection observer to track visible pages
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const newVisible = new Set(visiblePages);
        let topVisiblePage = currentPage;
        let topVisibleRatio = 0;

        entries.forEach((entry) => {
          const pageNum = parseInt(entry.target.getAttribute('data-page') || '0', 10);
          if (entry.isIntersecting) {
            newVisible.add(pageNum);
            // Track the most visible page for current page indicator
            if (entry.intersectionRatio > topVisibleRatio) {
              topVisibleRatio = entry.intersectionRatio;
              topVisiblePage = pageNum;
            }
          } else {
            newVisible.delete(pageNum);
          }
        });

        setVisiblePages(newVisible);

        // Update current page based on most visible page
        // Only update after initial scroll has been restored
        if (hasRestoredScrollRef.current && topVisiblePage !== currentPage && topVisibleRatio > 0.3) {
          dispatch(setCurrentPage(topVisiblePage));
        }
      },
      {
        root: containerRef.current,
        rootMargin: '100px 0px',
        threshold: [0, 0.25, 0.5, 0.75, 1],
      }
    );

    // Observe all page elements
    pageRefs.current.forEach((element) => {
      observer.observe(element);
    });

    return () => observer.disconnect();
  }, [totalPages, currentPage, dispatch, visiblePages]);

  // Register page ref
  const setPageRef = useCallback((pageNum: number, element: HTMLDivElement | null) => {
    if (element) {
      pageRefs.current.set(pageNum, element);
    } else {
      pageRefs.current.delete(pageNum);
    }
  }, []);

  // Scroll to specific page
  const scrollToPage = useCallback((pageNum: number, instant = false) => {
    const element = pageRefs.current.get(pageNum);
    if (element) {
      element.scrollIntoView({ behavior: instant ? 'instant' : 'smooth', block: 'start' });
    }
  }, []);

  // Scroll to restored page when progress loads
  useEffect(() => {
    // Mark as restored when progress loading completes (or fails)
    if (!hasRestoredScrollRef.current &&
        (progressLoadingStatus === 'succeeded' || progressLoadingStatus === 'failed')) {
      if (currentPage > 1) {
        // Small delay to ensure refs are ready, then scroll instantly
        const timer = setTimeout(() => {
          scrollToPage(currentPage, true); // Use instant scroll for restoration
          // Mark as restored after scroll completes
          hasRestoredScrollRef.current = true;
        }, 200);
        return () => clearTimeout(timer);
      } else {
        // No scroll needed (page 1), mark as restored immediately
        hasRestoredScrollRef.current = true;
      }
    }
  }, [progressLoadingStatus, currentPage, scrollToPage]);

  return (
    <Box
      ref={containerRef}
      sx={{
        height: '100%',
        overflow: 'auto',
        bgcolor: 'background.default',
        scrollBehavior: 'smooth',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 2,
          py: 2,
        }}
      >
        {pageNumbers.map((pageNum) => (
          <Box
            key={pageNum}
            ref={(el: HTMLDivElement | null) => setPageRef(pageNum, el)}
            data-page={pageNum}
            sx={{
              width: '100%',
              maxWidth: `${zoomLevel}%`,
              minHeight: 200,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              position: 'relative',
            }}
          >
            {/* Page number indicator */}
            <Typography
              variant="caption"
              sx={{
                position: 'absolute',
                top: 8,
                right: 8,
                bgcolor: 'rgba(0,0,0,0.6)',
                color: 'white',
                px: 1,
                py: 0.5,
                borderRadius: 1,
                zIndex: 1,
              }}
            >
              {pageNum} / {totalPages}
            </Typography>

            {preloadedPages[pageNum] ? (
              <Box
                component="img"
                src={preloadedPages[pageNum]}
                alt={`Page ${pageNum}`}
                sx={{
                  width: '100%',
                  height: 'auto',
                  objectFit: 'contain',
                }}
              />
            ) : (
              <Box
                sx={{
                  width: '100%',
                  height: 600,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: 'grey.900',
                }}
              >
                <CircularProgress />
              </Box>
            )}
          </Box>
        ))}
      </Box>
    </Box>
  );
};
