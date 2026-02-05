import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RatingWidget from './RatingWidget';
import * as ratingService from '../../services/ratingService';

// Mock the rating service
jest.mock('../../services/ratingService');

const mockedRatingService = ratingService as jest.Mocked<typeof ratingService>;

describe('RatingWidget', () => {
  const fileId = 1;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    mockedRatingService.getAverageRating.mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );
    mockedRatingService.getUserRating.mockImplementation(
      () => new Promise(() => {})
    );

    render(<RatingWidget fileId={fileId} />);

    expect(screen.getByText('Loading ratings...')).toBeInTheDocument();
  });

  it('should display average rating and total count', async () => {
    mockedRatingService.getAverageRating.mockResolvedValueOnce({
      fileId,
      averageRating: 4.5,
      totalRatings: 10,
    });
    mockedRatingService.getUserRating.mockResolvedValueOnce({
      id: 1,
      fileId,
      userId: 1,
      rating: 5,
    });

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('4.5 (10 ratings)')).toBeInTheDocument();
    });
  });

  it('should display "No ratings yet" when no ratings exist', async () => {
    mockedRatingService.getAverageRating.mockResolvedValueOnce({
      fileId,
      averageRating: null,
      totalRatings: 0,
    });
    mockedRatingService.getUserRating.mockRejectedValueOnce(new Error('Not found'));

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('No ratings yet')).toBeInTheDocument();
    });
  });

  it('should display singular "rating" for one rating', async () => {
    mockedRatingService.getAverageRating.mockResolvedValueOnce({
      fileId,
      averageRating: 5.0,
      totalRatings: 1,
    });
    mockedRatingService.getUserRating.mockRejectedValueOnce(new Error('Not found'));

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('5.0 (1 rating)')).toBeInTheDocument();
    });
  });

  it('should show helper text based on user rating state', async () => {
    mockedRatingService.getAverageRating.mockResolvedValueOnce({
      fileId,
      averageRating: null,
      totalRatings: 0,
    });
    mockedRatingService.getUserRating.mockRejectedValueOnce(new Error('Not found'));

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('Click to rate')).toBeInTheDocument();
    });
  });

  it('should show "Click to change your rating" when user has rated', async () => {
    mockedRatingService.getAverageRating.mockResolvedValueOnce({
      fileId,
      averageRating: 4.0,
      totalRatings: 5,
    });
    mockedRatingService.getUserRating.mockResolvedValueOnce({
      id: 1,
      fileId,
      userId: 1,
      rating: 4,
    });

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('Click to change your rating')).toBeInTheDocument();
    });
  });

  it('should submit rating when user clicks', async () => {
    mockedRatingService.getAverageRating.mockResolvedValue({
      fileId,
      averageRating: 4.0,
      totalRatings: 5,
    });
    mockedRatingService.getUserRating.mockRejectedValueOnce(new Error('Not found'));
    mockedRatingService.submitRating.mockResolvedValueOnce({
      id: 1,
      fileId,
      userId: 1,
      rating: 5,
    });

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('Click to rate')).toBeInTheDocument();
    });

    // Find the rating input and interact with it
    const ratingInput = screen.getByRole('radio', { name: '5 Stars' });
    fireEvent.click(ratingInput);

    await waitFor(() => {
      expect(mockedRatingService.submitRating).toHaveBeenCalledWith(fileId, 5);
    });
  });

  it('should revert rating on submission error', async () => {
    mockedRatingService.getAverageRating.mockResolvedValue({
      fileId,
      averageRating: 3.0,
      totalRatings: 3,
    });
    mockedRatingService.getUserRating.mockResolvedValueOnce({
      id: 1,
      fileId,
      userId: 1,
      rating: 3,
    });
    mockedRatingService.submitRating.mockRejectedValueOnce(new Error('Server error'));

    render(<RatingWidget fileId={fileId} />);

    await waitFor(() => {
      expect(screen.getByText('Click to change your rating')).toBeInTheDocument();
    });

    // Try to change rating
    const ratingInput = screen.getByRole('radio', { name: '5 Stars' });
    fireEvent.click(ratingInput);

    await waitFor(() => {
      expect(screen.getByText('Failed to save rating. Please try again.')).toBeInTheDocument();
    });
  });
});
