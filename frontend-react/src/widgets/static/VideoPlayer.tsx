/**
 * Video Player Widget - Video playback with controls
 * Supports multiple video sources and formats
 */
import { useState, useRef } from 'react'
import { Box, Typography, Paper, IconButton, Slider } from '@mui/material'
import {
  PlayArrow,
  Pause,
  VolumeUp,
  VolumeOff,
  Fullscreen,
} from '@mui/icons-material'
import { WidgetComponentProps, WidgetTypeDescriptor } from '@/types/dashboard'
import { registerWidget } from '@/widgets/widgetRegistry'

function VideoPlayer({ widget, data }: WidgetComponentProps) {
  const config = widget.config
  const videoRef = useRef<HTMLVideoElement>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [isMuted, setIsMuted] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(0.7)

  // Demo video URL - in production, this would come from datasources
  const videoUrl = config.settings?.videoUrl || 'https://www.w3schools.com/html/mov_bbb.mp4'
  const posterUrl = config.settings?.posterUrl || ''

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause()
      } else {
        videoRef.current.play()
      }
      setIsPlaying(!isPlaying)
    }
  }

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted
      setIsMuted(!isMuted)
    }
  }

  const handleVolumeChange = (_: Event, newValue: number | number[]) => {
    const vol = newValue as number
    setVolume(vol)
    if (videoRef.current) {
      videoRef.current.volume = vol
    }
  }

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime)
    }
  }

  const handleLoadedMetadata = () => {
    if (videoRef.current) {
      setDuration(videoRef.current.duration)
    }
  }

  const handleSeek = (_: Event, newValue: number | number[]) => {
    const time = newValue as number
    if (videoRef.current) {
      videoRef.current.currentTime = time
      setCurrentTime(time)
    }
  }

  const handleFullscreen = () => {
    if (videoRef.current) {
      if (document.fullscreenElement) {
        document.exitFullscreen()
      } else {
        videoRef.current.requestFullscreen()
      }
    }
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <Paper elevation={1} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {config.showTitle && (
        <Box sx={{ p: 2, pb: 1 }}>
          <Typography variant="h6" sx={{ fontSize: '16px', fontWeight: 'bold', color: '#0F3E5C' }}>
            {config.title || 'Video Player'}
          </Typography>
        </Box>
      )}

      <Box sx={{ flex: 1, position: 'relative', bgcolor: '#000', display: 'flex', alignItems: 'center' }}>
        <video
          ref={videoRef}
          src={videoUrl}
          poster={posterUrl}
          onTimeUpdate={handleTimeUpdate}
          onLoadedMetadata={handleLoadedMetadata}
          onEnded={() => setIsPlaying(false)}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
          }}
        />

        {/* Play button overlay (when not playing) */}
        {!isPlaying && (
          <IconButton
            onClick={togglePlay}
            sx={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              bgcolor: 'rgba(0,0,0,0.6)',
              color: 'white',
              width: 70,
              height: 70,
              '&:hover': { bgcolor: 'rgba(0,0,0,0.8)' },
            }}
          >
            <PlayArrow sx={{ fontSize: 40 }} />
          </IconButton>
        )}
      </Box>

      {/* Controls */}
      <Box sx={{ p: 2, bgcolor: '#F5F5F5' }}>
        {/* Progress bar */}
        <Slider
          value={currentTime}
          max={duration || 100}
          onChange={handleSeek}
          sx={{
            mb: 1,
            '& .MuiSlider-thumb': { width: 14, height: 14 },
            '& .MuiSlider-track': { bgcolor: '#0F3E5C' },
          }}
        />

        {/* Control buttons */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <IconButton size="small" onClick={togglePlay}>
              {isPlaying ? <Pause /> : <PlayArrow />}
            </IconButton>

            <Typography variant="caption" color="text.secondary" sx={{ minWidth: 80 }}>
              {formatTime(currentTime)} / {formatTime(duration)}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flex: 1, maxWidth: 200, ml: 2 }}>
            <IconButton size="small" onClick={toggleMute}>
              {isMuted ? <VolumeOff fontSize="small" /> : <VolumeUp fontSize="small" />}
            </IconButton>
            <Slider
              value={isMuted ? 0 : volume}
              max={1}
              step={0.1}
              onChange={handleVolumeChange}
              sx={{
                '& .MuiSlider-thumb': { width: 12, height: 12 },
                '& .MuiSlider-track': { bgcolor: '#0F3E5C' },
              }}
            />
          </Box>

          <IconButton size="small" onClick={handleFullscreen}>
            <Fullscreen fontSize="small" />
          </IconButton>
        </Box>
      </Box>
    </Paper>
  )
}

const descriptor: WidgetTypeDescriptor = {
  id: 'video_player',
  name: 'Video Player',
  description: 'Video playback with full controls and formats support',
  type: 'static',
  tags: ['video', 'player', 'media', 'stream'],
}

registerWidget(descriptor, VideoPlayer)
export default VideoPlayer
