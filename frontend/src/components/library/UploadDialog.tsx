import React, { useRef, useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  LinearProgress,
  Box,
  Alert,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

interface UploadDialogProps {
  open: boolean;
  onClose: () => void;
  onUpload: (file: File) => Promise<void>;
  uploadProgress: number;
  uploading: boolean;
  uploadError: string | null;
}

const UploadDialog: React.FC<UploadDialogProps> = ({
  open,
  onClose,
  onUpload,
  uploadProgress,
  uploading,
  uploadError,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);

  // Reset state when dialog closes
  useEffect(() => {
    if (!open) {
      setSelectedFile(null);
      setValidationError(null);
    }
  }, [open]);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    setValidationError(null);

    if (!file) {
      return;
    }

    // Validate file type
    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith('.cbz') && !fileName.endsWith('.cbr')) {
      setValidationError('Invalid file type. Please select a .cbz or .cbr file.');
      return;
    }

    // Validate file size (500MB = 524288000 bytes)
    const maxSize = 524288000;
    if (file.size > maxSize) {
      setValidationError(`File too large. Maximum size is 500MB (selected: ${(file.size / 1024 / 1024).toFixed(2)}MB).`);
      return;
    }

    setSelectedFile(file);
  };

  const handleUploadClick = async () => {
    if (!selectedFile) return;

    try {
      await onUpload(selectedFile);
      // Dialog will close via parent component after successful upload
    } catch {
      // Error handled by Redux state
    }
  };

  const handleClose = () => {
    if (!uploading) {
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      disableEscapeKeyDown={uploading}
    >
      <DialogTitle>Upload Comic</DialogTitle>
      <DialogContent>
        <input
          ref={fileInputRef}
          type="file"
          accept=".cbz,.cbr"
          style={{ display: 'none' }}
          onChange={handleFileSelect}
          disabled={uploading}
        />

        <Box sx={{ mb: 2 }}>
          <Button
            variant="outlined"
            startIcon={<CloudUploadIcon />}
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
            fullWidth
          >
            Select File
          </Button>
        </Box>

        {validationError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {validationError}
          </Alert>
        )}

        {uploadError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {uploadError}
          </Alert>
        )}

        {selectedFile && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Selected file:
            </Typography>
            <Typography variant="body1" sx={{ fontWeight: 500 }}>
              {selectedFile.name}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
            </Typography>
          </Box>
        )}

        {uploading && (
          <Box sx={{ mt: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Box sx={{ width: '100%', mr: 1 }}>
                <LinearProgress variant="determinate" value={uploadProgress} />
              </Box>
              <Box sx={{ minWidth: 35 }}>
                <Typography variant="body2" color="text.secondary">
                  {uploadProgress}%
                </Typography>
              </Box>
            </Box>
            <Typography variant="caption" color="text.secondary">
              Uploading... Please do not close this dialog.
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={uploading}>
          Cancel
        </Button>
        <Button
          onClick={handleUploadClick}
          variant="contained"
          disabled={!selectedFile || uploading || !!validationError}
        >
          Upload
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default UploadDialog;
