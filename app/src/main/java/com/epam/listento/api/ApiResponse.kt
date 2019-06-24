package com.epam.listento.api

class ApiResponse<T>(
    val status: Status,
    val body: T?,
    val error: String?
) {
    companion object {
        fun <T> success(body: T?) = ApiResponse(Status.SUCCESS, body, null)
        fun <T> error(error: String) = ApiResponse<T>(Status.ERROR, null, error)
        fun <T> loading() = ApiResponse<T>(Status.LOADING, null, null)
    }
}