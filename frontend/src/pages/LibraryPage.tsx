import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Grid,
  Typography,
  Button,
  CircularProgress,
  Alert,
  Snackbar,
  Toolbar,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import AppLayout from '../components/layout/AppLayout';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
  fetchFiles,
  uploadFile,
  deleteFile,
  clearUploadState,
} from '../store/slices/filesSlice';
import { ComicFile } from '../services/fileService';
import ComicCard from '../components/library/ComicCard';
import UploadDialog from '../components/library/UploadDialog';
import DeleteConfirmDialog from '../components/library/DeleteConfirmDialog';
import * as progressService from '../services/progressService';

const LibraryPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const { items: comics, status, error } = useAppSelector((state) => state.files);
  const { uploadProgress, uploadStatus, uploadError } = useAppSelector((state) => state.files);

  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [fileToDelete, setFileToDelete] = useState<ComicFile | null>(null);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState<'success' | 'error'>('success');
  const [progressMap, setProgressMap] = useState<Record<number, number>>({});

  // Fetch files on mount
  useEffect(() => {
    dispatch(fetchFiles());
  }, [dispatch]);

  // Fetch reading progress after files are loaded
  useEffect(() => {
    const fetchProgress = async () => {
      if (status === 'succeeded' && comics.length > 0) {
        try {
          const progressList = await progressService.getAllProgress();
          const map: Record<number, number> = {};
          progressList.forEach((progress) => {
            map[progress.fileId] = progress.currentPage;
          });
          setProgressMap(map);
        } catch (error) {
          // Silently fail - progress is not critical
          console.error('Failed to fetch reading progress:', error);
        }
      }
    };
    fetchProgress();
  }, [status, comics.length]);

  // Handle upload success
  useEffect(() => {
    if (uploadStatus === 'succeeded') {
      setUploadDialogOpen(false);
      setSnackbarMessage('File uploaded successfully');
      setSnackbarSeverity('success');
      setSnackbarOpen(true);
      dispatch(clearUploadState());
    }
  }, [uploadStatus, dispatch]);

  const handleUploadClick = () => {
    setUploadDialogOpen(true);
  };

  const handleUploadDialogClose = () => {
    if (uploadStatus !== 'uploading') {
      setUploadDialogOpen(false);
      dispatch(clearUploadState());
    }
  };

  const handleUpload = async (file: File) => {
    try {
      await dispatch(uploadFile(file)).unwrap();
    } catch (error) {
      const message = error && typeof error === 'string' ? error : 'Failed to upload file';
      setSnackbarMessage(message);
      setSnackbarSeverity('error');
      setSnackbarOpen(true);
    }
  };

  const handleOpenComic = (fileId: number) => {
    navigate(`/viewer/${fileId}`);
  };

  const handleDeleteClick = (file: ComicFile) => {
    setFileToDelete(file);
    setDeleteDialogOpen(true);
  };

  const handleDeleteDialogClose = () => {
    setDeleteDialogOpen(false);
    setFileToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!fileToDelete) return;

    try {
      await dispatch(deleteFile(fileToDelete.id)).unwrap();
      setSnackbarMessage('File deleted successfully');
      setSnackbarSeverity('success');
      setSnackbarOpen(true);
      handleDeleteDialogClose();
    } catch (error) {
      const message = error && typeof error === 'string' ? error : 'Failed to delete file';
      setSnackbarMessage(message);
      setSnackbarSeverity('error');
      setSnackbarOpen(true);
    }
  };

  const handleSnackbarClose = () => {
    setSnackbarOpen(false);
  };

  return (
    <AppLayout>
      <Box sx={{ mt: -3, ml: -3, mr: -3 }}>
        {/* Toolbar for Upload Button */}
        <Toolbar sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Library
          </Typography>
          <Button
            variant="contained"
            startIcon={<CloudUploadIcon />}
            onClick={handleUploadClick}
          >
            Upload
          </Button>
        </Toolbar>

        {/* Main Content */}
        <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
          {/* Loading State */}
          {status === 'loading' && (
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '60vh',
              }}
            >
              <CircularProgress />
            </Box>
          )}

          {/* Error State */}
          {status === 'failed' && error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          {/* Empty State */}
          {status === 'succeeded' && comics.length === 0 && (
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '60vh',
                textAlign: 'center',
              }}
            >
              <Typography variant="h5" color="text.secondary" gutterBottom>
                No comics yet
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Upload your first comic to get started
              </Typography>
              <Button
                variant="contained"
                startIcon={<CloudUploadIcon />}
                onClick={handleUploadClick}
              >
                Upload Comic
              </Button>
            </Box>
          )}

          {/* Comic Grid */}
          {status === 'succeeded' && comics.length > 0 && (
            <Grid container spacing={3}>
              {comics.map((comic) => (
                <Grid item xs={12} sm={6} md={4} lg={3} xl={2} key={comic.id}>
                  <ComicCard
                    file={comic}
                    onOpen={handleOpenComic}
                    onDelete={handleDeleteClick}
                    currentPage={progressMap[comic.id]}
                  />
                </Grid>
              ))}
            </Grid>
          )}
        </Container>
      </Box>

      {/* Upload Dialog */}
      <UploadDialog
        open={uploadDialogOpen}
        onClose={handleUploadDialogClose}
        onUpload={handleUpload}
        uploadProgress={uploadProgress}
        uploading={uploadStatus === 'uploading'}
        uploadError={uploadError}
      />

      {/* Delete Confirmation Dialog */}
      <DeleteConfirmDialog
        open={deleteDialogOpen}
        onClose={handleDeleteDialogClose}
        onConfirm={handleDeleteConfirm}
        fileName={fileToDelete?.originalFilename || ''}
      />

      {/* Success/Error Snackbar */}
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbarSeverity}
          sx={{ width: '100%' }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </AppLayout>
  );
};

export default LibraryPage;
