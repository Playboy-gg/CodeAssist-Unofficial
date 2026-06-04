package com.tyron.code.tasks.git

import com.tyron.builder.project.Project
import org.eclipse.jgit.api.Git
import com.tyron.code.tasks.git.ErrorOutput
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import org.codeassist.rihad.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ThreadUtils
import com.tyron.code.util.executeAsyncProvideError
import org.eclipse.jgit.lib.StoredConfig

object GitRemoteTask {

    fun remote(project: Project, context: Context) {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding, padding, padding)

        val etName = EditText(context)
        etName.hint = "Remote name (e.g. origin)"
        etName.setText("origin")
        layout.addView(etName)

        val etUrl = EditText(context)
        etUrl.hint = "Remote URL (https://github.com/user/repo.git)"
        layout.addView(etUrl)

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.title_add_remove_remote)
        builder.setView(layout)

        builder.setPositiveButton(R.string.add) { _, _ ->
            val future = executeAsyncProvideError({
                val remoteName = etName.text.toString().trim()
                val remoteUrl = etUrl.text.toString().trim()

                if (remoteName.isBlank() || remoteUrl.isBlank()) {
                    ThreadUtils.runOnUiThread {
                        Toast.makeText(context, context.getString(R.string.empty_remote), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val config: StoredConfig = Git.open(project.getRootFile()).repository.config
                    config.setString("remote", remoteName, "url", remoteUrl)
                    config.setString("remote", remoteName, "fetch", "+refs/heads/*:refs/remotes/$remoteName/*")
                    config.save()
                }
                return@executeAsyncProvideError
            }, { _, _ -> })

            future.whenComplete { result, error ->
                ThreadUtils.runOnUiThread {
                    if (result == null || error != null) {
                        ErrorOutput.ShowError(error, context)
                    } else {
                        Toast.makeText(context, context.getString(R.string.remote_added_successfully), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        builder.setNegativeButton(android.R.string.cancel, null)

        builder.setNeutralButton(R.string.remove) { _, _ ->
            val future = executeAsyncProvideError({
                val remoteName = etName.text.toString().trim()

                if (remoteName.isBlank()) {
                    ThreadUtils.runOnUiThread {
                        Toast.makeText(context, context.getString(R.string.empty_remote), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val config: StoredConfig = Git.open(project.getRootFile()).repository.config
                    config.unsetSection("remote", remoteName)
                    config.save()
                }
                return@executeAsyncProvideError
            }, { _, _ -> })

            future.whenComplete { result, error ->
                ThreadUtils.runOnUiThread {
                    if (result == null || error != null) {
                        ErrorOutput.ShowError(error, context)
                    } else {
                        Toast.makeText(context, context.getString(R.string.remote_removed_successfully), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        builder.show()
    }
}
