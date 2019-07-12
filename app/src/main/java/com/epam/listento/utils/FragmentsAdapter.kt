package com.epam.listento.utils

import androidx.fragment.app.Fragment

interface FragmentsAdapter {
    operator fun get(id: Int): Fragment
}