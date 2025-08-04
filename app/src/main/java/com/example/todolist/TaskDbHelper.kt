package com.example.todolist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "TasksDatabase.db"
        private const val TABLE_TASKS = "tasks"

        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_DEADLINE = "deadline"
        private const val KEY_DURATION = "duration"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_COMPLETED = "completed"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE " + TABLE_TASKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT,"
                + KEY_DEADLINE + " INTEGER,"
                + KEY_DURATION + " INTEGER,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_COMPLETED + " INTEGER)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    fun addTask(task: Task): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, task.name)
            put(KEY_DEADLINE, task.deadline.time) // Store date as Long (milliseconds)
            put(KEY_DURATION, task.duration)
            put(KEY_DESCRIPTION, task.description)
            put(KEY_COMPLETED, if (task.completed) 1 else 0) // Store boolean as Int
        }
        val id = db.insert(TABLE_TASKS, null, values)
        db.close()
        return id
    }

    fun getTask(id: Long): Task? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TASKS, arrayOf(KEY_ID, KEY_NAME, KEY_DEADLINE, KEY_DURATION, KEY_DESCRIPTION, KEY_COMPLETED),
            "$KEY_ID=?", arrayOf(id.toString()), null, null, null, null
        )

        var task: Task? = null
        if (cursor != null && cursor.moveToFirst()) {
            task = Task(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                deadline = Date(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DEADLINE))),
                duration = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DURATION)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                completed = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COMPLETED)) == 1
            )
            cursor.close()
        }
        db.close()
        return task
    }

    fun getAllTasks(): List<Task> {
        val tasksList = mutableListOf<Task>()
        val selectQuery = "SELECT * FROM $TABLE_TASKS ORDER BY $KEY_DEADLINE DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val task = Task(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                    deadline = Date(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DEADLINE))),
                    duration = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DURATION)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    completed = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COMPLETED)) == 1
                )
                tasksList.add(task)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return tasksList
    }

    fun updateTask(task: Task): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, task.name)
            put(KEY_DEADLINE, task.deadline.time)
            put(KEY_DURATION, task.duration)
            put(KEY_DESCRIPTION, task.description)
            put(KEY_COMPLETED, if (task.completed) 1 else 0)
        }
        val rowsAffected = db.update(TABLE_TASKS, values, "$KEY_ID=?", arrayOf(task.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteTask(id: Long): Int {
        val db = this.writableDatabase
        val rowsAffected = db.delete(TABLE_TASKS, "$KEY_ID=?", arrayOf(id.toString()))
        db.close()
        return rowsAffected
    }

    fun updateTaskCompletion(id: Long, completed: Boolean): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_COMPLETED, if (completed) 1 else 0)
        }
        val rowsAffected = db.update(TABLE_TASKS, values, "$KEY_ID=?", arrayOf(id.toString()))
        db.close()
        return rowsAffected
    }
}
