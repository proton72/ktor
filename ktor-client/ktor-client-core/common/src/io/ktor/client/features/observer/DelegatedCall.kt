package io.ktor.client.features.observer

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlin.coroutines.*

/**
 * Wrap existing [HttpClientCall] with new [content].
 */
@Deprecated(
    "Parameter [shouldCloseOrigin] is deprecated",
    ReplaceWith("wrapWithContent(content)"),
    level = DeprecationLevel.ERROR
)
fun HttpClientCall.wrapWithContent(
    content: ByteReadChannel,
    shouldCloseOrigin: Boolean
): HttpClientCall = wrapWithContent(content)

/**
 * Wrap existing [HttpClientCall] with new [content].
 */
@KtorExperimentalAPI
fun HttpClientCall.wrapWithContent(
    content: ByteReadChannel
): HttpClientCall = HttpClientCall(client).apply {
    request = DelegatedRequest(this, this@wrapWithContent.request)
    response = DelegatedResponse(content, this, this@wrapWithContent.response)
}

internal class DelegatedRequest(
    override val call: HttpClientCall,
    origin: HttpRequest
) : HttpRequest by origin

internal class DelegatedResponse(
    override val content: ByteReadChannel,
    override val call: HttpClientCall,
    private val origin: HttpResponse
) : HttpResponse {
    private val completionState: CompletableDeferred<Unit> = CompletableDeferred(origin.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = origin.coroutineContext + completionState

    override val status: HttpStatusCode get() = origin.status

    override val version: HttpProtocolVersion get() = origin.version

    override val requestTime: GMTDate get() = origin.requestTime

    override val responseTime: GMTDate get() = origin.responseTime

    override val headers: Headers get() = origin.headers

    override fun close() {
        completionState.complete(Unit)
    }
}
