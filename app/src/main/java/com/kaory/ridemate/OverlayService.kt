package com.kaory.ridemate

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.kaory.ridemate.data.repository.TelemetryRepository
import com.kaory.ridemate.domain.notification.NotificationEngine
import com.kaory.ridemate.domain.notification.SoundManager
import com.kaory.ridemate.domain.notification.SoundType
import com.kaory.ridemate.domain.notification.model.Notification as Notif
import com.kaory.ridemate.domain.notification.model.*
import com.kaory.ridemate.domain.tracker.DailyStatsTracker
import com.kaory.ridemate.domain.tracker.RecordTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var notificationEngine: NotificationEngine
    @Inject lateinit var recordTracker: RecordTracker
    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var telemetryRepository: TelemetryRepository
    @Inject lateinit var dailyStatsTracker: DailyStatsTracker

    private lateinit var windowManager: WindowManager
    private var mainOverlay: FrameLayout? = null
    private var minimisedIcon: View? = null
    private var gridLayout: GridLayout? = null
    private var neonBorder: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isOverlayShown = false
    private var isMinimised = false

    private var lastY = 0f
    private var isDragging = false

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startTelemetryProcessing()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
            else -> hideOverlay()
        }
        return START_STICKY
    }

    private fun startTelemetryProcessing() {
        var lastTotalKm = 0f
        var isFirstValue = true
        var previousNotificationIds = setOf<String>()

        scope.launch {
            telemetryRepository.processedTelemetry.collect { telemetry ->
                val currentTotal = telemetry.totalDistanceKm.toFloat()
                if (isFirstValue) {
                    lastTotalKm = currentTotal
                    isFirstValue = false
                    return@collect
                }
                val delta = if (currentTotal >= lastTotalKm) currentTotal - lastTotalKm else 0f
                if (delta > 0) lastTotalKm = currentTotal
                val speedKmh = telemetry.speedKmh
                if (delta > 0 || speedKmh > 0.1f) {
                    dailyStatsTracker.updateDailyStats(delta, speedKmh)
                }

                notificationEngine.updateTelemetry(
                    speedKmh = speedKmh,
                    deltaKm = delta,
                    totalDistanceKm = currentTotal,
                    dailyDistanceKm = recordTracker.state.value.dailyDistance
                )
                recordTracker.processTelemetry(speedKmh, delta)
            }
        }

        scope.launch {
            notificationEngine.activeNotifications.collect { activeList ->
                val newIds = activeList.map { it.id }.toSet()
                val addedIds = newIds - previousNotificationIds
                for (id in addedIds) {
                    val notif = activeList.find { it.id == id } ?: continue
                    val soundType = when (notif.trigger) {
                        TriggerType.SPEED -> {
                            val direction = notif.speedDirection ?: SpeedDirection.ABOVE
                            if (notif.type == NotificationType.PERSONAL) {
                                if (direction == SpeedDirection.ABOVE) SoundType.SPEED_ABOVE_PERSONAL
                                else SoundType.SPEED_BELOW_PERSONAL
                            } else {
                                if (direction == SpeedDirection.ABOVE) SoundType.SPEED_ABOVE_SERVICE
                                else SoundType.SPEED_BELOW_SERVICE
                            }
                        }
                        TriggerType.DISTANCE -> {
                            if (notif.type == NotificationType.PERSONAL) SoundType.DISTANCE_PERSONAL
                            else SoundType.DISTANCE_SERVICE
                        }
                    }
                    soundManager.startNotificationLoop(notif.id, soundType)
                }
                val removedIds = previousNotificationIds - newIds
                removedIds.forEach { id -> soundManager.stopNotificationLoop(id) }
                previousNotificationIds = newIds
            }
        }

        scope.launch {
            recordTracker.state.collect { state ->
                if (state.isNewSpeedRecord || state.isNewDailyRecord) {
                    soundManager.playRecordAlert()
                }
            }
        }
    }

    private fun showOverlay() {
        if (isOverlayShown && !isMinimised) return
        hideOverlay()

        val widthDp = 150.dp
        val params = WindowManager.LayoutParams(
            widthDp,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            y = 100
        }

        val leftRadius = 30.dp.toFloat()

        // Фон с закруглёнными только левыми углами
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.argb(204, 0, 0, 0))
            setStroke(1, Color.argb(40, 255, 255, 255))
            // Радиусы: верхний левый, верхний правый, нижний правый, нижний левый
            cornerRadii = floatArrayOf(30f, 30f, 0f, 0f, 0f, 0f, 30f, 30f)
        }

        mainOverlay = object : FrameLayout(this) {
            override fun dispatchDraw(canvas: Canvas) {
                val w = width.toFloat()
                val h = height.toFloat()
                if (w > 0 && h > 0) {
                    // Создаём путь с закруглёнными только левыми углами
                    val path = Path().apply {
                        moveTo(0f, leftRadius)
                        // Левое верхнее закругление
                        arcTo(RectF(0f, 0f, leftRadius * 2, leftRadius * 2), 180f, 90f, false)
                        // Верхняя прямая до правого края
                        lineTo(w, 0f)
                        // Правый край вниз
                        lineTo(w, h)
                        // Нижняя прямая влево до левого скругления
                        lineTo(0f, h)
                        // Левое нижнее закругление
                        arcTo(RectF(0f, h - leftRadius * 2, leftRadius * 2, h), 90f, 90f, false)
                        close()
                    }
                    canvas.save()
                    canvas.clipPath(path)
                    // Фон оверлея
                    canvas.drawColor(Color.argb(204, 0, 0, 0))
                    // Дочерние элементы (сетка, кнопки, неоновая рамка)
                    super.dispatchDraw(canvas)
                    canvas.restore()
                }
            }
        }.apply {
            // Фон задаём прозрачным, потому что рисуем в dispatchDraw
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Неоновая рамка
        neonBorder = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(-2, -2, -2, -2)
            }
            background = GradientDrawable().apply {
                setStroke(2, Color.argb(60, 255, 255, 255))
                cornerRadii = floatArrayOf(30f, 30f, 0f, 0f, 0f, 0f, 30f, 30f)
            }
            visibility = View.GONE
        }
        mainOverlay?.addView(neonBorder)

        // Сетка
        gridLayout = GridLayout(this).apply {
            columnCount = 2
            rowCount = 4
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(12.dp, 12.dp, 12.dp, 12.dp)
            }
        }
        mainOverlay?.addView(gridLayout)

        // Кнопки
        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply {
                setMargins(12.dp, 8.dp, 12.dp, 8.dp)
            }
        }

        val exitButton = TextView(this).apply {
            text = "✕"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(12, 4, 12, 4)
            setOnClickListener { stopSelf() }
        }
        val minimiseButton = TextView(this).apply {
            text = "➖"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(12, 4, 12, 4)
            setOnClickListener { minimiseOverlay() }
        }
        buttonBar.addView(minimiseButton)
        buttonBar.addView(exitButton)
        mainOverlay?.addView(buttonBar)

        // Перетаскивание
        mainOverlay?.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    isDragging = true
                    view.parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val dy = event.rawY - lastY
                        params.y = (params.y + dy).toInt()
                        windowManager.updateViewLayout(mainOverlay, params)
                        lastY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    view.performClick()
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(mainOverlay, params)
        isOverlayShown = true
        isMinimised = false
        observeData()
    }

    private fun minimiseOverlay() {
        mainOverlay?.visibility = View.GONE

        // Если свёрнутая иконка уже существует, просто выходим
        if (minimisedIcon != null) return

        val miniParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            y = 100
        }

        // Создаём кастомную иконку с текстом "<RM"
        val textView = TextView(this).apply {
            text = "\u276E RM"  // ❮ RM (стрелка влево + текст)
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) // Segoe UI аналог на Android
            gravity = Gravity.CENTER

            // Фон с закруглением (имитация капсулы)
            background = GradientDrawable().apply {
                setColor(Color.argb(180, 0, 0, 0))
                setCornerRadius(30f)
            }

            // Внутренние отступы: слева 12, сверху 8, справа 16, снизу 8
            setPadding(12, 8, 16, 8)

            // Возвращаем оверлей по касанию
            setOnClickListener {
                if (mainOverlay != null && mainOverlay?.isAttachedToWindow == true) {
                    mainOverlay?.visibility = View.VISIBLE
                }
                if (minimisedIcon != null && minimisedIcon?.isAttachedToWindow == true) {
                    windowManager.removeView(minimisedIcon)
                }
                minimisedIcon = null
                isMinimised = false
            }
        }

        minimisedIcon = textView
        windowManager.addView(minimisedIcon, miniParams)
        isMinimised = true
    }

    private fun hideOverlay() {
        mainOverlay?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        minimisedIcon?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        mainOverlay = null
        minimisedIcon = null
        isOverlayShown = false
        isMinimised = false
    }

    private fun observeData() {
        scope.launch {
            notificationEngine.activeNotifications.collect { notifList ->
                updateGrid(notifList)
            }
        }
        scope.launch {
            recordTracker.state.collect { state ->
                if (state.isNewSpeedRecord || state.isNewDailyRecord) {
                    showNeonRecord()
                }
            }
        }
    }

    private fun updateGrid(notifications: List<Notif>) {
        val personal = notifications.filter { it.type == NotificationType.PERSONAL }.take(4)
        val service = notifications.filter { it.type == NotificationType.SERVICE }.take(4)
        val cells = (personal + service).take(8)

        gridLayout?.removeAllViews()
        for (i in 0 until 8) {
            val cell = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 50.dp
                    height = 50.dp
                    setMargins(2, 2, 2, 2)
                }
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.argb(60, 255, 255, 255))
                    setCornerRadius(8f)
                }
            }
            if (i < cells.size) {
                val notif = cells[i]
                val icon = notificationEngine.getUserNotification(notif.id)?.icon ?: notif.title.take(1)
                cell.text = if (icon.length <= 2) icon else icon.take(1)
                cell.background = GradientDrawable().apply {
                    setColor(
                        when (notif.type) {
                            NotificationType.PERSONAL -> Color.argb(204, 76, 175, 80)
                            NotificationType.SERVICE -> Color.argb(204, 33, 150, 243)
                        }
                    )
                    setCornerRadius(12f)
                }
            } else {
                cell.setBackgroundColor(Color.TRANSPARENT)
            }
            gridLayout?.addView(cell)
        }
    }

    private fun showNeonRecord() {
        neonBorder?.apply {
            visibility = View.VISIBLE
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1000
                repeatCount = 1
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animation ->
                    val alpha = animation.animatedValue as Float
                    (background as GradientDrawable).setStroke(
                        4, Color.argb((255 * alpha).toInt(), 0, 191, 255)
                    )
                }
            }
            animator.start()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "RideMate Overlay", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RideMate HUD")
            .setContentText("Уведомления активны")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_SHOW = "com.kaory.ridemate.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE = "com.kaory.ridemate.ACTION_HIDE_OVERLAY"
    }
}