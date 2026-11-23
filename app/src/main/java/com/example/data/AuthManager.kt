package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = true
)

sealed class GoogleSignInResult {
    data class Success(val user: AuthUser) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}

class AuthManager(context: Context? = null) {

    private val auth: FirebaseAuth? = try {
        if (context == null || FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseAuth.getInstance()
        } else {
            null
        }
    } catch (e: Throwable) {
        Log.w("AuthManager", "FirebaseAuth unavailable in current environment: ${e.message}")
        null
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authUser = MutableStateFlow<AuthUser?>(auth?.currentUser?.toAuthUser())
    val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _authUser.value = user?.toAuthUser()
    }

    init {
        auth?.addAuthStateListener(authStateListener)
    }

    fun cleanup() {
        auth?.removeAuthStateListener(authStateListener)
    }

    /**
     * Explicit interactive Google Sign-In via Jetpack Credential Manager.
     * Uses a single GetSignInWithGoogleOption per GetCredentialRequest and properly
     * unwraps CustomCredential -> GoogleIdTokenCredential.
     */
    suspend fun signInWithGoogle(context: Context): GoogleSignInResult {
        val firebaseAuth = auth
            ?: return GoogleSignInResult.Error("Authentication service is not initialized.")

        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            if (webClientId.isBlank()) {
                return GoogleSignInResult.Error("Google Sign-In is not configured for this build.")
            }

            val credentialManager = CredentialManager.create(context)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(context, request)
            handleCredentialResponse(firebaseAuth, result)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Credential Manager error during Google Sign-In", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Unable to sign in with Google. Please try again.")
        } catch (e: Exception) {
            Log.e("AuthManager", "Unexpected error during Google Sign-In", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Sign-in failed. Please check your connection.")
        }
    }

    /**
     * Silent auto sign-in for returning authorized accounts on launch.
     */
    suspend fun attemptSilentSignIn(context: Context): GoogleSignInResult {
        val firebaseAuth = auth ?: return GoogleSignInResult.Cancelled
        if (firebaseAuth.currentUser != null) {
            return GoogleSignInResult.Success(firebaseAuth.currentUser!!.toAuthUser())
        }

        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            if (webClientId.isBlank()) return GoogleSignInResult.Cancelled

            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            handleCredentialResponse(firebaseAuth, result)
        } catch (_: Throwable) {
            GoogleSignInResult.Cancelled
        }
    }

    private suspend fun handleCredentialResponse(
        firebaseAuth: FirebaseAuth,
        response: GetCredentialResponse
    ): GoogleSignInResult {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            return if (user != null) {
                val mapped = user.toAuthUser()
                _currentUser.value = user
                _authUser.value = mapped
                GoogleSignInResult.Success(mapped)
            } else {
                GoogleSignInResult.Error("Sign-in succeeded, but no user profile was returned.")
            }
        }
        return GoogleSignInResult.Error("Unsupported credential type returned by Credential Manager.")
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                auth?.signOut()
                _currentUser.value = null
                _authUser.value = null
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Throwable) {
                Log.w("AuthManager", "Error clearing credential state on sign-out: ${e.message}")
            }
        }
    }

    companion object {
        fun FirebaseUser.toAuthUser(): AuthUser {
            return AuthUser(
                uid = uid,
                displayName = displayName?.takeIf { it.isNotBlank() }
                    ?: email?.substringBefore("@")
                    ?: "Family Member",
                email = email.orEmpty(),
                photoUrl = photoUrl?.toString(),
                isEmailVerified = isEmailVerified
            )
        }
    }
}
