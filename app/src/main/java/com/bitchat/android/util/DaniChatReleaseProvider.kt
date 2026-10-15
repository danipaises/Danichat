package com.bitchat.android.util

/** No release endpoint is trusted for this fork until its infrastructure is configured. */
internal object DaniChatReleaseProvider : LatestReleaseProvider {
    override suspend fun latestRelease(): Result<GitHubReleaseClient.ReleaseSnapshot> =
        Result.failure(
            ApkDownloadException(
                message = "DaniChat release distribution is not configured.",
                reason = ApkDownloadFailureReason.NoSources,
                retryable = false
            )
        )
}
