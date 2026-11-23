package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainAppShell
import com.example.ui.theme.FamSpendTheme
import com.example.util.InvitationTokenUtil
import com.example.viewmodel.FamilyExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FamilyExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            FamSpendTheme(darkTheme = uiState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppShell(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val dataUri = intent?.data ?: return
        val uriString = dataUri.toString()
        val tokenParam = dataUri.getQueryParameter("token")
            ?: InvitationTokenUtil.extractTokenFromInput(uriString)
        if (!tokenParam.isNullOrBlank()) {
            viewModel.handleIncomingInvitationLink(tokenParam)
        }
    }
}
