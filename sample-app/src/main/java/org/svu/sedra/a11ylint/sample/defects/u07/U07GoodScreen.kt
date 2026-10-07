package org.svu.sedra.a11ylint.sample.defects.u07

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import org.svu.sedra.a11ylint.sample.R

/**
 * U-07 good examples: every piece of accessibility text comes from `strings.xml`, so it is
 * translated with the rest of the app. U-07 must report nothing here.
 */
@Composable
fun U07GoodScreen(onOpen: () -> Unit, onShare: () -> Unit) {
    val icon = painterResource(R.drawable.ic_launcher_foreground)
    val volume = stringResource(R.string.u07_volume)
    val muted = stringResource(R.string.u07_muted)
    val settings = stringResource(R.string.u07_settings)
    val emailError = stringResource(R.string.u07_email_error)
    val share = stringResource(R.string.u07_share)

    Column {
        Icon(icon, contentDescription = stringResource(R.string.u07_delete_draft))

        Row(
            modifier = Modifier.clickable(
                onClickLabel = stringResource(R.string.u07_open_details),
                role = Role.Button,
            ) { onOpen() },
        ) {
            Text(stringResource(R.string.u07_shipping_forecast))
        }

        Row(
            modifier = Modifier.semantics {
                contentDescription = volume
                stateDescription = muted
                paneTitle = settings
                error(emailError)
                customActions = listOf(
                    CustomAccessibilityAction(share) { onShare(); true },
                )
            },
        ) {
            Text(volume)
        }
    }
}
