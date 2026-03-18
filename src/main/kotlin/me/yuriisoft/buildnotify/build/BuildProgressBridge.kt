package me.yuriisoft.buildnotify.build

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runToolbar.getDisplayName
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.APP)
class BuildProgressBridge : ExecutionListener {

    private val logger = thisLogger()

    override fun processStartScheduled(
        executorId: String,
        env: ExecutionEnvironment
    ) {
        logger.warn("processStartScheduled: execution ID=${env.executionId}, name=${env.project.name}, display name=${env.getDisplayName()}")
        super.processStartScheduled(executorId, env)
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        logger.warn("processStarting: execution ID=${env.executionId}, name=${env.project.name}")
        super.processStarting(executorId, env)
    }

    override fun processStarting(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler
    ) {
        logger.warn("processStarting: executorID=$executorId, execution ID=${env.executionId}, name=${env.project.name}, handler ${handler.javaClass}")
        super.processStarting(executorId, env, handler)
    }

    override fun processNotStarted(
        executorId: String,
        env: ExecutionEnvironment
    ) {
        logger.warn("processNotStarted: execution ID=$executorId, ${env.project.name}")
        super.processNotStarted(executorId, env)
    }

    override fun processNotStarted(
        executorId: String,
        env: ExecutionEnvironment,
        cause: Throwable?
    ) {
        logger.warn(
            "processNotStarted: executorId=$executorId, execution ID=${env.executionId}, name=${env.project.name}",
            cause
        )
        super.processNotStarted(executorId, env, cause)
    }

    override fun processStarted(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler
    ) {
        logger.warn("processStarted: execution ID=${env.executionId}, name=${env.project.name}")
        super.processStarted(executorId, env, handler)
    }

    override fun processTerminating(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler
    ) {
        logger.warn("processTerminating: executorId=${executorId}, execution ID=${env.executionId}, name=${env.project.name}")
        super.processTerminating(executorId, env, handler)
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ) {
        logger.warn("processTerminated: $executorId, execution ID=${env.executionId}, name=${env.project.name}, exitCode=$exitCode")
        super.processTerminated(executorId, env, handler, exitCode)
    }
}