package com.example.agendei_pro.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > 11) text.text.substring(0, 11) else text.text
        val out = StringBuilder()
        
        // Mapeamento dinâmico de offsets para evitar IndexOutOfBoundsException
        val originalToTransformedMap = IntArray(trimmed.length + 1)
        originalToTransformedMap[0] = 0
        
        for (i in trimmed.indices) {
            if (i == 0) {
                out.append("(")
                originalToTransformedMap[0] = 1
            }
            out.append(trimmed[i])
            originalToTransformedMap[i + 1] = out.length
            
            if (i == 1) {
                out.append(") ")
            } else if (i == 6) {
                out.append("-")
            }
        }
        
        // Assegurar que o cursor possa ir até o final da string atual
        originalToTransformedMap[trimmed.length] = out.length

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, trimmed.length)
                return originalToTransformedMap[safeOffset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, out.length)
                for (i in originalToTransformedMap.indices) {
                    if (originalToTransformedMap[i] >= safeOffset) {
                        return i
                      }
                  }
                  return trimmed.length
            }
        }

        return TransformedText(AnnotatedString(out.toString()), numberOffsetTranslator)
    }
}
