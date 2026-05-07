package com.proinnovation.isitonline.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.proinnovation.isitonline.databinding.DialogAddSiteBinding

class AddSiteDialog : DialogFragment() {

    interface Listener {
        fun onSiteAdded(url: String, label: String)
    }

    private var listener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddSiteBinding.inflate(layoutInflater)

        return AlertDialog.Builder(requireContext())
            .setTitle("Add Site")
            .setView(binding.root)
            .setPositiveButton("Add") { _, _ ->
                var url = binding.etUrl.text.toString().trim()
                val label = binding.etLabel.text.toString().trim()

                if (url.isNotEmpty()) {
                    // Prepend https:// if no scheme
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    listener?.onSiteAdded(url, label)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
