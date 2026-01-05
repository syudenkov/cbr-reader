import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import theme from './theme/theme';

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" component="h1">
          CBR/CBZ Viewer
        </Typography>
        <Typography variant="body1" sx={{ mt: 2 }}>
          Welcome to the Comic Book Reader
        </Typography>
      </Box>
    </ThemeProvider>
  );
}

export default App;
