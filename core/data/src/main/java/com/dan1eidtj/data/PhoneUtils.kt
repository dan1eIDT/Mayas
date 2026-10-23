package com.dan1eidtj.data

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil


object PhoneUtils {

    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }


    fun normalize(raw: String, defaultRegion: String = "RU"): String? {
        if (raw.isBlank()) return null
        return try {
            val parsed = util.parse(raw, defaultRegion)
            if (!util.isValidNumber(parsed)) return null
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: NumberParseException) {
            null
        }
    }


    fun formatForDisplay(raw: String, defaultRegion: String = "RU"): String {
        return try {
            val parsed = util.parse(raw, defaultRegion)
            if (!util.isValidNumber(parsed)) return raw
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (e: NumberParseException) {
            raw
        }
    }
}
