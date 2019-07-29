package com.epam.listento.repository

import android.net.Uri
import android.os.Environment
import android.util.Log
import com.epam.listento.api.YandexService
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "FILE_REPOSITORY"
private const val LOCAL_DIRECTORY = "ListenToMusic"

class FileRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val contextProvider: ContextProvider
) : FileRepository {

    private var job: Job? = null

    override fun downloadTrack(
        trackName: String,
        audioUrl: String,
        completion: (Response<Uri>) -> Unit
    ) {
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = service.downloadTrack(audioUrl)
                if (response.isSuccessful) {
                    val url = response.body()?.let {
                        downloadFile(trackName, it)
                    }
                    withContext(Dispatchers.Main) {
                        completion(Response.success(url))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        completion(Response.error(response.code(), response.errorBody()))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "$e")
            }
        }
    }

    private fun downloadFile(trackName: String, response: ResponseBody): Uri {
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
        return Uri.fromFile(file)
    }

    private fun createFile(trackName: String): File {
        val context = contextProvider.context()
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            LOCAL_DIRECTORY
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, trackName)
    }
}