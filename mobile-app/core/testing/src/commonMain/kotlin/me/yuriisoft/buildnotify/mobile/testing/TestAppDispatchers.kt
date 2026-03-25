package me.yuriisoft.buildnotify.mobile.testing

import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test-only [AppDispatchers] that runs all coroutines eagerly on
 * a single [UnconfinedTestDispatcher]. Pass `main` to
 * [Dispatchers.setMain] in `@BeforeTest` for ViewModel tests.
 */
class TestAppDispatchers : me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers.Abstract(
    main = UnconfinedTestDispatcher(),
    io = UnconfinedTestDispatcher(),
    default = UnconfinedTestDispatcher(),
)
