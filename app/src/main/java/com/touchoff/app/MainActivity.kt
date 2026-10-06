package com.touchoff.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var rootLayout: LinearLayout
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AppDeviceAdminReceiver::class.java)

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 60, 50, 60)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(rootLayout)

        if (!PasswordManager.isPasswordSet(this)) {
            showSetInitialPasswordScreen()
        } else {
            showControlPanel()
        }
    }

    override fun onResume() {
        super.onResume()
        if (PasswordManager.isPasswordSet(this)) {
            showControlPanel()
        }
    }

    // ---------- Initial password setup ----------

    private fun showSetInitialPasswordScreen() {
        rootLayout.removeAllViews()
        addLabel("ברוכים הבאים ל-Touch Locker\nהגדר סיסמה להגנת ההגדרות (לפחות 4 תווים)", 19f)

        val pwdField = passwordField("סיסמה חדשה")
        val saveBtn = Button(this).apply {
            text = "שמור"
            setOnClickListener {
                val pwd = pwdField.text.toString()
                if (pwd.length < 4) {
                    toast("סיסמה קצרה מדי")
                    return@setOnClickListener
                }
                PasswordManager.setPassword(this@MainActivity, pwd)
                showControlPanel()
            }
        }
        rootLayout.addView(pwdField)
        rootLayout.addView(saveBtn)
    }

    // ---------- Main control panel ----------

    private fun showControlPanel() {
        if (TouchLockState.isLocked(this)) {
            showLockedScreen()
        } else {
            showUnlockedPanel()
        }
    }

    /**
     * Shown while touch is disabled. Since there is no touch-based way to
     * leave this screen anyway (even gesture navigation needs touch), the
     * device simply stays frozen here — exactly one focused button, reachable
     * with the device's physical D-pad/keypad "OK" key (KEYCODE_DPAD_CENTER
     * / KEYCODE_ENTER), leading to a password prompt.
     */
    private fun showLockedScreen() {
        rootLayout.removeAllViews()
        addLabel("TouchOff", 22f)
        addLabel("הטאץ' נעול 🔒", 18f)
        addLabel("לחץ \"אישור\" במקלדת כדי להזין סיסמה ולשחרר", 14f)

        val unlockBtn = Button(this).apply {
            text = "שחרר נעילה"
            setOnClickListener { promptPasswordThen { unlockTouch(this) } }
        }
        rootLayout.addView(unlockBtn)
        unlockBtn.requestFocus()
    }

    private fun showUnlockedPanel() {
        rootLayout.removeAllViews()
        addLabel("TouchOff", 22f)
        addLabel("סטטוס: הטאץ' פעיל 🔓", 17f)
        addLabel("Reboot תמיד משחזר את הטאץ' לפעולה לבד.", 13f)

        val rootCheckBtn = Button(this).apply {
            text = "בדוק הרשאות Root"
            setOnClickListener { checkRoot(this) }
        }

        val lockBtn = Button(this).apply {
            text = "נעל טאץ' עכשיו"
            setOnClickListener { lockTouch(this) }
        }

        val adminActive = dpm.isAdminActive(adminComponent)
        val deviceAdminBtn = Button(this).apply {
            text = if (adminActive) "בטל הגנת הסרה (Device Admin)" else "הפעל הגנת הסרה (Device Admin, אופציונלי)"
            setOnClickListener {
                if (adminActive) {
                    promptPasswordThen { removeDeviceAdmin() }
                } else {
                    requestDeviceAdmin()
                }
            }
        }

        val changePwdBtn = Button(this).apply {
            text = "שנה סיסמה"
            setOnClickListener { promptPasswordThen { showChangePasswordScreen() } }
        }

        val debugBtn = Button(this).apply {
            text = "דיבאג: סרוק מכשירי קלט"
            setOnClickListener { runDebugScan(this) }
        }

        rootLayout.addView(rootCheckBtn)
        rootLayout.addView(lockBtn)
        rootLayout.addView(deviceAdminBtn)
        rootLayout.addView(changePwdBtn)
        rootLayout.addView(debugBtn)
        rootCheckBtn.requestFocus()
    }

    private fun runDebugScan(triggerButton: Button) {
        triggerButton.isEnabled = false
        triggerButton.text = "סורק..."
        Thread {
            val result = RootHelper.debugScanInputDevices()
            runOnUiThread {
                triggerButton.isEnabled = true
                triggerButton.text = "דיבאג: סרוק מכשירי קלט"

                val scrollableText = TextView(this).apply {
                    text = result.message
                    setPadding(30, 30, 30, 30)
                    textIsSelectable = true
                    textSize = 12f
                }
                val scrollView = android.widget.ScrollView(this).apply { addView(scrollableText) }

                android.app.AlertDialog.Builder(this)
                    .setTitle(if (result.success) "תוצאות הסריקה" else "שגיאה")
                    .setView(scrollView)
                    .setPositiveButton("סגור", null)
                    .show()
            }
        }.start()
    }

    private fun showChangePasswordScreen() {
        rootLayout.removeAllViews()
        addLabel("הזן סיסמה חדשה", 18f)
        val pwdField = passwordField("סיסמה חדשה")
        val saveBtn = Button(this).apply {
            text = "שמור"
            setOnClickListener {
                val pwd = pwdField.text.toString()
                if (pwd.length < 4) {
                    toast("סיסמה קצרה מדי")
                    return@setOnClickListener
                }
                PasswordManager.setPassword(this@MainActivity, pwd)
                toast("הסיסמה עודכנה")
                showControlPanel()
            }
        }
        val cancelBtn = Button(this).apply {
            text = "ביטול"
            setOnClickListener { showControlPanel() }
        }
        rootLayout.addView(pwdField)
        rootLayout.addView(saveBtn)
        rootLayout.addView(cancelBtn)
    }

    // ---------- Actions ----------

    private fun checkRoot(triggerButton: Button) {
        triggerButton.isEnabled = false
        triggerButton.text = "בודק..."
        Thread {
            val available = RootHelper.isRootAvailable()
            runOnUiThread {
                toast(if (available) "✅ גישת Root זמינה" else "❌ אין גישת Root במכשיר")
                triggerButton.isEnabled = true
                triggerButton.text = "2. בדוק הרשאות Root"
            }
        }.start()
    }

    private fun lockTouch(triggerButton: Button) {
        triggerButton.isEnabled = false
        triggerButton.text = "נועל..."
        Thread {
            val node = RootHelper.findTouchscreenNode()
            if (node == null) {
                runOnUiThread {
                    toast("לא נמצא מסך מגע, או שאין הרשאות Root")
                    triggerButton.isEnabled = true
                    triggerButton.text = "נעל טאץ' עכשיו"
                }
                return@Thread
            }
            val result = RootHelper.disableTouch(node)
            runOnUiThread {
                if (result.success) {
                    TouchLockState.saveLocked(this, node.devicePath, node.originalPerm)
                    toast("הטאץ' ננעל.")
                    showControlPanel()
                } else {
                    toast(result.message)
                    triggerButton.isEnabled = true
                    triggerButton.text = "נעל טאץ' עכשיו"
                }
            }
        }.start()
    }

    private fun unlockTouch(triggerButton: Button) {
        val nodePath = TouchLockState.getNodePath(this)
        val perm = TouchLockState.getOriginalPerm(this)
        if (nodePath == null || perm == null) {
            toast("הטאץ' כבר לא נעול")
            showControlPanel()
            return
        }
        triggerButton.isEnabled = false
        triggerButton.text = "משחרר..."
        Thread {
            val result = RootHelper.enableTouch(RootHelper.TouchNode(nodePath, perm))
            runOnUiThread {
                if (result.success) {
                    TouchLockState.clearLocked(this)
                    toast("הטאץ' שוחרר")
                    showControlPanel()
                } else {
                    toast(result.message)
                    triggerButton.isEnabled = true
                    triggerButton.text = "שחרר טאץ' (דורש סיסמה)"
                }
            }
        }.start()
    }

    private fun requestDeviceAdmin() {
        if (dpm.isAdminActive(adminComponent)) {
            toast("הגנת הסרה כבר פעילה")
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "מוסיף אישור נדרש לפני הסרת TouchOff."
            )
        }
        startActivity(intent)
    }

    private fun removeDeviceAdmin() {
        if (!dpm.isAdminActive(adminComponent)) {
            toast("הגנת הסרה כבר לא פעילה")
            showControlPanel()
            return
        }
        dpm.removeActiveAdmin(adminComponent)
        toast("הגנת הסרה בוטלה")
        showControlPanel()
    }

    // ---------- Small UI / helpers ----------

    private fun promptPasswordThen(action: () -> Unit) {
        rootLayout.removeAllViews()
        addLabel("הזן סיסמה", 18f)
        val pwdField = passwordField("סיסמה")
        val confirmBtn = Button(this).apply {
            text = "אישור"
            setOnClickListener {
                if (PasswordManager.checkPassword(this@MainActivity, pwdField.text.toString())) {
                    action()
                } else {
                    toast("סיסמה שגויה")
                }
            }
        }
        val cancelBtn = Button(this).apply {
            text = "ביטול"
            setOnClickListener { showControlPanel() }
        }
        rootLayout.addView(pwdField)
        rootLayout.addView(confirmBtn)
        rootLayout.addView(cancelBtn)
    }

    private fun addLabel(text: String, size: Float) {
        rootLayout.addView(TextView(this).apply {
            this.text = text
            textSize = size
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        })
    }

    private fun passwordField(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun toast(message: String) {
        if (!TextUtils.isEmpty(message)) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
