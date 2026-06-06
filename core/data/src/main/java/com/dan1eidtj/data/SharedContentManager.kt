package com.dan1eidtj.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SharedContentManager {
    var sharedText by mutableStateOf<String?>(null)
}