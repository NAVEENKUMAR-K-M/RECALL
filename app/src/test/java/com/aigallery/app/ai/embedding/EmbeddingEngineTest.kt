package com.aigallery.app.ai.embedding

import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingEngineTest {

    private val model = LocalSemanticEmbeddingModel()

    @Test
    fun testVectorDimensionAndNormalization() {
        val vector = model.generateEmbedding("Booking confirmed at Taj Exotica Goa for 18 Aug")
        assertEquals(LocalSemanticEmbeddingModel.DIMENSION, vector.size)

        var sumSq = 0f
        for (v in vector) {
            sumSq += v * v
        }
        val norm = sqrt(sumSq)
        assertTrue("Norm should be approximately 1.0, was $norm", abs(norm - 1.0f) < 0.01f)
    }

    @Test
    fun testCosineSimilarityIdentical() {
        val text = "Sony WH-1000XM5 headphones purchase Amazon"
        val v1 = model.generateEmbedding(text)
        val v2 = model.generateEmbedding(text)

        val sim = model.cosineSimilarity(v1, v2)
        assertTrue("Identical texts should have similarity ~1.0, was $sim", abs(sim - 1.0f) < 0.001f)
    }

    @Test
    fun testCosineSimilarityRelatedVsUnrelated() {
        val travel1 = model.generateEmbedding("Hotel reservation at Taj Goa", listOf("hotel", "goa", "booking"))
        val travel2 = model.generateEmbedding("Flight ticket to Goa booking", listOf("flight", "goa", "travel"))
        val unrelated = model.generateEmbedding("Calculus math homework derivatives", listOf("math", "calculus"))

        val simTravel = model.cosineSimilarity(travel1, travel2)
        val simUnrelated = model.cosineSimilarity(travel1, unrelated)

        assertTrue("Related travel items should have higher similarity than math ($simTravel vs $simUnrelated)", simTravel > simUnrelated)
    }

    @Test
    fun testSerializationAndDeserialization() {
        val original = model.generateEmbedding("Google Software Engineering Internship Offer")
        val bytes = model.serialize(original)
        val restored = model.deserialize(bytes)

        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            assertEquals(original[i], restored[i], 0.0001f)
        }
    }
}
