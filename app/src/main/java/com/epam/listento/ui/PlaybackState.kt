package com.epam.listento.ui

sealed class PlaybackState {
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Stopped : PlaybackState()
    object None : PlaybackState()
}