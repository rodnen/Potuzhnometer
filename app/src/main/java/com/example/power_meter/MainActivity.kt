package com.example.power_meter

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private val barViews = mutableListOf<View>()
    private val numberOfBars = 22;
    private val cornerRadius = 16f;
    private val notActiveColor = "#454545"
    private val colors = arrayOf("#13FE01", "#2FFB0B","#44F913", "#58F71A",
        "#6CF422", "#81F129", "#93EF30", "#A9EC38",
        "#BFEA40", "#D3E748", "#E6E54F", "#FBE256",
        "#FECE50", "#FEBA48", "#FFA33F", "#FF8D37",
        "#FF772E", "#FF6126", "#FF4B1D", "#FF3414",
        "#FF200C", "#FF0000")
    private val btnNames = arrayOf("×1.0", "×1.5", "×2.0")
    private val potuzhnaSpeedI = arrayOf(100L, 60L, 20L)
    private val potuzhnaSpeedD = arrayOf(150L, 100L, 50L)

    private var counter = 0
    private var maxCounter = 100
    private var minCounter = 0
    private var sencetivityType = 0

    private var isSoundPlaying = false
    private var isPressed = false
    private var potuzhno = false

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Зміна кольору статус бара та бара навігації
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)

        //Ініціалізвація
        val relativeLayout = findViewById<RelativeLayout>(R.id.content)
        val barsLayout = findViewById<LinearLayout>(R.id.bars_layout)
        val screenLayout = findViewById<RelativeLayout>(R.id.main_screen)
        val powerText = findViewById<TextView>(R.id.power_number)
        val sencetivityButton = findViewById<TextView>(R.id.sencetivity)
        val handler = Handler(Looper.getMainLooper())

        var mediaPlayer: MediaPlayer? = null

        val borderColor = "#262626"
        val cardsList = arrayOf(findViewById<CardView>(R.id.cardView),findViewById<CardView>(R.id.cardView_load), findViewById<CardView>(R.id.cardView_number))

        for(element in cardsList){
            element.setCardBackgroundColor(borderColor.toColorInt())
        }

        //Створення мсмужки завантаження
        fun createRoundedView(context: Context, cornerRadius: Float, color: Int, index: Int): View {
            val view = View(context)

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                this.cornerRadius = cornerRadius
            }

            view.background = drawable
            // Отримуємо висоту контейнера
            val container: View = findViewById(R.id.bars_layout)

            // Використовуємо container.post, щоб отримати висоту контейнера після його відображення
            container.post {
                val containerHeight = container.height
                // Обчислюємо мінімальну і максимальну висоту
                val minHeight = containerHeight / 3 // мінімальна висота, наприклад, 1/3 висоти контейнера
                val maxHeight = containerHeight   // максимальна висота, наприклад, 1/1 висоти контейнера

                // Визначаємо висоту елемента залежно від індексу
                val heightDp = minHeight + ((maxHeight - minHeight) * index / (numberOfBars - 1))

                // Оновлюємо LayoutParams для цього виду
                view.layoutParams = LinearLayout.LayoutParams(0, heightDp.toInt(), 1f).apply {
                    setMargins(6, 0, 6, 0)
                }
            }

            // Повертаємо вью
            return view
        }

        for(i in 0 until numberOfBars){
            val bar = createRoundedView(this, cornerRadius, notActiveColor.toColorInt(), i)

            barViews.add(bar)
            barsLayout.addView(bar)

        }

        //Подія натискання

        fun updateBarColors(counter: Int, maxCounter: Int) {
            val filledBars = (counter.toFloat() / maxCounter * barViews.size).toInt()

            for ((index, view) in barViews.withIndex()) {
                val drawable = view.background as GradientDrawable
                if (index < filledBars) {
                    drawable.setColor(colors[index].toColorInt())
                } else {
                    drawable.setColor(notActiveColor.toColorInt())
                }
            }
        }

        val color1 = "#EE4848".toColorInt()
        val color2 = "#630808".toColorInt()

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), color1, color2)
        colorAnimation.duration = 500
        colorAnimation.repeatCount = ValueAnimator.INFINITE
        colorAnimation.repeatMode = ValueAnimator.REVERSE

        colorAnimation.addUpdateListener { animator ->
            screenLayout.setBackgroundColor(animator.animatedValue as Int)
        }

        val decrementRunnable = object : Runnable {
            override fun run() {
                if (!isPressed && counter > minCounter && !isSoundPlaying) {
                    if(potuzhno) potuzhno = false

                    colorAnimation.cancel()

                    // Зупиняємо музику
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null

                    powerText.setTextColor("#FFFFFF".toColorInt())
                    screenLayout.setBackgroundColor("#000000".toColorInt())

                    counter--
                    powerText.text = counter.toString()
                    updateBarColors(counter, maxCounter)
                    handler.postDelayed(this, potuzhnaSpeedD[sencetivityType])
                }
                else{
                    handler.removeCallbacks(this)
                }
            }
        }

        val incrementRunnable = object : Runnable {
            override fun run() {
                if (isPressed && counter < maxCounter) {
                    counter++
                    powerText.text = counter.toString()
                    updateBarColors(counter, maxCounter)
                    handler.postDelayed(this, potuzhnaSpeedI[sencetivityType])
                }
                else{
                    if(!potuzhno) {
                        potuzhno = true
                        isSoundPlaying = true
                        powerText.setTextColor("#EE4848".toColorInt())
                        colorAnimation.start()

                        // Створюємо Runnable для відтворення звуку
                        val soundRunnable = object : Runnable {
                            override fun run() {
                                // Створення та програвання звуку
                                if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
                                    mediaPlayer?.release() // На всякий випадок
                                    mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.sound)
                                    mediaPlayer?.start()
                                    mediaPlayer?.isLooping = true // Якщо треба, щоб грав в циклі
                                }

                                // Виключити обробку з `decrementRunnable` на 3 секунди
                                handler.removeCallbacks(decrementRunnable)

                                // Запускаємо таймер для поновлення
                                handler.postDelayed({
                                    // Після 3 секунд знову включаємо обробку decrementRunnable
                                    isSoundPlaying = false
                                    handler.post(decrementRunnable)
                                }, 9000) // 9000 мс = 9 секунди
                            }
                        }

                        // Запускаємо звук
                        handler.post(soundRunnable)
                    }
                    handler.removeCallbacks(this)
                }
            }
        }

        sencetivityButton.setOnClickListener {
            if (sencetivityType < 2) sencetivityType++ else sencetivityType = 0
            sencetivityButton.text = btnNames[sencetivityType]
        }

        screenLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isPressed = true
                    handler.removeCallbacks(decrementRunnable)
                    handler.post(incrementRunnable)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPressed = false
                    handler.removeCallbacks(incrementRunnable)
                    handler.post(decrementRunnable)
                    true
                }

                else -> false
            }
        }

    }
}