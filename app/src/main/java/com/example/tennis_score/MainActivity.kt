package com.example.tennis_score

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var scoreViews: Array<Array<TextView>> // 2行3列
    private lateinit var ledFont: Typeface
    private lateinit var playerSignViews: Array<ImageView>
    private lateinit var signatureFilled: Array<Boolean> // サイン済みフラグ
    private var previousScores: Array<Array<String>>? = null
    private var undoAvailable = false
    private lateinit var undoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // フォント読み込み
        ledFont = try {
            Typeface.createFromAsset(assets, "digital-7.ttf")
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }

        signatureFilled = Array(2) { false } // サイン済みフラグ初期化

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // --- ヘッダー上の余白スペース ---
        val topSpacer = TextView(this).apply { text = " "; textSize = 12f }
        root.addView(topSpacer)

        // --- ヘッダー ---
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createBorderDrawable()
        }

        // --- 初期化ボタン ---
        val resetButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_reset) // restart_alt.xml を res/drawable に追加
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(16, 16, 16, 16)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("確認")
                    .setMessage("ゲームを初期化しますか？")
                    .setPositiveButton("OK") { _, _ ->
                        resetAll()
                        undoButton.isEnabled = false
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }
        }

        // --- 一つ前に戻すボタン ---
        undoButton = createBorderedButton("一つ前に戻す") {
            undoScores()
            undoButton.isEnabled = false
        }
        undoButton.isEnabled = false

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4,4,4,4)
            addView(resetButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f))
            addView(undoButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f))
            background = createBorderDrawable()
        }

        headerLayout.addView(buttonLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // --- ヘッダー数字 ---
        val headerTitles = arrayOf("Ⅰ", "Ⅱ", "Ⅲ")
        headerTitles.forEach { title ->
            val tv = createBorderedTextView(title, 48f)
            headerLayout.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        root.addView(headerLayout)

        // --- スコアボード ---
        val scoresLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            background = createBorderDrawable()
        }

        scoreViews = Array(2) { row -> Array(3) { col ->
            if (col == 1 || col == 2) createScoreTextView("") else createScoreTextView("0") // 2,3列は空欄
        } }

        playerSignViews = Array(2) { row ->
            ImageView(this).apply {
                setBackgroundColor(Color.BLACK)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageBitmap(createDefaultNameBitmap())
                setOnClickListener { showSignatureDialog(this, row) } // row番号渡す
            }
        }

        for (row in 0..1) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                background = createBorderDrawable()
            }

            // 左側：サイン
            rowLayout.addView(playerSignViews[row], LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            // 右側：スコア
            for (col in 0..2) {
                rowLayout.addView(scoreViews[row][col], LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }

            scoresLayout.addView(rowLayout)
        }

        root.addView(scoresLayout)
        setContentView(root)
    }

    // -------------------------
    // ヘルパー関数
    // -------------------------
    private fun createBorderedTextView(text: String, textSize: Float): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            setTextColor(Color.GREEN)
            typeface = ledFont
            gravity = Gravity.CENTER
            background = createBorderDrawable()
        }
    }

    private fun createBorderedButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun createBorderDrawable(): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            override fun draw(canvas: Canvas) {
                canvas.drawRect(bounds, paint)
            }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }

    // --- スコアTextView ---
    private fun createScoreTextView(initial: String = "0"): TextView {
        return object : TextView(this) {
            private val paintOval = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111111")
                style = Paint.Style.FILL
            }
            private val paintNoise = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(30, 255, 255, 255)
            }

            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f
                val cy = height / 2f
                val radiusX = width * 0.4f
                val radiusY = height * 0.4f
                val rect = RectF(cx - radiusX, cy - radiusY, cx + radiusX, cy + radiusY)
                canvas.drawOval(rect, paintOval)

                val noiseCount = 200
                for (i in 0 until noiseCount) {
                    val x = Random.nextFloat() * width
                    val y = Random.nextFloat() * height
                    canvas.drawPoint(x, y, paintNoise)
                }

                // 文字描画
                val text = this.text.toString()
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GREEN
                    textSize = 1000f
                    typeface = ledFont
                    textAlign = Paint.Align.CENTER
                }

                val fm = textPaint.fontMetrics
                val offset = height * 0.17f
                val yPos = cy - (fm.ascent + fm.descent) / 2 - offset
                canvas.drawText(text, cx, yPos, textPaint)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                val cx = width / 2f
                val cy = height / 2f
                val radiusX = width * 0.4f
                val radiusY = height * 0.4f
                val x = event.x
                val y = event.y
                val insideEllipse = ((x - cx) * (x - cx)) / (radiusX * radiusX) +
                        ((y - cy) * (y - cy)) / (radiusY * radiusY) <= 1f

                if (!insideEllipse) return false

                if (event.action == MotionEvent.ACTION_UP) {
                    savePreviousScores()
                    val current = text.toString()
                    if (current.isEmpty()) {
                        text = "0"
                    } else {
                        val value = current.toInt()
                        text = if (value >= 6) "" else (value + 1).toString()
                    }
                    undoButton.isEnabled = true
                }
                return true
            }
        }.apply {
            text = initial
            setTextColor(Color.GREEN)
            textSize = 480f
            typeface = ledFont
            gravity = Gravity.CENTER
            background = createBorderDrawable()
        }
    }

    private fun savePreviousScores() {
        previousScores = Array(2) { row -> Array(3) { col ->
            scoreViews[row][col].text.toString()
        } }
        undoAvailable = true
    }

    private fun undoScores() {
        previousScores?.let {
            for (row in 0..1) {
                for (col in 0..2) {
                    scoreViews[row][col].text = it[row][col]
                }
            }
        }
        undoAvailable = false
    }

    private fun resetAll() {
        for (row in 0..1) {
            for (col in 0..2) {
                scoreViews[row][col].text = if (col == 1 || col == 2) "" else "0"
            }
        }
        for (row in 0..1) {
            playerSignViews[row].setImageBitmap(createDefaultNameBitmap())
            signatureFilled[row] = false
        }
        previousScores = null
        undoAvailable = false
    }

    private fun createDefaultNameBitmap(): Bitmap {
        val width = 600
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint().apply {
            color = Color.GREEN
            textSize = 48f
            typeface = ledFont
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val lines = arrayOf("タップして", "名前を入れてください")
        val fm = paint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * 3f
        val totalHeight = lineHeight * lines.size
        var y = height / 2f - totalHeight / 2f - fm.ascent

        for (line in lines) {
            canvas.drawText(line, width / 2f, y, paint)
            y += lineHeight
        }

        return bitmap
    }

    private fun showSignatureDialog(target: ImageView, row: Int) {
        if (signatureFilled[row]) {
            // サイン済みの場合、確認アラート
            val currentSignature = target.drawable

            val imageView = ImageView(this).apply {
                setImageDrawable(currentSignature)
                layoutParams = LinearLayout.LayoutParams(300, 100)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                addView(TextView(this@MainActivity).apply {
                    text = "サインはすでに記入されています。\n変更しますか？"
                    setTextColor(Color.BLACK)
                    textSize = 16f
                    gravity = Gravity.CENTER
                })
                addView(imageView)
            }

            AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("変更する") { _, _ ->
                    openSignatureInput(target, row)
                }
                .setNegativeButton("戻る", null)
                .show()
        } else {
            openSignatureInput(target, row)
        }
    }


    private fun openSignatureInput(target: ImageView, row: Int) {
        val sigView = SignatureView(this).apply {
            layoutParams = LinearLayout.LayoutParams(800, 400)
            setBackgroundColor(Color.WHITE)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("あなたの名前をサインしてください")
            .setView(sigView)
            .setPositiveButton("OK") { _, _ ->
                val bmp = sigView.getBitmap()
                val styledBmp = convertSignatureToScoreStyle(bmp)
                target.setImageBitmap(styledBmp)
                signatureFilled[row] = true
            }
            .setNegativeButton("戻る") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setNeutralButton("クリア", null) // とりあえず null
            .create()

        dialog.show()

        // クリアボタンの挙動を上書きする
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            sigView.clear() // ここで SignatureView をクリアする
        }
    }

    private fun convertSignatureToScoreStyle(signature: Bitmap): Bitmap {
        val width = signature.width
        val height = signature.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawColor(Color.BLACK)

        val pixels = IntArray(width * height)
        signature.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = Color.alpha(color)
            if (color != Color.WHITE) {
                pixels[i] = Color.argb(alpha, 0, 255, 0)
            } else {
                pixels[i] = Color.BLACK
            }
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height)

        return result
    }
}

// --- 手書き入力ビュー ---
class SignatureView(context: Context) : android.view.View(context) {
    private val path = Path()
    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> path.moveTo(x, y)
            MotionEvent.ACTION_MOVE -> path.lineTo(x, y)
            MotionEvent.ACTION_UP -> {}
        }
        invalidate()
        return true
    }

    fun clear() {
        path.reset()
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        draw(c)
        return b
    }
}
