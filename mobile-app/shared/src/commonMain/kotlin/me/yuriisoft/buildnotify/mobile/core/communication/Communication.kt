package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Read-only view of a [StateFlow]-backed communication channel.
 *
 * Expose this type from ViewModels and Use Cases so the UI can observe
 * state without being able to push new values directly.
 *
 * ```kotlin
 * class BuildStatusViewModel : ViewModel() {
 *     private val _state = Communication.State<UiState>(UiState.Loading)
 *     val state: Communication<UiState> = _state        // ← exposed as read-only
 * }
 * ```
 */
interface Communication<out T> {
    fun observe(): StateFlow<T>
}

/**
 * Read-write extension: allows emitting new values into the channel.
 * Keep this private to the component that *owns* the state.
 */
interface MutableCommunication<T> : Communication<T> {
    suspend fun emit(value: T)
    fun emitSync(value: T)
}

/**
 * [StateFlow]-backed communication channel for **UI state**.
 *
 * Semantics:
 *   - Always has a current value ([initial]).
 *   - New collectors immediately receive the latest emission (replay = 1).
 *   - Duplicate values are ignored (distinctUntilChanged).
 *
 * Ideal for: screen state, loading flags, form values.
 *
 * ```kotlin
 * private val _uiState = Communication.State<DiscoveryUiState>(DiscoveryUiState.Idle)
 *
 * // Emit from a coroutine:
 * _uiState.emit(DiscoveryUiState.Loading)
 *
 * // Observe in a Composable:
 * val state by _uiState.observe().collectAsState()
 * ```
 */
class StateCommunication<T>(initial: T) : MutableCommunication<T> {

    private val _state = MutableStateFlow(initial)

    override fun observe(): StateFlow<T> = _state.asStateFlow()

    override suspend fun emit(value: T) {
        _state.emit(value)
    }

    override fun emitSync(value: T) {
        _state.value = value
    }
}

/**
 * Read-only view of a [SharedFlow]-backed one-shot event channel.
 * Use [MutableEventCommunication] to emit events.
 */
interface EventCommunication<out T> {
    fun observe(): SharedFlow<T>
}

/**
 * [SharedFlow]-backed channel for **one-shot UI events** that must not be replayed.
 *
 * Semantics:
 *   - No current value — collectors only receive emissions made *after* they subscribe.
 *   - Configurable [extraBufferCapacity] to avoid dropping events under backpressure.
 *   - Emissions do NOT deduplicate (unlike [StateCommunication]).
 *
 * Ideal for: navigation commands, snackbar messages, dialogs, vibration triggers.
 *
 * ```kotlin
 * private val _events = MutableEventCommunication<NavigationEvent>()
 *
 * // Emit from a coroutine:
 * _events.emit(NavigationEvent.OpenBuildStatus(host, port))
 *
 * // Observe in a Composable (LaunchedEffect):
 * LaunchedEffect(Unit) {
 *     viewModel.events.observe().collect { event -> handleEvent(event) }
 * }
 * ```
 */
class MutableEventCommunication<T>(
    extraBufferCapacity: Int = DEFAULT_BUFFER,
) : EventCommunication<T> {

    private val _events = MutableSharedFlow<T>(extraBufferCapacity = extraBufferCapacity)

    override fun observe(): SharedFlow<T> = _events.asSharedFlow()

    suspend fun emit(value: T) {
        _events.emit(value)
    }

    /**
     * Attempts to emit without suspending.
     * Returns `false` if the buffer is full and the event is dropped.
     * Prefer [emit] in a coroutine whenever possible.
     */
    fun tryEmit(value: T): Boolean = _events.tryEmit(value)

    private companion object {
        const val DEFAULT_BUFFER = 64
    }
}

/**
 * Semantic alias: use in ViewModel declarations to signal this channel carries UI state.
 *
 * ```kotlin
 * private val _state: UiStateCommunication<HomeUiState> =
 *     StateCommunication(HomeUiState.Loading)
 * ```
 */
typealias UiStateCommunication<T> = StateCommunication<T>

/**
 * Semantic alias: use in ViewModel declarations to signal this channel carries
 * fire-and-forget UI events.
 */
typealias UiEventCommunication<T> = MutableEventCommunication<T>
