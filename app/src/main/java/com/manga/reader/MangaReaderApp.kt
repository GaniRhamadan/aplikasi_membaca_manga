package com.manga.reader

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MangaReaderApp : Application(), SingletonImageLoader.Factory {

    companion object {
        private var _instance: MangaReaderApp? = null
        val instance: MangaReaderApp
            get() = _instance ?: throw IllegalStateException("Application is not initialized")
        val instanceOrNull: MangaReaderApp?
            get() = _instance
    }

    @OptIn(coil3.annotation.DelicateCoilApi::class)
    override fun onCreate() {
        super.onCreate()
        _instance = this
        try {
            SingletonImageLoader.setUnsafe(newImageLoader(this))
        } catch (_: Exception) {}
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .dns(com.manga.reader.data.network.MangaDns)
            .addInterceptor { chain ->
                val original = chain.request()
                val urlStr = original.url.toString()
                val referer = when {
                    urlStr.contains("mangadex.org") -> "https://mangadex.org/"
                    urlStr.contains("komikindo") -> "https://komikindo.ch/"
                    else -> "https://google.com/"
                }
                val req = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Referer", referer)
                    .build()
                chain.proceed(req)
            }
            .cache(Cache(File(cacheDir, "manga_http_cache"), 300L * 1024 * 1024))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("manga_image_cache"))
                    .maxSizeBytes(300L * 1024 * 1024)
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }
}
