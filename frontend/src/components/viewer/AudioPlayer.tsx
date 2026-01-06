import { useEffect, useRef, useState, useCallback } from 'react';
import {
  Card,
  CardContent,
  Box,
  IconButton,
  Slider,
  Typography,
  LinearProgress,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import PauseIcon from '@mui/icons-material/Pause';
import VolumeUpIcon from '@mui/icons-material/VolumeUp';
import VolumeOffIcon from '@mui/icons-material/VolumeOff';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import {
  setPlaying,
  setVolume,
  setCurrentTime,
  setDuration,
  setCurrentAudio,
} from '../../store/slices/ttsSlice';
import * as ttsService from '../../services/ttsService';

interface AudioPlayerProps {
  fileId: number;
  pageNumber: number;
}

/**
 * Audio player for TTS page playback
 * Synchronized with viewer page navigation
 */
export const AudioPlayer = ({ fileId, pageNumber }: AudioPlayerProps) => {
  const dispatch = useAppDispatch();
  const audioRef = useRef<HTMLAudioElement>(null);
  const [audioError, setAudioError] = useState(false);
  const [audioLoading, setAudioLoading] = useState(true);

  const { playing, volume, currentTime, duration } = useAppSelector(
    (state) => state.tts.currentAudio
  );

  // Load saved volume from localStorage on mount
  useEffect(() => {
    const savedVolume = localStorage.getItem('audioVolume');
    if (savedVolume) {
      const volumeValue = parseFloat(savedVolume);
      dispatch(setVolume(volumeValue));
      if (audioRef.current) {
        audioRef.current.volume = volumeValue / 100;
      }
    }
  }, [dispatch]);

  // Load audio when fileId or pageNumber changes
  useEffect(() => {
    setAudioError(false);
    setAudioLoading(true);

    const audioUrl = ttsService.getAudioUrl(fileId, pageNumber);

    if (audioRef.current) {
      // Stop current playback
      audioRef.current.pause();
      dispatch(setPlaying(false));

      // Load new audio
      audioRef.current.src = audioUrl;
      audioRef.current.load();

      // Update Redux state
      dispatch(setCurrentAudio({ fileId, pageNumber }));
    }
  }, [fileId, pageNumber, dispatch]);

  // Apply volume changes to audio element
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = volume / 100;
    }
  }, [volume]);

  const handlePlayPause = useCallback(() => {
    if (!audioRef.current || audioError || audioLoading) return;

    if (playing) {
      audioRef.current.pause();
      dispatch(setPlaying(false));
    } else {
      audioRef.current.play();
      dispatch(setPlaying(true));
    }
  }, [audioRef, audioError, audioLoading, playing, dispatch]);

  const handleMuteToggle = useCallback(() => {
    if (volume > 0) {
      // Mute
      dispatch(setVolume(0));
      localStorage.setItem('audioVolume', '0');
      if (audioRef.current) {
        audioRef.current.volume = 0;
      }
    } else {
      // Unmute to default 100%
      dispatch(setVolume(100));
      localStorage.setItem('audioVolume', '100');
      if (audioRef.current) {
        audioRef.current.volume = 1;
      }
    }
  }, [volume, dispatch]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      // Only handle if audio player is focused or no input is focused
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }

      // Space = play/pause
      if (e.code === 'Space' && !audioError && !audioLoading) {
        e.preventDefault();
        handlePlayPause();
      }

      // M = mute/unmute
      if (e.code === 'KeyM') {
        e.preventDefault();
        handleMuteToggle();
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [audioError, audioLoading, handlePlayPause, handleMuteToggle]);

  const handleVolumeChange = (_event: Event, newValue: number | number[]) => {
    const volumeValue = newValue as number;
    dispatch(setVolume(volumeValue));
    localStorage.setItem('audioVolume', volumeValue.toString());

    if (audioRef.current) {
      audioRef.current.volume = volumeValue / 100;
    }
  };

  const handleTimeUpdate = (e: React.SyntheticEvent<HTMLAudioElement>) => {
    dispatch(setCurrentTime(e.currentTarget.currentTime));
  };

  const handleDurationChange = (e: React.SyntheticEvent<HTMLAudioElement>) => {
    dispatch(setDuration(e.currentTarget.duration));
    setAudioLoading(false);
  };

  const handleEnded = () => {
    dispatch(setPlaying(false));
  };

  const handleError = () => {
    console.error('Audio load failed for page', pageNumber);
    setAudioError(true);
    setAudioLoading(false);
    dispatch(setPlaying(false));
  };

  const handleCanPlay = () => {
    setAudioLoading(false);
  };

  const handleSeek = (_event: Event, newValue: number | number[]) => {
    const timeValue = newValue as number;
    if (audioRef.current) {
      audioRef.current.currentTime = timeValue;
      dispatch(setCurrentTime(timeValue));
    }
  };

  const formatTime = (seconds: number): string => {
    if (!isFinite(seconds)) return '0:00';

    const minutes = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <Card
      sx={{
        position: 'sticky',
        top: 65,
        left: 0,
        right: 0,
        zIndex: 1000,
        borderRadius: 0,
        borderBottom: 1,
        borderColor: 'divider',
      }}
    >
      <CardContent sx={{ py: 1.5, px: 2, '&:last-child': { pb: 1.5 } }}>
        {/* Hidden audio element */}
        <audio
          ref={audioRef}
          onTimeUpdate={handleTimeUpdate}
          onDurationChange={handleDurationChange}
          onEnded={handleEnded}
          onError={handleError}
          onCanPlay={handleCanPlay}
          style={{ display: 'none' }}
        />

        {audioLoading && (
          <Box sx={{ mb: 1 }}>
            <LinearProgress />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
              Loading audio for page {pageNumber}...
            </Typography>
          </Box>
        )}

        {audioError && (
          <Typography variant="body2" color="error">
            No audio available for this page
          </Typography>
        )}

        {!audioError && !audioLoading && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {/* Play/Pause Button */}
            <IconButton
              onClick={handlePlayPause}
              aria-label={playing ? 'Pause audio' : 'Play audio'}
              size="large"
            >
              {playing ? <PauseIcon /> : <PlayArrowIcon />}
            </IconButton>

            {/* Progress Bar */}
            <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="caption" color="text.secondary" sx={{ minWidth: 40 }}>
                {formatTime(currentTime)}
              </Typography>

              <Slider
                value={currentTime}
                min={0}
                max={duration || 100}
                onChange={handleSeek}
                aria-label="Audio progress"
                size="small"
                sx={{ flex: 1 }}
              />

              <Typography variant="caption" color="text.secondary" sx={{ minWidth: 40 }}>
                {formatTime(duration)}
              </Typography>
            </Box>

            {/* Volume Control */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 150 }}>
              <IconButton
                onClick={handleMuteToggle}
                aria-label={volume > 0 ? 'Mute audio' : 'Unmute audio'}
                size="small"
              >
                {volume > 0 ? <VolumeUpIcon /> : <VolumeOffIcon />}
              </IconButton>

              <Slider
                value={volume}
                min={0}
                max={100}
                onChange={handleVolumeChange}
                aria-label="Volume"
                size="small"
                sx={{ width: 100 }}
              />
            </Box>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};
