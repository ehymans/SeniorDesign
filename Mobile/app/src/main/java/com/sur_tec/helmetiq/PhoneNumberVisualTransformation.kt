// File: PhoneNumberVisualTransformation.kt
package com.sur_tec.helmetiq

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(10) // Limit to 10 digits

        val formatted = StringBuilder("+1 ")
        if (digits.isNotEmpty()) {
            formatted.append("(")
            formatted.append(digits.substring(0, minOf(3, digits.length)))
            if (digits.length >= 3) {
                formatted.append(") ")
            }
        }
        if (digits.length > 3) {
            formatted.append(digits.substring(3, minOf(6, digits.length)))
        }
        if (digits.length >= 6) {
            formatted.append("-")
            formatted.append(digits.substring(6, minOf(10, digits.length)))
        }

        // Cursor should always be at the end
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return digits.length
            }
        }

        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}
