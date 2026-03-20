package me.yuriisoft.buildnotify.build.pipeline

/**
 * Ordered chain of [BuildEventHandler]s that processes a single build event.
 *
 * Implements the **Chain of Responsibility** pattern: handlers are tried in
 * registration order, and the first handler that returns a non-null result
 * "claims" the event. If no handler claims it, the event is silently dropped
 * (empty list).
 *
 * This class is **pure logic with no platform dependencies**, making it trivially
 * testable — construct with any set of handlers, no IDE required.
 *
 * ### Handler ordering contract
 *
 * 1. Discard handlers first (cheap short-circuit for noise events).
 * 2. More-specific handlers before less-specific ones
 *    (e.g. [FileDiagnosticHandler][me.yuriisoft.buildnotify.build.pipeline.handler.FileDiagnosticHandler]
 *    before [DiagnosticHandler][me.yuriisoft.buildnotify.build.pipeline.handler.DiagnosticHandler],
 *    because `FileMessageEvent` extends `MessageEvent`).
 *
 * @property handlers ordered list of handlers; first non-null result wins
 */
class BuildEventChain(private val handlers: List<BuildEventHandler>) {

    /**
     * Runs [context] through the handler chain.
     *
     * @return the list of [OutgoingBuildEvent]s produced by the first matching
     *         handler, or an empty list if no handler claimed the event
     */
    fun process(context: BuildEventContext): List<OutgoingBuildEvent> =
        handlers.firstNotNullOfOrNull { it.handle(context) } ?: emptyList()
}
