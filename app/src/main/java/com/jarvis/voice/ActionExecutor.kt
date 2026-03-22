package com.jarvis.voice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.telephony.SmsManager
import android.widget.Toast
import org.json.JSONObject
import java.util.Calendar

object ActionExecutor {
    fun execute(context: Context, response: String): String {
        val jsonStr = extractJson(response.trim()) ?: return response
        return try {
            val json = JSONObject(jsonStr)
            val action = json.optString("action", "")
            val params = json.optJSONObject("params") ?: JSONObject()
            val reply = json.optString("reply", "Done!")
            when (action) {
                "OPEN_APP" -> openApp(context, params, reply)
                "SEND_WHATSAPP" -> sendWhatsApp(context, params, reply)
                "SEND_SMS" -> sendSms(context, params, reply)
                "SET_ALARM" -> setAlarm(context, params, reply)
                "SET_TIMER" -> setTimer(context, params, reply)
                "HOME" -> goHome(context, reply)
                "BACK" -> { JarvisAccessibilityService.instance?.goBack(); reply }
                "SCROLL_DOWN" -> { JarvisAccessibilityService.instance?.scrollDown(); reply }
                "SCROLL_UP" -> { JarvisAccessibilityService.instance?.scrollUp(); reply }
                "CLICK" -> { JarvisAccessibilityService.instance?.clickOnText(params.optString("text")); reply }
                "TYPE_TEXT" -> { JarvisAccessibilityService.instance?.typeText(params.optString("text")); reply }
                else -> response
            }
        } catch (e: Exception) { response }
    }
    private fun extractJson(text: String): String? {
        val s = text.indexOf('{'); val e = text.lastIndexOf('}')
        if (s == -1 || e == -1 || e <= s) return null
        return try { val c = text.substring(s, e+1); JSONObject(c); c } catch (e: Exception) { null }
    }
    private fun openApp(context: Context, params: JSONObject, reply: String): String {
        val pkg = params.optString("package", ""); val name = params.optString("appName", "")
        return try {
            val i = if (pkg.isNotBlank()) context.packageManager.getLaunchIntentForPackage(pkg)
            else context.packageManager.getInstalledApplications(0).firstOrNull{ context.packageManager.getApplicationLabel(it).toString().lowercase().contains(name.lowercase()) }?.let{ context.packageManager.getLaunchIntentForPackage(it.packageName) }
            i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if (i != null) { context.startActivity(i); reply } else "❌ App not found"
        } catch (e: Exception) { "❌ ${e.message}" }
    }
    private fun sendWhatsApp(context: Context, params: JSONObject, reply: String): String {
        return try { context.startActivity(Intent(Intent.ACTION_VIEW).apply{ data=android.net.Uri.parse("https://api.whatsapp.com/send?phone=${params.optString("contact")}&text=${android.net.Uri.encode(params.optString("message"))}"); `package`="com.whatsapp"; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); reply } catch(e:Exception){ "❌ ${e.message}" }
    }
    private fun sendSms(context: Context, params: JSONObject, reply: String): String {
        return try { SmsManager.getDefault().sendTextMessage(params.optString("number"), null, params.optString("message"), null, null); reply } catch(e:Exception){ "❌ ${e.message}" }
    }
    private fun setAlarm(context: Context, params: JSONObject, reply: String): String {
        return try { context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply{ putExtra(AlarmClock.EXTRA_HOUR,params.optInt("hour",7)); putExtra(AlarmClock.EXTRA_MINUTES,params.optInt("minute",0)); putExtra(AlarmClock.EXTRA_MESSAGE,params.optString("label","Jarvis")); putExtra(AlarmClock.EXTRA_SKIP_UI,true); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); reply } catch(e:Exception){ "❌ ${e.message}" }
    }
    private fun setTimer(context: Context, params: JSONObject, reply: String): String {
        return try { context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply{ putExtra(AlarmClock.EXTRA_LENGTH,params.optInt("seconds",60)); putExtra(AlarmClock.EXTRA_SKIP_UI,true); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); reply } catch(e:Exception){ "❌ ${e.message}" }
    }
    private fun goHome(context: Context, reply: String): String {
        context.startActivity(Intent(Intent.ACTION_MAIN).apply{ addCategory(Intent.CATEGORY_HOME); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }); return reply
    }
}
