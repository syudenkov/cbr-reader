import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

interface ProgressBadgeProps {
  currentPage: number;
  totalPages: number;
}

const ProgressBadge: React.FC<ProgressBadgeProps> = ({ currentPage, totalPages }) => {
  // Calculate progress percentage
  const percentage = totalPages > 0 ? Math.round((currentPage / totalPages) * 100) : 0;

  // Don't show badge if no progress yet (still on page 1)
  if (currentPage <= 1) {
    return null;
  }

  return (
    <Box
      sx={{
        position: 'absolute',
        top: 8,
        right: 8,
        width: 48,
        height: 48,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'rgba(0, 0, 0, 0.7)',
        borderRadius: '50%',
        zIndex: 10,
      }}
    >
      <CircularProgress
        variant="determinate"
        value={percentage}
        size={40}
        thickness={4}
        sx={{
          position: 'absolute',
          color: 'primary.main',
        }}
      />
      <Typography
        variant="caption"
        sx={{
          fontSize: '0.75rem',
          fontWeight: 'bold',
          color: 'white',
        }}
      >
        {percentage}%
      </Typography>
    </Box>
  );
};

export default ProgressBadge;
