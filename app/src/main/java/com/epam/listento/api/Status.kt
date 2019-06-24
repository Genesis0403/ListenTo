package com.epam.listento.api

enum class Status {
    SUCCESS, ERROR;

    fun isSuccsess() = this == SUCCESS
    fun isError() = this == ERROR
}
