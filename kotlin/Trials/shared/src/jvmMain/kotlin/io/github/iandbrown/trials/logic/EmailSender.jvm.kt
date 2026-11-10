package io.github.iandbrown.trials.logic

import at.quickme.kotlinmailer.delivery.send
import at.quickme.kotlinmailer.email.emailBuilder
import at.quickme.kotlinmailer.smtp.smtpMailer
import io.github.iandbrown.trials.database.Setting

internal actual class EmailSender actual constructor(private val setting: Setting) {
    actual suspend fun send(from: String, to: String, copy: String, subject: String, body: String) {
        val mailer = smtpMailer(host = setting.smtpHost) {
            credentials(setting.userName, setting.password)
        }
        val email = if (copy.isNotBlank()) {
            emailBuilder {
                from(from)
                to(to)
                cc(copy)
                this.subject = subject
                this.text = body
            }
        } else {
            emailBuilder {
                from(from)
                to(to)
                this.subject = subject
                this.text = body
            }
        }
        println("$to $from $subject $body")
        email.send(mailer)
    }
}
