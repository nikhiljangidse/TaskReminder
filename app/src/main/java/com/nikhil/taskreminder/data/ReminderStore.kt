package com.nikhil.taskreminder.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** Saves reminders as JSON in SharedPreferences and exposes them as a StateFlow for the UI. */
object ReminderStore {
    private const val PREFS = "task_reminders"
    private const val KEY_ITEMS = "items"
    private const val KEY_NEXT_ID = "next_id"

    private val _items = MutableStateFlow<List<Reminder>>(emptyList())
    val items: StateFlow<List<Reminder>> = _items.asStateFlow()

    private var prefs: SharedPreferences? = null

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _items.value = parse(p.getString(KEY_ITEMS, null))
    }

    fun all(context: Context): List<Reminder> {
        init(context)
        return _items.value
    }

    fun get(context: Context, id: Int): Reminder? = all(context).firstOrNull { it.id == id }

    @Synchronized
    fun newId(context: Context): Int {
        init(context)
        val p = prefs!!
        val id = p.getInt(KEY_NEXT_ID, 1)
        p.edit().putInt(KEY_NEXT_ID, id + 1).commit()
        return id
    }

    @Synchronized
    fun upsert(context: Context, reminder: Reminder) {
        init(context)
        val list = _items.value.toMutableList()
        val index = list.indexOfFirst { it.id == reminder.id }
        if (index >= 0) list[index] = reminder else list.add(reminder)
        save(list)
    }

    @Synchronized
    fun delete(context: Context, id: Int) {
        init(context)
        save(_items.value.filterNot { it.id == id })
    }

    private fun save(list: List<Reminder>) {
        _items.value = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs!!.edit().putString(KEY_ITEMS, arr.toString()).commit()
    }

    private fun parse(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Reminder.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
