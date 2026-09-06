package com.example.studentprofileapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_THEME = "theme_prefs"
        private const val KEY_NIGHT_MODE = "key_night_mode"
        private const val KEY_RESULT_COLOR_RES = "key_result_color_res"

        private var previousThemeBitmap: Bitmap? = null
        private var revealCenterX: Int = 0
        private var revealCenterY: Int = 0
    }

    // UI references
    private lateinit var tilName: TextInputLayout
    private lateinit var tilStudentId: TextInputLayout
    private lateinit var tilCourse: TextInputLayout
    private lateinit var tilYear: TextInputLayout
    private lateinit var tilHometown: TextInputLayout

    private lateinit var etName: EditText
    private lateinit var etStudentId: EditText
    private lateinit var etCourse: EditText
    private lateinit var etYear: EditText
    private lateinit var etHometown: EditText

    private lateinit var btnThemeToggle: ImageButton
    private lateinit var tvResult: TextView
    private var resultTextColorRes: Int = R.color.textPlaceholder

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme mode if not already active
        val prefs = getSharedPreferences(PREFS_THEME, MODE_PRIVATE)
        val savedMode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (AppCompatDelegate.getDefaultNightMode() != savedMode) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainView = findViewById<View>(R.id.main)

        // Keep content clear of the status bar, nav bar, and keyboard
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }

        bindViews()
        updateThemeToggleIcon()

        btnThemeToggle.setOnClickListener {
            toggleTheme()
        }

        // Restore result text color across configuration/theme changes
        if (savedInstanceState != null) {
            resultTextColorRes = savedInstanceState.getInt(KEY_RESULT_COLOR_RES, R.color.textPlaceholder)
            tvResult.setTextColor(ContextCompat.getColor(this, resultTextColorRes))
        }

        // Typing in a field clears that field's inline error
        wireAutoClearError(tilName, etName)
        wireAutoClearError(tilStudentId, etStudentId)
        wireAutoClearError(tilCourse, etCourse)
        wireAutoClearError(tilYear, etYear)
        wireAutoClearError(tilHometown, etHometown)

        findViewById<View>(R.id.btnGenerate).setOnClickListener { generateProfile() }
        findViewById<View>(R.id.btnClear).setOnClickListener { clearForm() }

        // Execute smooth circular reveal animation if transitioning between themes
        checkAndPlayThemeTransition(mainView)
    }

    private fun bindViews() {
        tilName = findViewById(R.id.tilName)
        tilStudentId = findViewById(R.id.tilStudentId)
        tilCourse = findViewById(R.id.tilCourse)
        tilYear = findViewById(R.id.tilYear)
        tilHometown = findViewById(R.id.tilHometown)

        etName = findViewById(R.id.etName)
        etStudentId = findViewById(R.id.etStudentId)
        etCourse = findViewById(R.id.etCourse)
        etYear = findViewById(R.id.etYear)
        etHometown = findViewById(R.id.etHometown)

        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        tvResult = findViewById(R.id.tvResult)
    }

    private fun isDarkThemeActive(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateThemeToggleIcon() {
        if (isDarkThemeActive()) {
            btnThemeToggle.setImageResource(R.drawable.ic_sun)
            btnThemeToggle.contentDescription = getString(R.string.cd_toggle_theme)
        } else {
            btnThemeToggle.setImageResource(R.drawable.ic_moon)
            btnThemeToggle.contentDescription = getString(R.string.cd_toggle_theme)
        }
    }

    private fun toggleTheme() {
        val isDark = isDarkThemeActive()
        val targetMode = if (isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES

        // Capture center of toggle button relative to the main view
        val mainView = findViewById<View>(R.id.main)
        val btnLoc = IntArray(2)
        val mainLoc = IntArray(2)
        btnThemeToggle.getLocationInWindow(btnLoc)
        mainView.getLocationInWindow(mainLoc)
        revealCenterX = (btnLoc[0] - mainLoc[0]) + btnThemeToggle.width / 2
        revealCenterY = (btnLoc[1] - mainLoc[1]) + btnThemeToggle.height / 2

        // Capture screenshot of current screen
        previousThemeBitmap = captureScreenBitmap()

        // Persist preference
        getSharedPreferences(PREFS_THEME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, targetMode)
            .apply()

        // Apply new night mode or recreate
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
        } else {
            recreate()
        }
    }

    private fun checkAndPlayThemeTransition(mainView: View) {
        val oldBitmap = previousThemeBitmap ?: return
        val cx = revealCenterX
        val cy = revealCenterY
        previousThemeBitmap = null // Consume immediately

        val contentViewGroup = findViewById<ViewGroup>(android.R.id.content) ?: run {
            oldBitmap.recycle()
            return
        }

        val oldImageView = ImageView(this).apply {
            setImageBitmap(oldBitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Place the previous theme screenshot behind the new layout
        contentViewGroup.addView(oldImageView, 0)
        // Make the new view temporarily invisible until the circular reveal starts
        mainView.visibility = View.INVISIBLE

        mainView.post {
            if (isFinishing || isDestroyed) {
                contentViewGroup.removeView(oldImageView)
                if (!oldBitmap.isRecycled) oldBitmap.recycle()
                return@post
            }

            try {
                val width = mainView.width
                val height = mainView.height
                val maxRadius = kotlin.math.hypot(
                    maxOf(cx, width - cx).toDouble(),
                    maxOf(cy, height - cy).toDouble()
                ).toFloat()

                mainView.visibility = View.VISIBLE
                val anim = ViewAnimationUtils.createCircularReveal(mainView, cx, cy, 0f, maxRadius).apply {
                    duration = 500L
                    interpolator = AccelerateDecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            contentViewGroup.removeView(oldImageView)
                            if (!oldBitmap.isRecycled) oldBitmap.recycle()
                        }
                    })
                }

                // Smooth rotation of the toggle icon to complement the reveal wave
                btnThemeToggle.rotation = -90f
                btnThemeToggle.animate()
                    .rotation(0f)
                    .setDuration(500L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                anim.start()
            } catch (_: Throwable) {
                mainView.visibility = View.VISIBLE
                contentViewGroup.removeView(oldImageView)
                if (!oldBitmap.isRecycled) oldBitmap.recycle()
            }
        }
    }

    private fun captureScreenBitmap(): Bitmap? {
        return try {
            val root = window.decorView
            if (root.width <= 0 || root.height <= 0) return null
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            root.draw(canvas)
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_RESULT_COLOR_RES, resultTextColorRes)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            previousThemeBitmap?.let {
                if (!it.isRecycled) it.recycle()
                previousThemeBitmap = null
            }
        }
    }

    /**
     * Reads every field, validates, and either shows the profile
     * generated by the Java ProfileGenerator class or a validation message.
     */
    private fun generateProfile() {
        clearFieldErrors()

        val fields = listOf(
            tilName to etName,
            tilStudentId to etStudentId,
            tilCourse to etCourse,
            tilYear to etYear,
            tilHometown to etHometown
        )
        val values = fields.associate { it.second to it.second.text.toString().trim() }
        val emptyFields = fields.filter { values[it.second].isNullOrEmpty() }

        if (emptyFields.isNotEmpty()) {
            // Highlight each missing field and show a validation message
            emptyFields.forEach { it.first.error = getString(R.string.error_required) }
            emptyFields.first().second.requestFocus()

            resultTextColorRes = R.color.colorError
            tvResult.setTextColor(ContextCompat.getColor(this, resultTextColorRes))
            tvResult.text = getString(R.string.validation_message)
            return
        }

        // All fields complete -> let the Java class build the profile
        val generator = ProfileGenerator()
        val profile = generator.generateProfile(
            values.getValue(etName),
            values.getValue(etStudentId),
            values.getValue(etCourse),
            values.getValue(etYear),
            values.getValue(etHometown)
        )

        resultTextColorRes = R.color.textDefault
        tvResult.setTextColor(ContextCompat.getColor(this, resultTextColorRes))
        tvResult.text = profile
    }

    /** Clears all inputs and resets the result to its default message. */
    private fun clearForm() {
        clearFieldErrors()
        etName.text.clear()
        etStudentId.text.clear()
        etCourse.text.clear()
        etYear.text.clear()
        etHometown.text.clear()

        resultTextColorRes = R.color.textPlaceholder
        tvResult.setTextColor(ContextCompat.getColor(this, resultTextColorRes))
        tvResult.setText(R.string.result_placeholder)
    }

    private fun clearFieldErrors() {
        listOf(tilName, tilStudentId, tilCourse, tilYear, tilHometown)
            .forEach { it.error = null }
    }

    private fun wireAutoClearError(til: TextInputLayout, et: EditText) {
        et.doAfterTextChanged { til.error = null }
    }
}
