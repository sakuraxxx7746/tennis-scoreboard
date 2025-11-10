package com.example.tennis_score

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity_ : AppCompatActivity() {

    private lateinit var scoreA: TextView
    private lateinit var scoreB: TextView
    private lateinit var setButton: Button
    private lateinit var undoButton: Button
    private lateinit var resetButton: Button
    private lateinit var nameA: TextView
    private lateinit var nameB: TextView

    private val setResults = Array(3) { Pair(-1, -1) }
    private var currentSetIndex = 0
    private val history = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // --- プレイヤー名行 ---
        val namesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        nameA = TextView(this).apply {
            text = "Player A"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        nameB = TextView(this).apply {
            text = "Player B"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        namesLayout.addView(nameA)
        namesLayout.addView(nameB)
        root.addView(namesLayout)

        // --- 現在のスコア ---
        val scoreLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // 画面の残りスペースをスコアに全部使う
            )
        }

        scoreA = TextView(this).apply {
            text = "0"
            textSize = 200f // 大きくして余白消した分を拡大
            setBackgroundColor(Color.parseColor("#FFD5E5"))
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        scoreB = TextView(this).apply {
            text = "0"
            textSize = 200f
            setBackgroundColor(Color.parseColor("#D5F0FF"))
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        scoreLayout.addView(scoreA)
        scoreLayout.addView(scoreB)
        root.addView(scoreLayout)

        // --- セット結果 ---
        val setLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val setRows = mutableListOf<TextView>()
        for (i in 0 until 3) {
            val resultView = TextView(this).apply {
                text = "-"
                textSize = 32f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 0)
            }
            setLayout.addView(resultView)
            setRows.add(resultView)
        }
        root.addView(setLayout)

        // --- ボタン行 ---
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        setButton = Button(this).apply {
            text = "セット完了"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        undoButton = Button(this).apply {
            text = "巻き戻し"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        buttonLayout.addView(setButton)
        buttonLayout.addView(undoButton)
        root.addView(buttonLayout)

        // --- リセットボタン（中央に配置） ---
        resetButton = Button(this).apply {
            text = "リセット"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = 10
            }
        }
        root.addView(resetButton)

        setContentView(root)

        // --- イベント ---
        scoreA.setOnClickListener { incrementScore(scoreA) }
        scoreB.setOnClickListener { incrementScore(scoreB) }
        setButton.setOnClickListener { completeSet(setRows) }
        undoButton.setOnClickListener { undoAction(setRows) }
        resetButton.setOnClickListener { resetAll(setRows) }

        nameA.setOnClickListener { showNameEditDialog(nameA, "Player A") }
        nameB.setOnClickListener { showNameEditDialog(nameB, "Player B") }
    }

    private fun showNameEditDialog(target: TextView, defaultName: String) {
        val input = EditText(this).apply {
            if (target.text.toString() == defaultName) hint = "名前を入力してください"
            else {
                setText(target.text)
                setSelection(text.length)
            }
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(20))
        }

        AlertDialog.Builder(this)
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString().ifBlank { defaultName }
                target.text = newName
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun incrementScore(scoreView: TextView) {
        val oldValue = scoreView.text.toString().toInt()
        val newValue = (oldValue + 1) % 7
        scoreView.text = newValue.toString()
        history.add { scoreView.text = oldValue.toString() }
    }

    private fun completeSet(setRows: List<TextView>) {
        if (currentSetIndex >= 3) return
        val setIndex = currentSetIndex
        val aScore = scoreA.text.toString().toInt()
        val bScore = scoreB.text.toString().toInt()
        setResults[setIndex] = Pair(aScore, bScore)
        setRows[setIndex].text = "$aScore - $bScore"

        history.add {
            setRows[setIndex].text = "-"
            setResults[setIndex] = Pair(-1, -1)
            currentSetIndex = setIndex
            scoreA.text = aScore.toString()
            scoreB.text = bScore.toString()
            enableInputs(true)
        }

        currentSetIndex++
        scoreA.text = "0"
        scoreB.text = "0"

        if (currentSetIndex >= 3) enableInputs(false)
    }

    private fun undoAction(setRows: List<TextView>) {
        if (history.isNotEmpty()) {
            val last = history.removeAt(history.lastIndex)
            last()
        }
    }

    private fun resetAll(setRows: List<TextView>) {
        val prevResults = setResults.map { it.copy() }
        val prevSetIndex = currentSetIndex
        val prevA = scoreA.text.toString()
        val prevB = scoreB.text.toString()
        val wasEnabled = setButton.isEnabled

        for (i in 0 until 3) {
            setRows[i].text = "-"
            setResults[i] = Pair(-1, -1)
        }
        scoreA.text = "0"
        scoreB.text = "0"
        currentSetIndex = 0
        enableInputs(true)

        history.add {
            for (i in 0 until 3) {
                val (a, b) = prevResults[i]
                setRows[i].text = if (a >= 0) "$a - $b" else "-"
                setResults[i] = prevResults[i]
            }
            scoreA.text = prevA
            scoreB.text = prevB
            currentSetIndex = prevSetIndex
            enableInputs(wasEnabled)
        }
    }

    private fun enableInputs(enabled: Boolean) {
        scoreA.isEnabled = enabled
        scoreB.isEnabled = enabled
        setButton.isEnabled = enabled
    }
}