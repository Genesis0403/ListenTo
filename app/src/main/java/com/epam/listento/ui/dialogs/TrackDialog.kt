package com.epam.listento.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.ui.MainViewModel
import javax.inject.Inject

class TrackDialog : DialogFragment() {

    @Inject
    lateinit var factory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by activityViewModels {
        factory
    }

    private val args: TrackDialogArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        App.component.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setItems(R.array.tracks_array) { _, which ->
            when (which) {
                ADD_TO_CACHE -> {
                    mainViewModel.cacheTrack(args.id, args.title, args.artist)
                }
                REMOVE_FROM_CACHE -> {
                    mainViewModel.uncacheTrack(args.id, args.title, args.artist)
                }
            }
        }
        return builder.create()
    }

    private companion object {
        private const val ADD_TO_CACHE = 0
        private const val REMOVE_FROM_CACHE = 1
    }
}