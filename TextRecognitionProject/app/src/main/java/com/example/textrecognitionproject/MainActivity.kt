package com.example.textrecognitionproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : AppCompatActivity(), RecognitionListener {
    private lateinit var textoTranscricao: TextView
    private lateinit var botaoGravar: FloatingActionButton
    private lateinit var reconhecedorDeFala: SpeechRecognizer
    private lateinit var spinnerLanguages: Spinner
    private lateinit var intencaoReconhecedor: Intent
    private var idiomaSelecionado: String = Locale.getDefault().toString()
    private var estadoDaFala: EstadoDaFala = EstadoDaFala.PRONTO

    private enum class EstadoDaFala {
        PRONTO, OUVINDO, ERRO
    }

    companion object {
        private const val CODIGO_REQUISICAO_PERMISSAO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textoTranscricao = findViewById(R.id.textViewTranscription)
        botaoGravar = findViewById(R.id.btnRecord)
        spinnerLanguages = findViewById(R.id.spinnerLanguages)
        configurarspinnerLanguages()
        configurarReconhecedorDeFala()
        atualizarInterface(EstadoDaFala.PRONTO)

        botaoGravar.setOnClickListener {
            if (estadoDaFala == EstadoDaFala.PRONTO) {
                verificarPermissaoAudio()
            } else {
                pararDeOuvir()
            }
        }
    }
    private fun configurarspinnerLanguages() {
        val idiomas = listOf(
            "Português (Brasil)" to "pt-BR",
            "Inglês (EUA)" to "en-US",
            "Francês" to "fr-FR",
            "Italiano" to "it-IT",
            "Russo" to "ru-RU"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, idiomas.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguages.adapter = adapter

        spinnerLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                idiomaSelecionado = idiomas[position].second
                configurarReconhecedorDeFala()
            }

            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
    }
    private fun configurarReconhecedorDeFala() {
        reconhecedorDeFala = SpeechRecognizer.createSpeechRecognizer(this)
        reconhecedorDeFala.setRecognitionListener(this)

        intencaoReconhecedor = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, idiomaSelecionado)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    private fun verificarPermissaoAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), CODIGO_REQUISICAO_PERMISSAO)
        } else {
            iniciarOuvir()
        }
    }

    private fun iniciarOuvir() {
        atualizarInterface(EstadoDaFala.OUVINDO)
        textoTranscricao.text = "Falando..."
        reconhecedorDeFala.startListening(intencaoReconhecedor)
    }

    private fun pararDeOuvir() {
        atualizarInterface(EstadoDaFala.PRONTO)
        reconhecedorDeFala.stopListening()
    }

    private fun atualizarInterface(estado: EstadoDaFala) {
        estadoDaFala = estado
        when (estado) {
            EstadoDaFala.PRONTO -> {
                botaoGravar.setImageResource(R.drawable.ic_mic)
                botaoGravar.contentDescription = getString(R.string.record_button_description_ready)
            }
            EstadoDaFala.OUVINDO -> {
                botaoGravar.setImageResource(R.drawable.ic_mic_off)
                botaoGravar.contentDescription = getString(R.string.record_button_description_listening)
            }
            EstadoDaFala.ERRO -> {
                botaoGravar.setImageResource(R.drawable.ic_mic)
                botaoGravar.contentDescription = getString(R.string.record_button_description_ready)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_REQUISICAO_PERMISSAO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarOuvir()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(results: Bundle?) {
        val correspondencias = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (correspondencias != null && correspondencias.isNotEmpty()) {
            textoTranscricao.text = correspondencias.first()
        }
        atualizarInterface(EstadoDaFala.PRONTO)
    }

    override fun onReadyForSpeech(params: Bundle?) { }
    override fun onBeginningOfSpeech() { }
    override fun onRmsChanged(rmsdB: Float) { }
    override fun onBufferReceived(buffer: ByteArray?) { }
    override fun onEndOfSpeech() { }

    override fun onError(error: Int) {
        Log.d("SpeechRecognizer", "Erro código: $error")
        atualizarInterface(EstadoDaFala.ERRO)
        val mensagemDeErro = when (error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> getString(R.string.error_network_timeout)
            SpeechRecognizer.ERROR_NETWORK -> getString(R.string.error_network)
            SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.error_no_match)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> getString(R.string.error_permissions)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.error_speech_timeout)
            else -> getString(R.string.error_unknown)
        }
        Toast.makeText(this, mensagemDeErro, Toast.LENGTH_SHORT).show()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val correspondencias = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (correspondencias != null && correspondencias.isNotEmpty()) {
            textoTranscricao.text = correspondencias.first()
        }
    }
    override fun onEvent(eventType: Int, params: Bundle?) { }

    override fun onDestroy() {
        super.onDestroy()
        reconhecedorDeFala.destroy()
    }
}