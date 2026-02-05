import React, { useState, useEffect } from 'react';
import { Box, Rating, Typography, CircularProgress } from '@mui/material';
import StarIcon from '@mui/icons-material/Star';
import { submitRating, getAverageRating, getUserRating } from '../../services/ratingService';

interface RatingWidgetProps {
  fileId: number;
}

const RatingWidget: React.FC<RatingWidgetProps> = ({ fileId }) => {
  const [userRating, setUserRating] = useState<number | null>(null);
  const [averageRating, setAverageRating] = useState<number | null>(null);
  const [totalRatings, setTotalRatings] = useState<number>(0);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch average rating and user rating on mount
  useEffect(() => {
    const fetchRatings = async () => {
      try {
        const [avgResponse, userRatingResponse] = await Promise.all([
          getAverageRating(fileId),
          getUserRating(fileId).catch(() => null) // Don't fail if no user rating
        ]);

        setAverageRating(avgResponse.averageRating);
        setTotalRatings(avgResponse.totalRatings);

        if (userRatingResponse?.rating) {
          setUserRating(userRatingResponse.rating);
        }
      } catch (err) {
        console.error('Error fetching ratings:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchRatings();
  }, [fileId]);

  const handleRatingChange = async (
    _event: React.SyntheticEvent,
    newValue: number | null
  ) => {
    if (newValue === null || saving) return;

    const previousRating = userRating;

    // Optimistic update
    setUserRating(newValue);
    setSaving(true);
    setError(null);

    try {
      await submitRating(fileId, newValue);

      // Refresh average rating after successful submission
      const avgResponse = await getAverageRating(fileId);
      setAverageRating(avgResponse.averageRating);
      setTotalRatings(avgResponse.totalRatings);
    } catch (err) {
      console.error('Error submitting rating:', err);

      // Revert on error
      setUserRating(previousRating);
      setError('Failed to save rating. Please try again.');

      // Clear error message after 3 seconds
      setTimeout(() => setError(null), 3000);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 1 }}>
        <CircularProgress size={16} />
        <Typography variant="caption" color="text.secondary">
          Loading ratings...
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ py: 1 }}>
      {/* Average Rating Display */}
      {averageRating !== null ? (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1 }}>
          <Rating
            value={averageRating}
            precision={0.1}
            readOnly
            size="small"
            emptyIcon={<StarIcon style={{ opacity: 0.3 }} fontSize="inherit" />}
            sx={{ color: 'warning.main' }}
          />
          <Typography variant="caption" color="text.secondary">
            {averageRating.toFixed(1)} ({totalRatings} {totalRatings === 1 ? 'rating' : 'ratings'})
          </Typography>
        </Box>
      ) : (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
          No ratings yet
        </Typography>
      )}

      {/* User Rating Input */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Rating
          name={`rating-${fileId}`}
          value={userRating}
          onChange={handleRatingChange}
          disabled={saving}
          precision={1}
          size="medium"
          emptyIcon={<StarIcon style={{ opacity: 0.3 }} fontSize="inherit" />}
          sx={{ color: 'warning.main' }}
        />
        {saving && <CircularProgress size={16} />}
      </Box>

      {/* Error Message */}
      {error && (
        <Typography variant="caption" color="error" sx={{ display: 'block', mt: 0.5 }}>
          {error}
        </Typography>
      )}

      {/* Helper Text */}
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
        {userRating ? 'Click to change your rating' : 'Click to rate'}
      </Typography>
    </Box>
  );
};

export default RatingWidget;
