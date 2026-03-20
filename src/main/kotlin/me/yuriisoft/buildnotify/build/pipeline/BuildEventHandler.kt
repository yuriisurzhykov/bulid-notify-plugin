package me.yuriisoft.buildnotify.build.pipeline

import com.intellij.build.events.BuildEvent

/**
 * Handles a specific subset of IntelliJ [BuildEvent]s and converts them to
 * typed [OutgoingBuildEvent]s for the WebSocket wire protocol.
 *
 * Implementations form a **chain of responsibility**: the pipeline invokes
 * handlers in order and stops at the first one that claims the event.
 *
 * ### Return semantics
 *
 * | Return value         | Meaning                                        |
 * |----------------------|------------------------------------------------|
 * | non-null list        | Event handled; list may be empty (= discard)   |
 * | `null`               | Not my responsibility — pass to the next handler|
 *
 * ### Implementation guidelines
 *
 * - Return `null` as early as possible when the event type doesn't match.
 * - Return `emptyList()` to explicitly suppress an event (e.g. [OutputBuildEvent]).
 * - A handler may produce **multiple** outgoing events from a single input
 *   (e.g. a finish event that also carries a diagnostic).
 * - Handlers must be **stateless** and **thread-safe** — the pipeline may call
 *   them from different build sessions concurrently.
 */
fun interface BuildEventHandler {

    /**
     * Inspects the [context] and optionally converts it into outgoing events.
     *
     * @param context build event together with its session metadata
     * @return a list of outgoing events if handled, or `null` to delegate downstream
     */
    fun handle(context: BuildEventContext): List<OutgoingBuildEvent>?
}

