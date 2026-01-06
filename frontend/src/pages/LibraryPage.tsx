import { Typography, Box } from '@mui/material';
import AppLayout from '../components/layout/AppLayout';

const LibraryPage = () => {
  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" component="h1" gutterBottom>
          Comic Library
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Your comic book collection will appear here.
        </Typography>
      </Box>
    </AppLayout>
  );
};

export default LibraryPage;
