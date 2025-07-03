import javax.sound.sampled.*
import javax.swing.*
import java.awt.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.*

class EightBitSoundGenerator : JFrame("8-bit Sound Generator") {
    // Wave types
    enum class WaveType { SQUARE, SAWTOOTH, SINE, NOISE }

    // Parameters
    private var waveType = WaveType.SQUARE
    private var sampleRate = 44100

    // Envelope
    private var attackTime = 0.05f
    private var sustainTime = 0.1f
    private var sustainPunch = 0.0f
    private var decayTime = 0.2f

    // Frequency
    private var startFrequency = 440f
    private var minFrequencyCutoff = 20f
    private var slide = 0.0f
    private var deltaSlide = 0.0f

    // Vibrato
    private var vibratoDepth = 0.0f
    private var vibratoSpeed = 0.0f

    // Arpeggiation
    private var arpFrequencyMult = 1.0f
    private var arpChangeSpeed = 0.0f

    // Duty Cycle
    private var dutyCycle = 0.5f
    private var dutySweep = 0.0f

    // Retrigger
    private var retriggerRate = 0.0f

    // Flanger
    private var flangerOffset = 0.0f
    private var flangerSweep = 0.0f

    // Low-Pass Filter
    private var lpfCutoff = 22000f
    private var lpfCutoffSweep = 0.0f
    private var lpfResonance = 0.0f

    // High-Pass Filter
    private var hpfCutoff = 0f
    private var hpfCutoffSweep = 0.0f

    // UI Components
    private val waveTypeCombo = JComboBox(WaveType.values())
    private val playButton = JButton("Play")
    private val randomButton = JButton("Random")
    private val exportButton = JButton("Export WAV")

    private val sampleRateButtons = listOf(
        JToggleButton("44k", true),
        JToggleButton("22k"),
        JToggleButton("11k"),
        JToggleButton("6k")
    )

    private val sliders = mutableMapOf<String, JSlider>()
    private val sliderValues = mutableMapOf<String, JLabel>()

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        layout = BorderLayout()

        // Top panel - wave type and play controls
        val topPanel = JPanel()
        topPanel.add(JLabel("Wave Type:"))
        topPanel.add(waveTypeCombo)
        topPanel.add(playButton)
        topPanel.add(randomButton)
        topPanel.add(exportButton)

        // Sample rate buttons
        val sampleRatePanel = JPanel()
        sampleRatePanel.add(JLabel("Sample Rate:"))
        val buttonGroup = ButtonGroup()
        sampleRateButtons.forEach {
            buttonGroup.add(it)
            sampleRatePanel.add(it)
        }

        val topContainer = JPanel(BorderLayout())
        topContainer.add(topPanel, BorderLayout.NORTH)
        topContainer.add(sampleRatePanel, BorderLayout.SOUTH)

        add(topContainer, BorderLayout.NORTH)

        // Main panel with sliders
        val mainPanel = JPanel()
        mainPanel.layout = GridLayout(0, 3, 5, 5)

        // Add all parameter sliders
        addParameterSlider(mainPanel, "Attack time", 0.0f, 1.0f, attackTime, 0.01f)
        addParameterSlider(mainPanel, "Sustain time", 0.0f, 1.0f, sustainTime, 0.01f)
        addParameterSlider(mainPanel, "Sustain punch", 0.0f, 1.0f, sustainPunch, 0.01f)
        addParameterSlider(mainPanel, "Decay time", 0.0f, 1.0f, decayTime, 0.01f)

        addParameterSlider(mainPanel, "Start frequency", 20f, 2000f, startFrequency, 1f)
        addParameterSlider(mainPanel, "Min freq. cutoff", 20f, 2000f, minFrequencyCutoff, 1f)
        addParameterSlider(mainPanel, "Slide", -1.0f, 1.0f, slide, 0.01f)
        addParameterSlider(mainPanel, "Delta slide", -1.0f, 1.0f, deltaSlide, 0.01f)

        addParameterSlider(mainPanel, "Vibrato depth", 0.0f, 1.0f, vibratoDepth, 0.01f)
        addParameterSlider(mainPanel, "Vibrato speed", 0.0f, 20f, vibratoSpeed, 0.1f)

        addParameterSlider(mainPanel, "Arp freq mult", 0.0f, 3.0f, arpFrequencyMult, 0.1f)
        addParameterSlider(mainPanel, "Arp change speed", 0.0f, 10f, arpChangeSpeed, 0.1f)

        addParameterSlider(mainPanel, "Duty cycle", 0.0f, 1.0f, dutyCycle, 0.01f)
        addParameterSlider(mainPanel, "Duty sweep", -1.0f, 1.0f, dutySweep, 0.01f)

        addParameterSlider(mainPanel, "Retrigger rate", 0.0f, 20f, retriggerRate, 0.1f)

        addParameterSlider(mainPanel, "Flanger offset", 0.0f, 0.1f, flangerOffset, 0.001f)
        addParameterSlider(mainPanel, "Flanger sweep", -0.1f, 0.1f, flangerSweep, 0.001f)

        addParameterSlider(mainPanel, "LPF cutoff", 20f, 22000f, lpfCutoff, 10f)
        addParameterSlider(mainPanel, "LPF cutoff sweep", -22000f, 22000f, lpfCutoffSweep, 10f)
        addParameterSlider(mainPanel, "LPF resonance", 0.0f, 1.0f, lpfResonance, 0.01f)

        addParameterSlider(mainPanel, "HPF cutoff", 0f, 22000f, hpfCutoff, 10f)
        addParameterSlider(mainPanel, "HPF cutoff sweep", -22000f, 22000f, hpfCutoffSweep, 10f)

        val scrollPane = JScrollPane(mainPanel)
        add(scrollPane, BorderLayout.CENTER)

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 600)
        isVisible = true
    }

    private fun addParameterSlider(panel: JPanel, name: String, min: Float, max: Float, initial: Float, step: Float) {
        val label = JLabel(name)
        val valueLabel = JLabel(String.format("%.3f", initial))
        val slider = JSlider(0, 1000, ((initial - min) / (max - min) * 1000).toInt())

        panel.add(label)
        panel.add(slider)
        panel.add(valueLabel)

        sliders[name] = slider
        sliderValues[name] = valueLabel

        slider.addChangeListener {
            val value = min + (slider.value / 1000.0f) * (max - min)
            valueLabel.text = String.format("%.3f", value)
            updateParameter(name, value)
        }
    }

    private fun updateParameter(name: String, value: Float) {
        when (name) {
            "Attack time" -> attackTime = value
            "Sustain time" -> sustainTime = value
            "Sustain punch" -> sustainPunch = value
            "Decay time" -> decayTime = value

            "Start frequency" -> startFrequency = value
            "Min freq. cutoff" -> minFrequencyCutoff = value
            "Slide" -> slide = value
            "Delta slide" -> deltaSlide = value

            "Vibrato depth" -> vibratoDepth = value
            "Vibrato speed" -> vibratoSpeed = value

            "Arp freq mult" -> arpFrequencyMult = value
            "Arp change speed" -> arpChangeSpeed = value

            "Duty cycle" -> dutyCycle = value
            "Duty sweep" -> dutySweep = value

            "Retrigger rate" -> retriggerRate = value

            "Flanger offset" -> flangerOffset = value
            "Flanger sweep" -> flangerSweep = value

            "LPF cutoff" -> lpfCutoff = value
            "LPF cutoff sweep" -> lpfCutoffSweep = value
            "LPF resonance" -> lpfResonance = value

            "HPF cutoff" -> hpfCutoff = value
            "HPF cutoff sweep" -> hpfCutoffSweep = value
        }
    }

    private fun setupListeners() {
        playButton.addActionListener { playSound() }
        randomButton.addActionListener { randomizeParameters() }
        exportButton.addActionListener { exportWav() }

        waveTypeCombo.addActionListener {
            waveType = waveTypeCombo.selectedItem as WaveType
        }

        sampleRateButtons.forEachIndexed { index, button ->
            button.addActionListener {
                sampleRate = when (index) {
                    0 -> 44100
                    1 -> 22050
                    2 -> 11025
                    3 -> 6000
                    else -> 44100
                }
            }
        }
    }

    private fun randomizeParameters() {
        val random = Random()

        fun randomFloat(min: Float, max: Float): Float = min + random.nextFloat() * (max - min)

        waveType = WaveType.values()[random.nextInt(WaveType.values().size)]
        waveTypeCombo.selectedItem = waveType

        attackTime = randomFloat(0.0f, 0.5f)
        sustainTime = randomFloat(0.0f, 0.5f)
        sustainPunch = randomFloat(0.0f, 0.5f)
        decayTime = randomFloat(0.1f, 1.0f)

        startFrequency = randomFloat(50f, 1000f)
        minFrequencyCutoff = randomFloat(20f, 500f)
        slide = randomFloat(-0.5f, 0.5f)
        deltaSlide = randomFloat(-0.5f, 0.5f)

        vibratoDepth = randomFloat(0.0f, 0.5f)
        vibratoSpeed = randomFloat(0.0f, 10f)

        arpFrequencyMult = randomFloat(0.5f, 2.0f)
        arpChangeSpeed = randomFloat(0.0f, 5f)

        dutyCycle = randomFloat(0.1f, 0.9f)
        dutySweep = randomFloat(-0.5f, 0.5f)

        retriggerRate = randomFloat(0.0f, 10f)

        flangerOffset = randomFloat(0.0f, 0.05f)
        flangerSweep = randomFloat(-0.05f, 0.05f)

        lpfCutoff = randomFloat(1000f, 22000f)
        lpfCutoffSweep = randomFloat(-5000f, 5000f)
        lpfResonance = randomFloat(0.0f, 0.5f)

        hpfCutoff = randomFloat(0f, 1000f)
        hpfCutoffSweep = randomFloat(-1000f, 1000f)

        // Update all sliders
        sliders.forEach { (name, slider) ->
            val (min, max) = when (name) {
                "Attack time" -> 0.0f to 1.0f
                "Sustain time" -> 0.0f to 1.0f
                "Sustain punch" -> 0.0f to 1.0f
                "Decay time" -> 0.0f to 1.0f

                "Start frequency" -> 20f to 2000f
                "Min freq. cutoff" -> 20f to 2000f
                "Slide" -> -1.0f to 1.0f
                "Delta slide" -> -1.0f to 1.0f

                "Vibrato depth" -> 0.0f to 1.0f
                "Vibrato speed" -> 0.0f to 20f

                "Arp freq mult" -> 0.0f to 3.0f
                "Arp change speed" -> 0.0f to 10f

                "Duty cycle" -> 0.0f to 1.0f
                "Duty sweep" -> -1.0f to 1.0f

                "Retrigger rate" -> 0.0f to 20f

                "Flanger offset" -> 0.0f to 0.1f
                "Flanger sweep" -> -0.1f to 0.1f

                "LPF cutoff" -> 20f to 22000f
                "LPF cutoff sweep" -> -22000f to 22000f
                "LPF resonance" -> 0.0f to 1.0f

                "HPF cutoff" -> 0f to 22000f
                "HPF cutoff sweep" -> -22000f to 22000f
                else -> 0f to 1f
            }

            val value = when (name) {
                "Attack time" -> attackTime
                "Sustain time" -> sustainTime
                "Sustain punch" -> sustainPunch
                "Decay time" -> decayTime

                "Start frequency" -> startFrequency
                "Min freq. cutoff" -> minFrequencyCutoff
                "Slide" -> slide
                "Delta slide" -> deltaSlide

                "Vibrato depth" -> vibratoDepth
                "Vibrato speed" -> vibratoSpeed

                "Arp freq mult" -> arpFrequencyMult
                "Arp change speed" -> arpChangeSpeed

                "Duty cycle" -> dutyCycle
                "Duty sweep" -> dutySweep

                "Retrigger rate" -> retriggerRate

                "Flanger offset" -> flangerOffset
                "Flanger sweep" -> flangerSweep

                "LPF cutoff" -> lpfCutoff
                "LPF cutoff sweep" -> lpfCutoffSweep
                "LPF resonance" -> lpfResonance

                "HPF cutoff" -> hpfCutoff
                "HPF cutoff sweep" -> hpfCutoffSweep
                else -> 0f
            }

            slider.value = ((value - min) / (max - min) * 1000).toInt()
            sliderValues[name]?.text = String.format("%.3f", value)
        }
    }

    private fun playSound() {
        val soundBytes = generateSound(1.0) // 1 second duration
        val audioFormat = AudioFormat(sampleRate.toFloat(), 8, 1, true, false)
        val audioInputStream = AudioInputStream(ByteArrayInputStream(soundBytes), audioFormat, soundBytes.size.toLong())

        try {
            val clip = AudioSystem.getClip()
            clip.open(audioInputStream)
            clip.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun exportWav() {
        val soundBytes = generateSound(3.0) // 3 second duration for export
        val audioFormat = AudioFormat(sampleRate.toFloat(), 8, 1, true, false)

        val file = File("C:\\Users\\sp2lc\\Music\\lewapnoob\\8bit_sound_${System.currentTimeMillis()}.wav")
        file.parentFile.mkdirs()

        try {
            val audioInputStream = AudioInputStream(ByteArrayInputStream(soundBytes), audioFormat, soundBytes.size.toLong())
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file)
            JOptionPane.showMessageDialog(this, "Sound exported to:\n${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(this, "Error exporting sound: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun generateSound(duration: Double): ByteArray {
        val length = (duration * sampleRate).toInt()
        val sound = ByteArray(length)

        var phase = 0.0
        var currentFrequency = startFrequency.toDouble()
        var currentSlide = slide.toDouble()
        var currentDuty = dutyCycle.toDouble()

        var lpfCutoffCurrent = lpfCutoff.toDouble()
        var lpfCutoffSweepCurrent = lpfCutoffSweep.toDouble()
        var lpfResonanceCurrent = lpfResonance.toDouble()

        var hpfCutoffCurrent = hpfCutoff.toDouble()
        var hpfCutoffSweepCurrent = hpfCutoffSweep.toDouble()

        var flangerOffsetCurrent = flangerOffset.toDouble()
        var flangerSweepCurrent = flangerSweep.toDouble()

        var lpfBuffer = 0.0
        var lpfBuffer2 = 0.0
        var hpfBuffer = 0.0

        for (i in 0 until length) {
            val time = i.toDouble() / sampleRate

            // Envelope
            val envelope = when {
                time < attackTime -> time / attackTime.toDouble()
                time < attackTime + sustainTime -> 1.0 + sustainPunch * (1.0 - (time - attackTime) / sustainTime.toDouble())
                else -> max(0.0, 1.0 - (time - attackTime - sustainTime) / decayTime.toDouble())
            }

            // Frequency modulation
            currentFrequency += currentSlide
            currentSlide += deltaSlide / sampleRate.toDouble()
            currentFrequency = max(minFrequencyCutoff.toDouble(), currentFrequency)

            // Vibrato
            val vibrato = vibratoDepth * sin(2.0 * PI * vibratoSpeed * time)

            // Arpeggiation
            val arpMod = if (arpChangeSpeed > 0) {
                floor(time * arpChangeSpeed * 3.0) % 3.0
            } else {
                0.0
            }
            val arpFreqMult = when (arpMod.toInt()) {
                1 -> arpFrequencyMult * 0.5
                2 -> arpFrequencyMult * 0.25
                else -> arpFrequencyMult
            }

            // Duty cycle sweep
            currentDuty += dutySweep / sampleRate.toDouble()
            currentDuty = max(0.0, min(1.0, currentDuty))

            // Flanger
            flangerOffsetCurrent += flangerSweepCurrent / sampleRate.toDouble()
            flangerOffsetCurrent = max(0.0, min(0.1, flangerOffsetCurrent))

            // Calculate actual frequency with all modulations
            val actualFrequency = currentFrequency * (1.0 + vibratoDepth.toDouble()) * arpFreqMult.toDouble()

            // Generate wave
            phase += actualFrequency / sampleRate.toDouble()
            phase %= 1.0

            val sample = when (waveType) {
                WaveType.SQUARE -> if (phase < currentDuty) 1.0 else -1.0
                WaveType.SAWTOOTH -> phase * 2.0 - 1.0
                WaveType.SINE -> sin(2.0 * PI * phase)
                WaveType.NOISE -> Random().nextDouble() * 2.0 - 1.0
            }

            // Apply envelope
            var processedSample = sample * envelope

            // Apply low-pass filter
            lpfCutoffCurrent += lpfCutoffSweepCurrent / sampleRate.toDouble()
            lpfCutoffCurrent = max(20.0, min(22000.0, lpfCutoffCurrent))

            val lpfRC = 1.0 / (2.0 * PI * lpfCutoffCurrent)
            val lpfAlpha = 1.0 / (1.0 + lpfRC * sampleRate.toDouble())

            lpfBuffer += lpfAlpha * (processedSample - lpfBuffer)
            lpfBuffer2 += lpfAlpha * (lpfBuffer - lpfBuffer2)
            processedSample = lpfBuffer2 * (1.0 - lpfResonanceCurrent) + processedSample * lpfResonanceCurrent

            // Apply high-pass filter
            hpfCutoffCurrent += hpfCutoffSweepCurrent / sampleRate.toDouble()
            hpfCutoffCurrent = max(0.0, min(22000.0, hpfCutoffCurrent))

            if (hpfCutoffCurrent > 0) {
                val hpfRC = 1.0 / (2.0 * PI * hpfCutoffCurrent)
                val hpfAlpha = hpfRC / (hpfRC + 1.0 / sampleRate.toDouble())
                hpfBuffer += hpfAlpha * (processedSample - hpfBuffer)
                processedSample -= hpfBuffer
            }

            // Convert to 8-bit
            val byteSample = (processedSample * 127.0).toInt().toByte()
            sound[i] = byteSample

            // Retrigger effect
            if (retriggerRate > 0 && i % (sampleRate / retriggerRate).toInt() == 0) {
                phase = 0.0
            }
        }

        return sound
    }
}

fun main() {
    SwingUtilities.invokeLater {
        EightBitSoundGenerator()
    }
}