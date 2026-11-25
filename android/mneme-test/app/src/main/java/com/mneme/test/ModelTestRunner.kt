package com.mneme.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.whispercpp.java.whisper.WhisperContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import kotlin.system.measureTimeMillis

class ModelTestRunner(private val context: Context) {

    private val resultFile = File("/sdcard/Mneme/test-results.txt")

    suspend fun runAll(onLine: suspend (String) -> Unit) = withContext(Dispatchers.Default) {
        resultFile.parentFile?.mkdirs()
        resultFile.writeText("")
        log(onLine, "=== Mneme model test run ===")
        log(onLine, ModelPaths.describe())

        // Safer tests first (no native ggml clash risk)
        runFilesCheck(onLine)
        runOcrTest(onLine)
        runEmbedTest(onLine)
        runFallbackNote(onLine)
        runLlmTest(onLine)
        runWhisperTest(onLine)

        log(onLine, "=== Done ===")
    }

    private suspend fun log(onLine: suspend (String) -> Unit, line: String) {
        Log.i(TAG, line)
        try {
            resultFile.appendText(line + "\n")
        } catch (_: Exception) {
        }
        onLine(line)
    }

    private suspend fun runFilesCheck(onLine: suspend (String) -> Unit) {
        log(onLine, "\n[Files]")
        listOf(
            "LLM" to ModelPaths.llmGguf,
            "Whisper" to ModelPaths.whisperBin,
            "Embed TFLite" to ModelPaths.embedTflite,
            "JFK wav" to ModelPaths.jfkWav,
        ).forEach { (label, file) ->
            val ok = file.isFile && file.canRead() && file.length() > 0
            log(onLine, if (ok) "PASS $label" else "FAIL $label missing: ${file.absolutePath}")
        }
    }

    private suspend fun runLlmTest(onLine: suspend (String) -> Unit) {
        log(onLine, "\n[LLM] Qwen3.5-2B via llama.cpp JNI")
        val model = ModelPaths.llmGguf
        if (!model.isFile) {
            log(onLine, "SKIP LLM — model file missing")
            return
        }
        try {
            val engine = AiChat.getInferenceEngine(context.applicationContext)
            waitForState(engine, InferenceEngine.State.Initialized, onLine)

            val loadMs = measureTimeMillis {
                engine.loadModel(model.absolutePath)
            }
            log(onLine, "PASS load (${loadMs}ms)")

            val prompt = "Extract JSON with title and due_date from: Chapter 11 due August 25."
            val tokens = mutableListOf<String>()
            val genMs = measureTimeMillis {
                engine.sendUserPrompt(prompt, predictLength = 48)
                    .take(48)
                    .toList()
                    .also { tokens.addAll(it) }
            }
            val text = tokens.joinToString("")
            log(onLine, "PASS generate (${genMs}ms, ${tokens.size} tokens)")
            log(onLine, "Output: ${text.take(200).replace('\n', ' ')}")
            engine.cleanUp()
        } catch (e: Exception) {
            log(onLine, "FAIL LLM — ${e.javaClass.simpleName}: ${e.message}")
            Log.e(TAG, "LLM test failed", e)
        }
    }

    private suspend fun runWhisperTest(onLine: suspend (String) -> Unit) {
        log(onLine, "\n[STT] Whisper base via whisper.cpp JNI")
        val model = ModelPaths.whisperBin
        val wav = ModelPaths.jfkWav
        if (!model.isFile || !wav.isFile) {
            log(onLine, "SKIP Whisper — model or wav missing")
            return
        }
        var ctx: WhisperContext? = null
        try {
            val loadMs = measureTimeMillis {
                ctx = WhisperContext.createContextFromFile(model.absolutePath)
            }
            log(onLine, "PASS load (${loadMs}ms)")

            val audio = WaveDecoder.decode(wav)
            var transcript = ""
            val transcribeMs = measureTimeMillis {
                transcript = ctx!!.transcribeData(audio).trim()
            }
            val ok = transcript.contains("fellow Americans", ignoreCase = true)
            log(onLine, if (ok) "PASS transcribe (${transcribeMs}ms)" else "FAIL transcribe (${transcribeMs}ms)")
            log(onLine, "Text: $transcript")
        } catch (e: Exception) {
            log(onLine, "FAIL Whisper — ${e.message}")
        } finally {
            try {
                ctx?.release()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun runEmbedTest(onLine: suspend (String) -> Unit) {
        log(onLine, "\n[Embed] EmbeddingGemma TFLite")
        val model = ModelPaths.embedTflite
        if (!model.isFile) {
            log(onLine, "SKIP Embed — tflite missing")
            return
        }
        try {
            var shape = ""
            val ms = measureTimeMillis {
                Interpreter(model).use { interpreter ->
                    interpreter.allocateTensors()
                    val input = interpreter.getInputTensor(0)
                    val output = interpreter.getOutputTensor(0)
                    shape = "in=${input.shape().contentToString()} out=${output.shape().contentToString()}"
                }
            }
            log(onLine, "PASS load + allocate (${ms}ms)")
            log(onLine, shape)
        } catch (e: Exception) {
            log(onLine, "FAIL Embed — ${e.message}")
        }
    }

    private suspend fun runOcrTest(onLine: suspend (String) -> Unit) {
        log(onLine, "\n[OCR] ML Kit Text Recognition")
        try {
            val bitmap = Bitmap.createBitmap(640, 200, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                drawColor(Color.WHITE)
                drawText(
                    "Mneme OCR Test 25 Aug",
                    40f,
                    120f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        textSize = 48f
                    }
                )
            }
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            var text = ""
            val ms = measureTimeMillis {
                text = suspendCancellableCoroutine { cont ->
                    recognizer.process(image)
                        .addOnSuccessListener { cont.resume(it.text) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                }
            }
            val ok = text.contains("Mneme", ignoreCase = true) && text.contains("25")
            log(onLine, if (ok) "PASS OCR (${ms}ms)" else "FAIL OCR (${ms}ms)")
            log(onLine, "Text: $text")
        } catch (e: Exception) {
            log(onLine, "FAIL OCR — ${e.message}")
        }
    }

    private suspend fun runFallbackNote(onLine: suspend (String) -> Unit) {
        log(onLine, "\n[Fallback] Qwen2.5 LiteRT .task")
        val task = ModelPaths.fallbackTask
        if (task.isFile && task.length() > 0) {
            log(onLine, "SKIP runtime — file present (${task.length()} bytes); LiteRT-LM wiring in main app")
        } else {
            log(onLine, "SKIP — fallback .task not on device")
        }
    }

    private suspend fun waitForState(
        engine: InferenceEngine,
        want: InferenceEngine.State,
        onLine: suspend (String) -> Unit,
    ) {
        var spins = 0
        while (engine.state.value::class != want::class && spins < 120) {
            kotlinx.coroutines.delay(250)
            spins++
        }
        if (engine.state.value::class != want::class) {
            throw IllegalStateException("Engine not ${want::class.simpleName}: ${engine.state.value}")
        }
        log(onLine, "Engine ready: ${want::class.simpleName}")
    }

    companion object {
        private const val TAG = "MnemeTest"
    }
}
