package com.pranay.jobtracker.domain

import com.google.api.client.util.Base64
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart

class EmailPreprocessor {

    fun preprocess(message: Message): RawEmailData {
        val payload = message.payload
        val headers = payload?.headers ?: emptyList()

        val subject = headers.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: ""
        val from    = headers.find { it.name.equals("From",    ignoreCase = true) }?.value ?: ""
        val date    = headers.find { it.name.equals("Date",    ignoreCase = true) }?.value ?: ""

        val rawBody = extractPlainText(payload)
        // Prefer plain-text body capped at 500 chars; fall back to Gmail snippet (~100 chars)
        // to avoid sending large HTML blobs to Gemini.
        val body = if (rawBody.isNotBlank()) rawBody.take(500) else message.snippet ?: ""

        return RawEmailData(emailId = message.id, subject = subject, body = body, date = date, from = from)
    }

    private fun extractPlainText(payload: MessagePart?): String {
        if (payload == null) return ""
        if (payload.mimeType == "text/plain") {
            val data = payload.body?.data ?: return ""
            return runCatching { String(Base64.decodeBase64(data)) }.getOrDefault("")
        }
        payload.parts?.forEach { part ->
            val result = extractPlainText(part)
            if (result.isNotBlank()) return result
        }
        return ""
    }
}
