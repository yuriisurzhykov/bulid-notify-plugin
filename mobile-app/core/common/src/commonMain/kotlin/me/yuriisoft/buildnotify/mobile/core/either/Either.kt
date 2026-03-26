package me.yuriisoft.buildnotify.mobile.core.either

/**
 * Represents a value of one of two possible types.
 *
 * By convention [Left] carries a failure / error, [Right] carries a success value.
 * This mirrors the Haskell / Arrow-kt tradition and keeps "success on the right".
 *
 * Prefer the factory functions on the companion over constructing [Left]/[Right] directly:
 * ```kotlin
 * Either.success(data)
 * Either.failure(error)
 * Either.catching { riskyCall() }
 * ```
 */
sealed class Either<out L, out R> {

    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    val isSuccess: Boolean get() = this is Right
    val isFailure: Boolean get() = this is Left

    companion object {
        fun <R> success(value: R): Either<Nothing, R> = Right(value)
        fun <L> failure(value: L): Either<L, Nothing> = Left(value)

        /**
         * Wraps [block] in a try/catch and returns [Right] on success,
         * [Left<Throwable>] if the block throws.
         */
        inline fun <R> catching(block: () -> R): Either<Throwable, R> =
            runCatching(block).toEither()
    }
}

/** Shorthand alias: `Success<String>` reads more naturally than `Either.Right<String>`. */
typealias Success<R> = Either.Right<R>

/** Shorthand alias: `Failure<Throwable>` reads more naturally than `Either.Left<Throwable>`. */
typealias Failure<L> = Either.Left<L>

// ─── Transformation ───────────────────────────────────────────────────────────

/**
 * Transforms the [Either.Right] value; [Either.Left] passes through untouched.
 * Equivalent to `flatMap { Either.success(transform(it)) }`.
 */
inline fun <L, R, T> Either<L, R>.map(transform: (R) -> T): Either<L, T> = when (this) {
    is Either.Left -> this
    is Either.Right -> Either.Right(transform(value))
}

/**
 * Transforms the [Either.Left] value; [Either.Right] passes through untouched.
 * Useful for normalising different error types to a common domain error.
 */
inline fun <L, R, T> Either<L, R>.mapLeft(transform: (L) -> T): Either<T, R> = when (this) {
    is Either.Left -> Either.Left(transform(value))
    is Either.Right -> this
}

/**
 * Chains two [Either]-returning computations.
 * Short-circuits on the first [Either.Left].
 */
inline fun <L, R, T> Either<L, R>.flatMap(transform: (R) -> Either<L, T>): Either<L, T> =
    when (this) {
        is Either.Left -> this
        is Either.Right -> transform(value)
    }

// ─── Extraction ───────────────────────────────────────────────────────────────

/**
 * Collapses both branches into a single value of type [T].
 *
 * ```kotlin
 * val message = result.fold(
 *     onLeft  = { error -> "Failed: ${error.message}" },
 *     onRight = { data  -> "OK: $data" },
 * )
 * ```
 */
inline fun <L, R, T> Either<L, R>.fold(
    onLeft: (L) -> T,
    onRight: (R) -> T,
): T = when (this) {
    is Either.Left -> onLeft(value)
    is Either.Right -> onRight(value)
}

/** Returns the [Either.Right] value, or the result of [default] if this is [Either.Left]. */
inline fun <L, R> Either<L, R>.getOrElse(default: () -> R): R = when (this) {
    is Either.Left -> default()
    is Either.Right -> value
}

/** Returns the [Either.Right] value, or `null` if this is [Either.Left]. */
fun <L, R> Either<L, R>.getOrNull(): R? = when (this) {
    is Either.Left -> null
    is Either.Right -> value
}

/** Returns the [Either.Left] value, or `null` if this is [Either.Right]. */
fun <L, R> Either<L, R>.leftOrNull(): L? = when (this) {
    is Either.Left -> value
    is Either.Right -> null
}

// ─── Side effects ─────────────────────────────────────────────────────────────

/**
 * Runs [action] if this is [Either.Right]; returns `this` for chaining.
 *
 * ```kotlin
 * result
 *     .onSuccess { data -> updateUi(data) }
 *     .onFailure { err  -> logError(err)  }
 * ```
 */
inline fun <L, R> Either<L, R>.onSuccess(action: (R) -> Unit): Either<L, R> {
    if (this is Either.Right) action(value)
    return this
}

/** Runs [action] if this is [Either.Left]; returns `this` for chaining. */
inline fun <L, R> Either<L, R>.onFailure(action: (L) -> Unit): Either<L, R> {
    if (this is Either.Left) action(value)
    return this
}

// ─── Recovery ─────────────────────────────────────────────────────────────────

/**
 * Converts [Either.Left] into [Either.Right] using [transform].
 * Useful for providing a fallback value when an operation fails.
 */
inline fun <L, R> Either<L, R>.recover(transform: (L) -> R): Either<L, R> = when (this) {
    is Either.Left -> Either.Right(transform(value))
    is Either.Right -> this
}

/**
 * Like [recover] but allows returning another [Either], enabling fallback chains.
 */
inline fun <L, R> Either<L, R>.recoverWith(transform: (L) -> Either<L, R>): Either<L, R> =
    when (this) {
        is Either.Left -> transform(value)
        is Either.Right -> this
    }

// ─── Kotlin Result interop ────────────────────────────────────────────────────

/**
 * Converts a Kotlin [Result] to [Either].
 * Particularly useful inside [UseCase] implementations:
 * ```kotlin
 * override suspend fun execute(params: NoParams) =
 *     runCatching { repository.load() }.toEither()
 * ```
 */
fun <R> Result<R>.toEither(): Either<Throwable, R> =
    fold(onSuccess = { Either.Right(it) }, onFailure = { Either.Left(it) })
