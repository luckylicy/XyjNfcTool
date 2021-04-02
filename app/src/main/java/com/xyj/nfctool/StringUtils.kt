package com.xyj.nfctool

import java.lang.StringBuilder

/**
 * StringUtils.kt
 * description: TODO
 *
 * @author : Licy
 * @date : 2021/3/20
 * email ：licy3051@qq.com
 */
object StringUtils {

    fun isEmpty(value: String): Boolean {
        return value.isNotEmpty()
    }

    fun convertHexToString(hex: String): String {
        val stringBuilder = StringBuilder()
        val temp = StringBuilder()

        for (index in hex.indices step 2) {
            var substring = hex.substring(index, index + 2)
            var toInt = substring.toInt(16)
            stringBuilder.append(toInt.toChar())
            temp.append(toInt)
        }
        return stringBuilder.toString()
    }
}