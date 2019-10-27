package com.epam.listento.repository

import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.utils.ContextProvider
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "FILE_REPOSITORY"

class FileRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val contextProvider: ContextProvider
) : FileRepository {

    @WorkerThread
    override suspend fun downloadTrack(
        trackName: String,
        audioUrl: String
    ): ApiResponse<String> {
        return try {
            val response = service.downloadTrack(audioUrl)
            if (response.isSuccessful && response.body() != null) {
                val url = downloadFile(trackName, response.body()!!)
                ApiResponse.success(url)
            } else {
                ApiResponse.error(response.errorBody().toString())
            }
        } catch (e: Throwable) {
            Log.e(TAG, "$e")
            ApiResponse.error(e, e.message)
        }
    }

    @WorkerThread
    private fun downloadFile(trackName: String, response: ResponseBody): String {
        val file = createFile(trackName)
        try {
            val fileReader = ByteArray(4096)
            response.byteStream().use { inputStream ->
                FileOutputStream(file).use { outStream ->
                    while (true) {
                        val read = inputStream.read(fileReader)
                        if (read == -1) {
                            break
                        }
                        outStream.write(fileReader, 0, read)
                    }
                    outStream.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "$e")
        }
        return file.path
    }

    private fun createFile(trackName: String): File {
        val context = contextProvider.context()
        val localDir = context.getString(R.string.app_local_dir)
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            localDir
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, trackName)
    }
}
