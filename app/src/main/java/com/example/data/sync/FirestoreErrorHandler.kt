package com.example.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

object FirestoreErrorHandler {

    fun handleFirestoreError(
        exception: Throwable,
        operationType: OperationType,
        path: String
    ): String {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            null
        }

        val payload = JSONObject().apply {
            put("error", exception.message ?: exception.javaClass.simpleName)
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", JSONObject().apply {
                put("userId", currentUser?.uid ?: JSONObject.NULL)
                put("email", currentUser?.email ?: JSONObject.NULL)
                put("emailVerified", currentUser?.isEmailVerified ?: false)
                put("isAnonymous", currentUser?.isAnonymous ?: false)
            })
        }.toString()

        try {
            Log.e("FirestoreError", payload, exception)
        } catch (_: Throwable) {
            // Safe no-op in pure JVM tests where android.util.Log may not be mocked
        }
        return payload
    }
}
