package io.github.iandbrown.trials.logic

import io.github.iandbrown.trials.database.Setting

internal expect class EmailSender(setting: Setting) {
    suspend fun send(from: String, to: String, subject: String, body: String)
}
