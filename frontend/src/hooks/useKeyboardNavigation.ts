import { useEffect } from 'react';

interface KeyboardHandlers {
  onPrevPage: () => void;
  onNextPage: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onToggleFullscreen: () => void;
  onFirstPage: () => void;
  onLastPage: () => void;
}

/**
 * Custom hook for keyboard navigation in the viewer
 * Handles arrow keys, zoom shortcuts, fullscreen toggle, and page jump shortcuts
 */
export const useKeyboardNavigation = (handlers: KeyboardHandlers) => {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Prevent default browser behavior for viewer-specific keys
      if (['ArrowLeft', 'ArrowRight', '+', '=', '-', '_', 'f', 'F11', 'Home', 'End'].includes(event.key)) {
        event.preventDefault();
      }

      switch (event.key) {
        case 'ArrowLeft':
          handlers.onPrevPage();
          break;
        case 'ArrowRight':
          handlers.onNextPage();
          break;
        case '+':
        case '=':  // + key without shift
          handlers.onZoomIn();
          break;
        case '-':
        case '_':  // - key without shift
          handlers.onZoomOut();
          break;
        case 'f':
        case 'F11':
          handlers.onToggleFullscreen();
          break;
        case 'Home':
          handlers.onFirstPage();
          break;
        case 'End':
          handlers.onLastPage();
          break;
        case 'Escape':
          // Exit fullscreen if active
          if (document.fullscreenElement) {
            document.exitFullscreen();
          }
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [handlers]);
};
