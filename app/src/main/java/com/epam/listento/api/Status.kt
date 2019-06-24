package com.epam.listento.api

enum class Status {
    LOADING, SUCCESS, ERROR;

    fun isLoading() = this == LOADING
}