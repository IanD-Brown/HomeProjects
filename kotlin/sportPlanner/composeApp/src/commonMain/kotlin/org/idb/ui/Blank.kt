package org.idb.ui

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Blank: ImageVector
    get() {
        if (_blank != null) {
            return _blank!!
        }
        _blank = materialIcon(name = "Filled.Blank") {
            materialPath {
            }
        }
        return _blank!!
    }

private var _blank: ImageVector? = null
