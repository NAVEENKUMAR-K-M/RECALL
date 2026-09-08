package com.aigallery.app.ai.embedding

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.sqrt

interface EmbeddingModel {
    fun generateEmbedding(text: String, tokens: List<String> = emptyList()): FloatArray
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float
    fun serialize(vector: FloatArray): ByteArray
    fun deserialize(bytes: ByteArray): FloatArray
    fun getDimension(): Int
    fun getModelVersion(): String
}

class LocalSemanticEmbeddingModel : EmbeddingModel {

    companion object {
        const val DIMENSION = 64
        const val VERSION = "local_dense_v1"
    }

    override fun getDimension(): Int = DIMENSION
    override fun getModelVersion(): String = VERSION

    override fun generateEmbedding(text: String, tokens: List<String>): FloatArray {
        val vector = FloatArray(DIMENSION) { 0f }
        val combinedWords = mutableListOf<String>()

        // Split text into normalized tokens
        text.lowercase(Locale.ROOT)
            .split(Regex("[\\s,;:.\\-_/()\\[\\]\"'!?#@₹$]+"))
            .filter { it.length >= 2 }
            .forEach { combinedWords.add(it) }

        tokens.forEach { token ->
            token.lowercase(Locale.ROOT)
                .split(Regex("[\\s,;:.\\-_/()\\[\\]\"'!?#@₹$]+"))
                .filter { it.length >= 2 }
                .forEach { combinedWords.add(it) }
        }

        if (combinedWords.isEmpty()) {
            return vector
        }

        // Semantic hash-projection over 64 dimensions with term frequency weighting
        for (word in combinedWords) {
            val hash1 = (word.hashCode() and 0x7FFFFFFF) % DIMENSION
            val hash2 = ((word.hashCode() * 31 + 17) and 0x7FFFFFFF) % DIMENSION
            val sign = if (word.length % 2 == 0) 1.0f else -1.0f

            // Domain weight boosts for high-information keywords
            val weight = when {
                word.length > 5 -> 1.5f
                word.startsWith("http") -> 0.5f
                word in listOf("hotel", "booking", "flight", "internship", "order", "payment", "upi", "ticket") -> 2.0f
                else -> 1.0f
            }

            vector[hash1] += weight * sign
            vector[hash2] += (weight * 0.5f) * -sign
        }

        // Normalize to unit vector (L2 norm)
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        if (norm > 1e-6f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector
    }

    override fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f
        var dot = 0f
        var n1 = 0f
        var n2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            n1 += v1[i] * v1[i]
            n2 += v2[i] * v2[i]
        }
        val denom = sqrt(n1) * sqrt(n2)
        return if (denom > 1e-6f) (dot / denom).coerceIn(-1.0f, 1.0f) else 0f
    }

    override fun serialize(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (value in vector) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

    override fun deserialize(bytes: ByteArray): FloatArray {
        val count = bytes.size / 4
        val result = FloatArray(count)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            result[i] = buffer.float
        }
        return result
    }
}
