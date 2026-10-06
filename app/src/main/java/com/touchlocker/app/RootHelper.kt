package com.touchlocker.app

import java.io.BufferedReader

/**
 * Disables / re-enables the touchscreen at the kernel input-device level,
 * using root. This is the only way to block touch system-wide (on every
 * screen, every app, the status bar, everything) without locking the
 * device into a single app — there is no public Android API that does
 * this, Device Owner included.
 *
 * How it works: every input device (touchscreen, volume keys, etc.) has a
 * node under /dev/input/eventN. The system's input service reads touch
 * events from the touchscreen's node. If that node's permissions are
 * stripped (chmod 000), the input service can no longer read it and touch
 * events stop reaching the system entirely — while every other input
 * device (volume, power) is untouched since only the touchscreen's node
 * is modified. Restoring the original permissions re-enables it instantly.
 *
 * A reboot always restores default permissions on its own (the kernel /
 * ueventd recreate the node fresh at boot), so this can never "brick"
 * touch permanently.
 */
object RootHelper {

    data class Result(val success: Boolean, val message: String)
    data class TouchNode(val devicePath: String, val originalPerm: String)

    private fun runAsRoot(command: String): Triple<Int, String, String> {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val stdout = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val stderr = process.errorStream.bufferedReader().use(BufferedReader::readText)
        val exit = process.waitFor()
        return Triple(exit, stdout.trim(), stderr.trim())
    }

    /** Quick check whether a working root shell (su) is available at all. */
    fun isRootAvailable(): Boolean {
        return try {
            val (exit, _, _) = runAsRoot("id")
            exit == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Scans /proc/bus/input/devices via `getevent -il` for the device that
     * exposes multitouch position axes (ABS_MT_POSITION_X / _Y) — the
     * standard signature of a touchscreen — and returns its /dev/input/eventN
     * path plus its current (pre-lock) permission string, e.g. "660".
     * Must be called from a background thread.
     */
    fun findTouchscreenNode(): TouchNode? {
        return try {
            val (exit, stdout, _) = runAsRoot("getevent -il")
            if (exit != 0 || stdout.isBlank()) return null

            // getevent -il prints one block per device, each starting with
            // a line like: "add device 3: /dev/input/event4" followed by
            // "  name: ..." and capability lines. We want the block that
            // mentions ABS_MT_POSITION_X.
            val blocks = stdout.split(Regex("(?=add device)"))
            val touchBlock = blocks.firstOrNull { it.contains("ABS_MT_POSITION_X") }
                ?: return null

            val devicePath = Regex("""(/dev/input/event\d+)""")
                .find(touchBlock)?.groupValues?.get(1) ?: return null

            val (permExit, permOut, _) = runAsRoot("stat -c '%a' $devicePath")
            val perm = if (permExit == 0 && permOut.isNotBlank()) permOut else "660"

            TouchNode(devicePath, perm)
        } catch (e: Exception) {
            null
        }
    }

    /** Strips all permissions from the touchscreen node, blocking touch system-wide. */
    fun disableTouch(node: TouchNode): Result {
        return try {
            val (exit, _, stderr) = runAsRoot("chmod 000 ${node.devicePath}")
            if (exit == 0) {
                Result(true, "הטאץ' נחסם.")
            } else {
                Result(false, "נכשל לחסום את הטאץ': $stderr")
            }
        } catch (e: Exception) {
            Result(false, "שגיאה: ${e.message}")
        }
    }

    /** Restores the touchscreen node's original permissions, re-enabling touch. */
    fun enableTouch(node: TouchNode): Result {
        return try {
            val (exit, _, stderr) = runAsRoot("chmod ${node.originalPerm} ${node.devicePath}")
            if (exit == 0) {
                Result(true, "הטאץ' שוחרר.")
            } else {
                Result(false, "נכשל לשחרר את הטאץ': $stderr")
            }
        } catch (e: Exception) {
            Result(false, "שגיאה: ${e.message}")
        }
    }
}
