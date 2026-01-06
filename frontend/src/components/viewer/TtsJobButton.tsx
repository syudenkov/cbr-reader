import { IconButton, Tooltip, CircularProgress } from '@mui/material';
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { createTtsJob } from '../../store/slices/ttsSlice';

interface TtsJobButtonProps {
  fileId: number;
}

/**
 * Button to create and monitor TTS job processing
 * Shows different icons and states based on job status
 */
export const TtsJobButton = ({ fileId }: TtsJobButtonProps) => {
  const dispatch = useAppDispatch();
  const job = useAppSelector((state) => state.tts.jobs[fileId]);
  const jobLoadingStatus = useAppSelector((state) => state.tts.jobLoadingStatus);

  const status = job?.status;

  const handleCreateJob = () => {
    if (!job || status === 'FAILED') {
      dispatch(createTtsJob(fileId));
    }
  };

  const getButtonIcon = () => {
    // Show loading spinner while creating job
    if (jobLoadingStatus === 'loading') {
      return <CircularProgress size={24} />;
    }

    if (!job) {
      return <RecordVoiceOverIcon />;
    }

    switch (status) {
      case 'PENDING':
      case 'PROCESSING':
        return <CircularProgress size={24} />;
      case 'COMPLETED':
        return <CheckCircleIcon color="success" />;
      case 'FAILED':
        return <ErrorIcon color="error" />;
      default:
        return <RecordVoiceOverIcon />;
    }
  };

  const getTooltipText = () => {
    if (jobLoadingStatus === 'loading') {
      return 'Creating TTS job...';
    }

    if (!job) {
      return 'Generate Audio';
    }

    switch (status) {
      case 'PENDING':
        return 'Job queued...';
      case 'PROCESSING':
        return `Processing... ${job.progress}% (Page ${job.currentPage}/${job.totalPages})`;
      case 'COMPLETED':
        return 'Audio ready';
      case 'FAILED':
        return `Failed: ${job.errorMessage || 'Unknown error'} (click to retry)`;
      default:
        return 'Generate Audio';
    }
  };

  // Disable button during PENDING/PROCESSING/COMPLETED states
  const isDisabled =
    jobLoadingStatus === 'loading' ||
    status === 'PENDING' ||
    status === 'PROCESSING' ||
    status === 'COMPLETED';

  return (
    <Tooltip title={getTooltipText()}>
      {/* Wrap in span to show tooltip on disabled button */}
      <span>
        <IconButton
          onClick={handleCreateJob}
          disabled={isDisabled}
          aria-label="Generate TTS audio"
          size="large"
        >
          {getButtonIcon()}
        </IconButton>
      </span>
    </Tooltip>
  );
};
