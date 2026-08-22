package io.legado.app.help.update

import androidx.annotation.Keep
import io.legado.app.BuildConfig
import io.legado.app.constant.AppConst
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.CoroutineScope

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    private const val CUSTOM_RELEASES_URL =
        "https://api.github.com/repos/xiaoxin0819/legado/releases/latest"

    private suspend fun getLatestRelease(): GithubRelease {
        val res = okHttpClient.newCallResponse {
            url(CUSTOM_RELEASES_URL)
        }
        if (!res.isSuccessful) {
            throw NoStackTraceException("获取新版本出错(${res.code})")
        }
        val body = res.body.text()
        if (body.isBlank()) {
            throw NoStackTraceException("获取新版本出错")
        }
        return GSON.fromJsonObject<GithubRelease>(body).getOrElse {
            throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
        }
    }

    private fun customAssetSuffix(): String? = when (BuildConfig.APPLICATION_ID) {
        "com.legado.app.lyx" -> "_readingLYX.apk"
        "com.legado.app.releaseB" -> "_readingB.apk"
        "com.legado.app.debug" -> "_debug.apk"
        else -> null
    }

    private suspend fun checkCustomUpdate(): AppUpdate.UpdateInfo? {
        val suffix = customAssetSuffix() ?: return null
        val release = getLatestRelease()
        if (release.isPreRelease) return null
        // Use the APK filename as the source of truth. Release tags in this
        // repository may be dates such as v2026.07.07.
        val latest = release.gitReleaseToAppReleaseInfo(preferAssetVersion = true)
            .firstOrNull {
                it.name.endsWith(suffix) &&
                    compareReleaseVersions(it.versionName, AppConst.appInfo.versionName) > 0
            } ?: return null
        return AppUpdate.UpdateInfo(
            latest.versionName,
            latest.note,
            latest.downloadUrl,
            latest.name
        )
    }

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            checkCustomUpdate() ?: throw NoStackTraceException("已是最新版本")
        }.timeout(10000)
    }
}
