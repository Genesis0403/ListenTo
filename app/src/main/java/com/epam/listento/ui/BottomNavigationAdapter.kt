package com.epam.listento.ui

import androidx.fragment.app.Fragment
import com.epam.listento.utils.FragmentsAdapter

private const val NO_SUCH_FRAGMENT_ERROR = "No such fragment in adapter."

class BottomNavigationAdapter(
    private val fragments: Map<Int, Fragment>
) : FragmentsAdapter {

    override fun get(id: Int): Fragment {
        if (!fragments.containsKey(id) && fragments[id] == null) {
            throw IllegalStateException("$NO_SUCH_FRAGMENT_ERROR: $id")
        }
        return fragments[id] as Fragment
    }
}
