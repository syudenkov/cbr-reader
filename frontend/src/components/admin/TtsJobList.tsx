import { Paper, Typography } from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';

/**
 * Placeholder component for TTS job history in admin panel
 * Full implementation will be added in Iteration 4 (I4.T3)
 */
export const TtsJobList = () => {
  return (
    <Paper
      sx={{
        p: 4,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 300,
        bgcolor: 'background.paper',
      }}
    >
      <InfoIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
      <Typography variant="h6" color="text.secondary" gutterBottom>
        TTS Job History
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Admin feature coming in Iteration 4
      </Typography>
    </Paper>
  );
};
