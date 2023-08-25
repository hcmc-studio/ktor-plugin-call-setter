package studio.hcmc.plugin

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

private val _applicationCall = ThreadLocal<ApplicationCall>()

/**
 * 현재 실행 중인 요청. Ktor 실행 영역을 벗어날 때 작동이 보장되지 않음.
 */
val currentApplicationCall: ApplicationCall get() = _applicationCall.get()

val CallSetter = createApplicationPlugin("CallSetter", {}) {
    on(TracerHook()) { call, block ->
        withContext(_applicationCall.asContextElement(call)) {
            block()
        }
    }
}

/**
 * **See Also:** [StackOverflow](https://stackoverflow.com/questions/72056279/ktor-2-how-to-add-thread-local-data-for-a-request)
 */
private class TracerHook : Hook<suspend (ApplicationCall, suspend () -> Unit) -> Unit> {
    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall, suspend () -> Unit) -> Unit
    ) {
        val phase = ApplicationCallPipeline.Monitoring
        val namedPhase = PipelinePhase("${phase.name}Trace")
        pipeline.insertPhaseBefore(phase, namedPhase)
        pipeline.intercept(namedPhase) {
            handler(call, ::proceed)
        }
    }
}