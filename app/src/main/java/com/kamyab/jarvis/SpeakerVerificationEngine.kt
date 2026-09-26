package com.kamyab.jarvis

import android.content.Context
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import org.jtransforms.fft.DoubleFFT_1D
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

class SpeakerVerificationEngine(private val context: Context) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val EMBEDDING_SIZE = 192
        const val MEL_BINS = 80
        const val FFT_SIZE = 400
        const val HOP = 160
        const val FRAMES = 150
        const val THRESHOLD = 0.40f
        private const val MODEL_URL = "https://ml-models-bucket.gauthamvijay.com/ecapa-body-192.tflite"
    }

    private val modelFile = File(context.filesDir, "ecapa-body-192.tflite")
    private var model: CompiledModel? = null

    fun isModelReady() = modelFile.exists() && modelFile.length() > 1_000_000

    fun downloadModelIfNeeded(onDone: (Result<File>) -> Unit) {
        Thread {
            try {
                if (!isModelReady()) {
                    val conn = URL(MODEL_URL).openConnection() as HttpURLConnection
                    conn.connectTimeout = 20_000
                    conn.readTimeout = 120_000
                    conn.requestMethod = "GET"
                    conn.connect()
                    if (conn.responseCode !in 200..299) error("model HTTP " + conn.responseCode)
                    val tmp = File(context.filesDir, "ecapa.tmp")
                    conn.inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                    conn.disconnect()
                    if (tmp.length() < 1_000_000) error("downloaded model is invalid")
                    if (modelFile.exists()) modelFile.delete()
                    if (!tmp.renameTo(modelFile)) error("cannot install model")
                }
                onDone(Result.success(modelFile))
            } catch (t: Throwable) {
                onDone(Result.failure(t))
            }
        }.start()
    }

    @Synchronized
    fun loadModel() {
        if (model != null) return
        check(isModelReady()) { "Speaker model is not downloaded" }
        model = CompiledModel.create(modelFile.absolutePath, CompiledModel.Options(Accelerator.CPU))
    }

    @Synchronized
    fun close() {
        model?.close()
        model = null
    }

    fun embed(pcm16: ShortArray): FloatArray {
        check(pcm16.size >= SAMPLE_RATE) { "at least 1 second of audio is required" }
        loadModel()
        val features = melFeatures(pcm16)
        val inputs = model!!.createInputBuffers()
        val outputs = model!!.createOutputBuffers()
        try {
            inputs[0].writeFloat(features)
            model!!.run(inputs, outputs)
            val out = outputs[0].readFloat()
            require(out.size >= EMBEDDING_SIZE) { "unexpected ECAPA output size: " + out.size }
            return l2(out.copyOf(EMBEDDING_SIZE))
        } finally {
            inputs.forEach { it.close() }
            outputs.forEach { it.close() }
        }
    }

    fun similarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == EMBEDDING_SIZE && b.size == EMBEDDING_SIZE)
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }

    fun verify(probe: FloatArray, enrolled: FloatArray, threshold: Float = THRESHOLD): Boolean =
        similarity(probe, enrolled) >= threshold

    private fun melFeatures(pcm: ShortArray): FloatArray {
        val targetSamples = (FRAMES - 1) * HOP + FFT_SIZE
        val x = FloatArray(targetSamples)
        val start = max(0, (pcm.size - targetSamples) / 2)
        val count = min(targetSamples, pcm.size - start)
        for (i in 0 until count) x[i] = pcm[start + i] / 32768f

        val out = FloatArray(FRAMES * MEL_BINS)
        val fft = DoubleFFT_1D(FFT_SIZE.toLong())
        val spec = DoubleArray(FFT_SIZE * 2)
        val melBank = melBank()

        for (frame in 0 until FRAMES) {
            val base = frame * HOP
            java.util.Arrays.fill(spec, 0.0)
            for (n in 0 until FFT_SIZE) {
                val sample = if (base + n < x.size) x[base + n] else 0f
                val win = 0.54 - 0.46 * cos(2.0 * Math.PI * n / (FFT_SIZE - 1))
                spec[2 * n] = sample * win
            }
            fft.realForwardFull(spec)

            val power = DoubleArray(FFT_SIZE / 2 + 1)
            for (k in power.indices) {
                val re = spec[2 * k]
                val im = spec[2 * k + 1]
                power[k] = (re * re + im * im) / FFT_SIZE
            }

            for (m in 0 until MEL_BINS) {
                var e = 0.0
                for (k in power.indices) e += power[k] * melBank[m][k]
                out[frame * MEL_BINS + m] = ln(max(e, 1e-10)).toFloat()
            }
        }

        for (m in 0 until MEL_BINS) {
            var mean = 0f
            for (f in 0 until FRAMES) mean += out[f * MEL_BINS + m]
            mean /= FRAMES
            for (f in 0 until FRAMES) out[f * MEL_BINS + m] -= mean
        }
        return out
    }

    private fun melBank(): Array<DoubleArray> {
        val bins = FFT_SIZE / 2 + 1
        val low = 80.0
        val high = 7600.0
        fun hzToMel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
        fun melToHz(m: Double) = 700.0 * (10.0.pow(m / 2595.0) - 1.0)
        val loMel = hzToMel(low)
        val hiMel = hzToMel(high)
        val points = IntArray(MEL_BINS + 2) {
            val mel = loMel + (hiMel - loMel) * it / (MEL_BINS + 1)
            floor((FFT_SIZE + 1) * melToHz(mel) / SAMPLE_RATE).toInt().coerceIn(0, bins - 1)
        }
        return Array(MEL_BINS) { m ->
            DoubleArray(bins).also { w ->
                val l = points[m]
                val c = points[m + 1]
                val r = points[m + 2]
                for (k in l until c) if (c > l) w[k] = (k - l).toDouble() / (c - l)
                for (k in c..r) if (r > c) w[k] = max(w[k], (r - k).toDouble() / (r - c))
            }
        }
    }

    private fun l2(x: FloatArray): FloatArray {
        var s = 0f
        for (v in x) s += v * v
        val n = sqrt(max(s, 1e-12f))
        return FloatArray(x.size) { x[it] / n }
    }
}
