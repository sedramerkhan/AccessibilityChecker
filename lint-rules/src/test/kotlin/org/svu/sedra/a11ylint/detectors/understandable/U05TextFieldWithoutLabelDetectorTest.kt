package org.svu.sedra.a11ylint.detectors.understandable

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.svu.sedra.a11ylint.testing.A11yLintTest

class U05TextFieldWithoutLabelDetectorTest : A11yLintTest() {
    override fun getDetector(): Detector = U05TextFieldWithoutLabelDetector()

    override fun getIssues(): List<Issue> = listOf(U05TextFieldWithoutLabelDetector.ISSUE)

    /** A screen file with the imports every test needs. [body] starts at line 20. */
    private fun screen(body: String): TestFile = kotlin(
        "src/test/pkg/Screen.kt",
        """
package test.pkg

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics


""" + body.trimIndent(),
    )

    private fun message(name: String, placeholder: Boolean) =
        "[U-05] $name has no label" +
            (if (placeholder) " (the placeholder disappears when the user types)" else "") +
            ", so screen reader users are not told what to enter [ComposeTextFieldWithoutLabel]"

    // Positive tests: must be flagged.

    fun testTextFieldWithoutAnyLabel() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:23: Error: ${message("TextField", placeholder = false)}
                TextField(value = name, onValueChange = { name = it })
                ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun NameField() {
                    var name by remember { mutableStateOf("") }
                    TextField(value = name, onValueChange = { name = it })
                }
                """,
            ),
        )
    }

    fun testPlaceholderAloneIsNotALabel() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Error: ${message("OutlinedTextField", placeholder = true)}
                OutlinedTextField(
                ~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun EmailField(email: String, onEmailChange: (String) -> Unit) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = { Text("you@example.com") },
                    )
                }
                """,
            ),
        )
    }

    fun testNullLabelIsMissing() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Error: ${message("TextField", placeholder = false)}
                TextField(query, onQueryChange, label = null)
                ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Search(query: String, onQueryChange: (String) -> Unit) {
                    TextField(query, onQueryChange, label = null)
                }
                """,
            ),
        )
    }

    fun testSeveralFieldsInAForm() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:24: Error: ${message("TextField", placeholder = true)}
                    TextField(city, onCity, placeholder = { Text("City") })
                    ~~~~~~~~~
            src/test/pkg/Screen.kt:25: Error: ${message("OutlinedTextField", placeholder = false)}
                    OutlinedTextField(zip, onZip, isError = zip.isEmpty())
                    ~~~~~~~~~~~~~~~~~
            2 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun Address(street: String, city: String, zip: String, onStreet: (String) -> Unit, onCity: (String) -> Unit, onZip: (String) -> Unit) {
                    Column {
                        TextField(street, onStreet, label = { Text("Street") })
                        TextField(city, onCity, placeholder = { Text("City") })
                        OutlinedTextField(zip, onZip, isError = zip.isEmpty())
                    }
                }
                """,
            ),
        )
    }

    private fun nameOnlyMessage(name: String, placeholder: Boolean) =
        "[U-05] $name has a contentDescription but no visible label" +
            (if (placeholder) " (the placeholder disappears when the user types)" else "") +
            ", so users who do not use a screen reader are not told what to enter [ComposeTextFieldWithoutLabel]"

    // A name in semantics helps screen readers but is not a visible label: a warning.
    fun testContentDescriptionWithoutVisibleLabel() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Warning: ${nameOnlyMessage("TextField", placeholder = false)}
                TextField(
                ~~~~~~~~~
            0 errors, 1 warnings
            """,
            screen(
                """
                @Composable
                fun NameField(name: String, onNameChange: (String) -> Unit) {
                    TextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier.semantics { contentDescription = "Name" },
                    )
                }
                """,
            ),
        )
    }

    fun testContentDescriptionWithPlaceholder() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Warning: ${nameOnlyMessage("OutlinedTextField", placeholder = true)}
                OutlinedTextField(
                ~~~~~~~~~~~~~~~~~
            0 errors, 1 warnings
            """,
            screen(
                """
                @Composable
                fun SearchField(query: String, onQueryChange: (String) -> Unit) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.semantics { contentDescription = "Search" },
                        placeholder = { Text("Search") },
                    )
                }
                """,
            ),
        )
    }

    // Semantics that set something other than a name do not help: still an error.
    fun testSemanticsWithoutANameIsAnError() {
        expectWarnings(
            """
            src/test/pkg/Screen.kt:22: Error: ${message("TextField", placeholder = false)}
                TextField(
                ~~~~~~~~~
            1 errors, 0 warnings
            """,
            screen(
                """
                @Composable
                fun NameField(name: String, onNameChange: (String) -> Unit) {
                    TextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier.semantics { role = Role.Button },
                    )
                }
                """,
            ),
        )
    }

    // Negative tests: must not be flagged.

    fun testFieldWithLabel() {
        expectClean(
            screen(
                """
                @Composable
                fun NameField(name: String, onNameChange: (String) -> Unit) {
                    TextField(value = name, onValueChange = onNameChange, label = { Text("Name") })
                }
                """,
            ),
        )
    }

    fun testLabelWithPlaceholder() {
        expectClean(
            screen(
                """
                @Composable
                fun EmailField(email: String, onEmailChange: (String) -> Unit) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        placeholder = { Text("you@example.com") },
                    )
                }
                """,
            ),
        )
    }

    fun testLabelPassedAsVariable() {
        expectClean(
            screen(
                """
                @Composable
                fun Field(value: String, onChange: (String) -> Unit, label: @Composable () -> Unit) {
                    TextField(value, onChange, label = label)
                }
                """,
            ),
        )
    }

    fun testOtherFunctionsNamedTextField() {
        expectClean(
            screen(
                """
                @Composable
                fun TextField(hint: String) {
                    Text(hint)
                }

                @Composable
                fun Screen() {
                    TextField("Search")
                }
                """,
            ),
        )
    }
}
