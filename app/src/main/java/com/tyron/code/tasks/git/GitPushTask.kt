package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.text.InputType
import android.widget.Toast
import org.codeassist.rihad.R
import org.codeassist.rihad.databinding.LayoutDialogProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import com.tyron.code.tasks.git.GitProgressMonitor
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

object GitPushTask {

    fun push(project: Project, context: Context) {
        val prefs = context.getSharedPreferences("git_credentials", Context.MODE_PRIVATE)
        val savedUsername = prefs.getString("username", "") ?: ""
        val savedToken = prefs.getString("token", "") ?: ""

        if (savedUsername.isEmpty() || savedToken.isEmpty()) {
            showCredentialDialog(project, context, prefs)
        } else {
            startPush(project, context, savedUsername, savedToken)
        }
    }

    private fun showCredentialDialog(project: Project, context: Context, prefs: SharedPreferences) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding, padding, padding)

        val etUsername = EditText(context)
        etUsername.hint = "GitHub Username"
        etUsername.inputType = InputType.TYPE_CLASS_TEXT
        layout.addView(etUsername)

        val etToken = EditText(context)
        etToken.hint = "GitHub Token (PAT)"
        etToken.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etToken)

        MaterialAlertDialogBuilder(context)
            .setTitle("GitHub Credentials")
            .setView(layout)
            .setPositiveButton("Push") { _, _ ->
                val username = etUsername.text.toString().trim()
                val token = etToken.text.toString().trim()

                if (username.isNotEmpty() && token.isNotEmpty()) {
                    prefs.edit()
                        .putString("username", username)
                        .putString("token", token)
                        .apply()
                    startPush(project, context, username, token)
                } else {
                    Toast.makeText(context, "Username and token required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startPush(project: Project, context: Context, username: String, token: String) {
        val inflater = LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val binding = LayoutDialogProgressBinding.inflate(inflater, null, false)
        binding.message.visibility = View.VISIBLE

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.pushing)
        builder.setView(binding.root)
        builder.setCancelable(false)

        val progress = GitProgressMonitor(binding.progress, binding.message)
        val credentialsProvider = UsernamePasswordCredentialsProvider(username, token)

        val future = executeAsyncProvideError({
            Git.open(project.getRootFile())
                .push()
                .setProgressMonitor(progress)
                .setCredentialsProvider(credentialsProvider) // SSH callback sorano hoyeche
                .call()

            return@executeAsyncProvideError
        }, { _, _ -> })

        val dialog = builder.show()

        future.whenComplete { result, error ->
            ThreadUtils.runOnUiThread {
                dialog?.dismiss()

                if (result == null || error != null) {
                    ErrorOutput.ShowError(error, context)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.push_completed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
