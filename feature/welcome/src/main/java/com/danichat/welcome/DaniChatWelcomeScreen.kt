package com.danichat.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Presentation only. The host owns navigation, persistence, and all real transports. */
@Composable
fun DaniChatWelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLicense by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.danichat_mark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(72.dp)
        )
        Text(stringResource(R.string.dani_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.dani_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.dani_intro), style = MaterialTheme.typography.bodyLarge)
        WelcomeCard(R.string.dani_nearby_title, R.string.dani_nearby_body)
        WelcomeCard(R.string.dani_identity_title, R.string.dani_identity_body)
        WelcomeCard(R.string.dani_preview_title, R.string.dani_preview_body)
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(stringResource(R.string.dani_continue))
        }
        TextButton(onClick = { showLicense = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dani_license_action))
        }
        Text(
            stringResource(R.string.dani_attribution),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showLicense) {
        val resources = LocalContext.current.resources
        val license = remember(resources) {
            resources.openRawResource(R.raw.danichat_license).bufferedReader().use { it.readText() }
        }
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text(stringResource(R.string.dani_license_action)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.dani_attribution))
                    Spacer(Modifier.height(16.dp))
                    Text(license, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) {
                    Text(stringResource(R.string.dani_close))
                }
            }
        )
    }
}

@Composable
private fun WelcomeCard(title: Int, body: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
