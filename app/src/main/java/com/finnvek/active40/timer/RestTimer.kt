package com.finnvek.active40.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finnvek.active40.MainActivity
import com.finnvek.active40.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Sovelluksen laajuinen palautusajastin. Perustuu päättymishetkeen (aikaleima), joten jäljellä oleva aika
 * pysyy oikeana, vaikka sovellus olisi taustalla. Päättyessä: värinä, jos sovellus on näkyvissä,
 * muuten ilmoitus (jos ilmoitukset on sallittu). Ajastin toimii myös ilman ilmoituslupaa.
 */
class RestTimer(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _endsAt = MutableStateFlow<Instant?>(null)

    /** Päättymishetki tai null, kun ajastin ei ole käynnissä. */
    val endsAt: StateFlow<Instant?> = _endsAt.asStateFlow()

    /** MainActivity päivittää tämän onStart/onStop-kutsuissa. */
    @Volatile
    var isAppInForeground: Boolean = false

    private var job: Job? = null

    fun start(seconds: Int) = schedule(Instant.now().plusSeconds(seconds.toLong()))

    /** Siirtää käynnissä olevan ajastimen päättymistä. Jos aikaa ei jää, ajastin pysähtyy. */
    fun shift(deltaSeconds: Long) {
        val current = _endsAt.value ?: return
        val shifted = current.plusSeconds(deltaSeconds)
        if (shifted.isAfter(Instant.now())) schedule(shifted) else stop()
    }

    fun stop() {
        job?.cancel()
        job = null
        _endsAt.value = null
    }

    private fun schedule(endsAt: Instant) {
        job?.cancel()
        _endsAt.value = endsAt
        job =
            scope.launch {
                delay(Duration.between(Instant.now(), endsAt).toMillis().coerceAtLeast(0))
                _endsAt.value = null
                onFinished()
            }
    }

    private fun onFinished() {
        if (isAppInForeground) vibrate() else notifyFinished()
    }

    private fun vibrate() {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }
        val effect = VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        // Ilmoitusluokka: kosketuspalautteen poiskytkentä ei mykistä ajastinta, äänetön tila kylläkin.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator?.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
            )
        }
    }

    private fun notifyFinished() {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val openApp =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_strength)
                .setContentTitle(context.getString(R.string.rest_notification_title))
                .setContentText(context.getString(R.string.rest_notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Lupa peruttiin juuri ennen ilmoitusta; ajastin toimii silti.
        }
    }

    /** Luo ilmoituskanavan. Kutsutaan sovelluksen käynnistyessä. */
    fun createChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.rest_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { enableVibration(true) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "rest_timer"
        const val NOTIFICATION_ID = 1
        const val VIBRATION_MS = 400L
    }
}
