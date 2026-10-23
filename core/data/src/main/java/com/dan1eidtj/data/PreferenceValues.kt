package com.dan1eidtj.data

fun readBooleanPreference(value: Any?, defaultValue: Boolean): Boolean = when (value) {
    is Boolean -> value
    is String -> when (value.trim().lowercase()) {
        "true" -> true
        "false" -> false
        else -> defaultValue
    }
    else -> defaultValue
}
