package com.epam.listento.api

class ApiResponse<T>(
    val status: Status,
    val body: T?,
    val error: String?
) {
    companion object {
        fun <T> success(body: T?): ApiResponse<T> = ApiResponse(Status.SUCCESS, body, null)
        fun <T> error(error: String, body: T? = null): ApiResponse<T> = ApiResponse(Status.ERROR, body, error)
    }
}
