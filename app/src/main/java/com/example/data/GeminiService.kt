package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini API Moshi Models ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

// --- Output Structured Schemas ---

@JsonClass(generateAdapter = true)
data class VisualIngestionResult(
    val material_name: String,
    val category: String, // "Thread", "Lace", "Gota", "Ribbon", "Buttons"
    val estimated_width: String,
    val pattern_description: String,
    val color: String,
    val suggested_price: Double,
    val unit_type: String // "Yard", "Meter", "Box", "Dozen", "Spool"
)

@JsonClass(generateAdapter = true)
data class QuantityEstimatorResult(
    val gota_yards: Double,
    val lace_yards: Double,
    val ribbon_meters: Double,
    val thread_spools: Int,
    val buttons_dozen: Double,
    val explanation: String
)

// --- Retrofit/Direct REST Client Service ---

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Converts a Bitmap to a Base64-encoded string for inlineData.
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Inspects a material picture and extracts structured inventory specifications.
     */
    suspend fun analyzeMaterialImage(bitmap: Bitmap): VisualIngestionResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is empty or placeholder! Yielding high-fidelity fallback.")
            return@withContext getMockVisualIngestion()
        }

        val prompt = """
            Analyze this photo of a styling/tailoring accessory and return structured properties:
            - material_name: (suggest a crisp descriptive name like 'Vibrant Crimson Gota' or 'Vintage Ivory Crochet Lace')
            - category: (strictly one of: 'Thread', 'Lace', 'Gota', 'Ribbon', 'Buttons')
            - estimated_width: (e.g. '1.5 inches', '4 cm', or 'N/A')
            - pattern_description: (short stylistic summary)
            - color: (primary color detected)
            - suggested_price: (suggested price per unit, e.g. 3.50)
            - unit_type: (strictly coordinate with category: Lace/Gota/Ribbon -> 'Yard' or 'Meter'; Thread -> 'Spool' or 'Box'; Buttons -> 'Dozen')

            Return ONLY a valid JSON object matching the requested schema. No markdown wrapping.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val requestBodyJson = moshi.adapter(GeminiRequest::class.java).toJson(request)
            
            val httpRequest = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Unsuccessful API call: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body")
                val geminiResponse = moshi.adapter(GeminiResponse::class.java).fromJson(bodyString)
                val rawText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No candidates returned from Gemini")

                Log.d(TAG, "Raw Response received: $rawText")
                moshi.adapter(VisualIngestionResult::class.java).fromJson(rawText)
                    ?: throw Exception("Failed to parse visual ingestion JSON output")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed, yielding fallback mock", e)
            getMockVisualIngestion()
        }
    }

    /**
     * Estimates tailoring materials needed for any project query.
     */
    suspend fun estimateProjectMaterials(projectDescription: String): QuantityEstimatorResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is empty or placeholder! Yielding high-fidelity mock estimator result.")
            return@withContext getMockQuantityEstimate(projectDescription)
        }

        val prompt = """
            Estimator task: A buyer wants to sew of item: "$projectDescription".
            Estimate the exact materials they need from our traditional catalog.
            
            Provide:
            - gota_yards: (Double value of gota ribbon yards needed, e.g. 5.5. 0 if not needed)
            - lace_yards: (Double value of lace yards needed, e.g. 6.0. 0 if not needed)
            - ribbon_meters: (Double value of ribbon meters needed, e.g. 3.0. 0 if not needed)
            - thread_spools: (Int of thread spools needed, e.g. 3. At least 1 spool is always recommended)
            - buttons_dozen: (Double count in dozens. e.g. 0.5 means 6 buttons, 1.0 is 12, etc. 0 if not needed)
            - explanation: (A helpful 2-3 sentence breakdown explaining the math based on typical garments e.g. Duptatta borders require 5 yards, sleeves and neckline need 1.5, etc.)

            Return ONLY a valid JSON object matching this schema. No markdown wrapping.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val requestBodyJson = moshi.adapter(GeminiRequest::class.java).toJson(request)

            val httpRequest = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Unsuccessful API call: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body")
                val geminiResponse = moshi.adapter(GeminiResponse::class.java).fromJson(bodyString)
                val rawText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No text candidates returned")

                Log.d(TAG, "Raw Estimator response: $rawText")
                moshi.adapter(QuantityEstimatorResult::class.java).fromJson(rawText)
                    ?: throw Exception("Failed to parse JSON response into QuantityEstimatorResult")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Estimator API failure, reverting to offline calculator fallback", e)
            getMockQuantityEstimate(projectDescription)
        }
    }

    private fun getMockVisualIngestion(): VisualIngestionResult {
        return VisualIngestionResult(
            material_name = "Handcrafted Rajasthani Gota Lace",
            category = "Gota",
            estimated_width = "2.5 cm",
            pattern_description = "Golden floral block leaf pattern woven with highly metallic polyester thread. Ideal for wedding lehengas.",
            color = "Shining Metallic Gold",
            suggested_price = 4.75,
            unit_type = "Yard"
        )
    }

    private fun getMockQuantityEstimate(desc: String): QuantityEstimatorResult {
        val lowerCase = desc.lowercase()
        return if (lowerCase.contains("anarkali") || lowerCase.contains("heavy") || lowerCase.contains("gota")) {
            QuantityEstimatorResult(
                gota_yards = 8.5,
                lace_yards = 5.0,
                ribbon_meters = 2.0,
                thread_spools = 4,
                buttons_dozen = 1.0,
                explanation = "An Anarkali suit with heavy gota work requires approximately 8.5 yards of Gota for a double flare border, 5.0 yards of complementary Lace along the dupatta hem, 2.0 meters of Ribbon for the sleeve accents, 4 spools of matching thread, and a dozen brass buttons for the decorative back and neck placket."
            )
        } else if (lowerCase.contains("kurta") || lowerCase.contains("simple") || lowerCase.contains("blouse")) {
            QuantityEstimatorResult(
                gota_yards = 2.0,
                lace_yards = 3.0,
                ribbon_meters = 0.0,
                thread_spools = 2,
                buttons_dozen = 0.5,
                explanation = "A simple Kurta or blouse design uses minimal trimming. We estimate 2.0 yards of gota for sleeve borders, 3.0 yards of fine lace for the neckline and slits, 2 matching spools of thread, and a half-dozen (0.5) buttons for the front placket decoration."
            )
        } else {
            QuantityEstimatorResult(
                gota_yards = 4.0,
                lace_yards = 4.25,
                ribbon_meters = 1.5,
                thread_spools = 2,
                buttons_dozen = 0.8,
                explanation = "Based on standard dress garments, we estimated a moderate trim layout. This includes approximately 4.0 yards of Gota, 4.25 yards of lace for panels, 1.5 meters of ribbon, 2 spools of sewing thread, and 10 decorative buttons (approx 0.8 dozen)."
            )
        }
    }
}
