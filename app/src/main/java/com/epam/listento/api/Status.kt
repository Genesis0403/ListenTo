package com.epam.listento.api

enum class Status {
    SUCCESS, ERROR;

    fun isSuccess() = this == SUCCESS
    fun isError() = this == ERROR
}
