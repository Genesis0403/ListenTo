package com.epam.listento.model.player

sealed class PlaybackState {
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Stopped : PlaybackState()
    object None : PlaybackState()
}