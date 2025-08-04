package com.example.todolist

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView // Added for TextView in detail dialog
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Data class for Task
data class Task(
    var id: Long? = null, // Nullable Long for SQLite auto-increment ID
    val name: String,
    val deadline: Date,
    val duration: Int, // Number of days
    val description: String,
    var completed: Boolean = false // Default to not completed
) {
    // Override toString to display task name and status in the ListView
    override fun toString(): String {
        val status = if (completed) "[COMPLETED] " else ""
        return status + name + " - Deadline: " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(deadline)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var editTextTaskName: EditText
    private lateinit var editTextTaskDuration: EditText
    private lateinit var editTextTaskDescription: EditText
    private lateinit var buttonAddTask: Button
    private lateinit var listViewTasks: ListView
    private lateinit var tasksAdapter: ArrayAdapter<Task>
    private val tasksList = mutableListOf<Task>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var dbHelper: TaskDbHelper

    private lateinit var spinnerDay: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner

    private lateinit var dayAdapter: ArrayAdapter<Int>
    private val daysList = mutableListOf<Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TaskDbHelper(this)

        editTextTaskName = findViewById(R.id.editTextTaskName)
        editTextTaskDuration = findViewById(R.id.editTextTaskDuration)
        editTextTaskDescription = findViewById(R.id.editTextTaskDescription)
        buttonAddTask = findViewById(R.id.buttonAddTask)
        listViewTasks = findViewById(R.id.listViewTasks)

        spinnerDay = findViewById(R.id.spinnerDay)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        spinnerYear = findViewById(R.id.spinnerYear)

        tasksAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tasksList)
        listViewTasks.adapter = tasksAdapter

        setupDeadlineSpinners()
        loadTasksFromDb()

        buttonAddTask.setOnClickListener {
            val taskName = editTextTaskName.text.toString().trim()
            val durationStr = editTextTaskDuration.text.toString().trim()
            val description = editTextTaskDescription.text.toString().trim()

            if (taskName.isNotEmpty() && durationStr.isNotEmpty() && description.isNotEmpty()) {
                try {
                    val year = spinnerYear.selectedItem as Int
                    val month = spinnerMonth.selectedItemPosition // 0-indexed
                    val day = spinnerDay.selectedItem as Int

                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, day)
                    val deadline = calendar.time
                    
                    val duration = durationStr.toInt()
                    val newTask = Task(name = taskName, deadline = deadline, duration = duration, description = description)
                    
                    dbHelper.addTask(newTask)
                    loadTasksFromDb() // Refresh list from DB

                    // Clear input fields
                    editTextTaskName.text.clear()
                    editTextTaskDuration.text.clear()
                    editTextTaskDescription.text.clear()
                    val currentCalendar = Calendar.getInstance()
                    spinnerYear.setSelection(0) 
                    spinnerMonth.setSelection(currentCalendar.get(Calendar.MONTH))
                    updateDaysSpinner(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH))
                    spinnerDay.setSelection(currentCalendar.get(Calendar.DAY_OF_MONTH) -1 )

                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input format: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
        }

        listViewTasks.setOnItemClickListener { _, _, position, _ ->
            showTaskDetailDialog(tasksList[position])
        }

        listViewTasks.setOnItemLongClickListener { _, _, position, _ ->
            showEditDeleteDialog(tasksList[position]) // Pass Task object
            true // Consume the long click
        }
    }

    private fun setupDeadlineSpinners() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear..currentYear + 10).toList()
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter

        val monthNameMap = SimpleDateFormat("MMMM", Locale.getDefault()).calendar.getDisplayNames(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val months = monthNameMap?.keys?.toTypedArray() ?: arrayOf<String>()
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter
        
        dayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysList)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = dayAdapter

        val calendar = Calendar.getInstance()
        spinnerYear.setSelection(0) 
        spinnerMonth.setSelection(calendar.get(Calendar.MONTH)) 
        updateDaysSpinner(spinnerYear.selectedItem as Int, spinnerMonth.selectedItemPosition)
        spinnerDay.setSelection(calendar.get(Calendar.DAY_OF_MONTH) - 1) 

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDaysSpinner(spinnerYear.selectedItem as Int, spinnerMonth.selectedItemPosition)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerYear.onItemSelectedListener = listener
        spinnerMonth.onItemSelectedListener = listener
    }

    private fun updateDaysSpinner(year: Int, monthIndex: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthIndex)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val previouslySelectedDay = if (spinnerDay.adapter != null && spinnerDay.selectedItemPosition != AdapterView.INVALID_POSITION && spinnerDay.selectedItem is Int) {
            spinnerDay.selectedItem as Int
        } else {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        }

        daysList.clear()
        daysList.addAll((1..maxDays).toList())
        dayAdapter.notifyDataSetChanged()

        if (previouslySelectedDay <= maxDays) {
            spinnerDay.setSelection(daysList.indexOf(previouslySelectedDay).takeIf { it != -1 } ?: 0)
        } else {
            spinnerDay.setSelection(daysList.size -1) 
        }
    }

    private fun loadTasksFromDb() {
        tasksList.clear()
        tasksList.addAll(dbHelper.getAllTasks())
        tasksAdapter.notifyDataSetChanged()
    }

    private fun showTaskDetailDialog(task: Task) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_task_detail, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle(getString(R.string.task_detail_title))

        val textViewName = dialogView.findViewById<TextView>(R.id.textViewDetailTaskName)
        val textViewDeadline = dialogView.findViewById<TextView>(R.id.textViewDetailDeadline)
        val textViewDuration = dialogView.findViewById<TextView>(R.id.textViewDetailDuration)
        val textViewDescription = dialogView.findViewById<TextView>(R.id.textViewDetailDescription)
        val textViewStatus = dialogView.findViewById<TextView>(R.id.textViewDetailStatus)
        val buttonEdit = dialogView.findViewById<Button>(R.id.buttonDetailEditTask)
        val buttonDelete = dialogView.findViewById<Button>(R.id.buttonDetailDeleteTask)

        textViewName.text = "${getString(R.string.label_task_name)} ${task.name}"
        textViewDeadline.text = "${getString(R.string.label_deadline)} ${dateFormat.format(task.deadline)}"
        textViewDuration.text = "${getString(R.string.label_duration)} ${task.duration}"
        textViewDescription.text = "${getString(R.string.label_description)} ${task.description}"
        textViewStatus.text = "${getString(R.string.label_status)} ${if (task.completed) getString(R.string.status_completed) else getString(R.string.status_pending)}"

        val detailDialog = dialogBuilder.create()

        buttonEdit.setOnClickListener {
            showEditDeleteDialog(task)
            detailDialog.dismiss()
        }

        buttonDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete_title))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.delete_button_text)) { _, _ ->
                    task.id?.let {
                        dbHelper.deleteTask(it)
                        loadTasksFromDb()
                        detailDialog.dismiss()
                    } ?: Toast.makeText(this, "Error deleting task: ID is null", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel_button_text), null)
                .show()
        }
        
        detailDialog.show()
    }

    private fun showEditDeleteDialog(taskToEdit: Task) { // Changed signature to accept Task
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Edit or Delete Task")

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_edit_task, null)
        dialogBuilder.setView(dialogView)

        val dialogEditTextTaskName = dialogView.findViewById<EditText>(R.id.dialogEditTextTaskName)
        val dialogEditTextTaskDeadline = dialogView.findViewById<EditText>(R.id.dialogEditTextTaskDeadline)
        val dialogEditTextTaskDuration = dialogView.findViewById<EditText>(R.id.dialogEditTextTaskDuration)
        val dialogEditTextTaskDescription = dialogView.findViewById<EditText>(R.id.dialogEditTextTaskDescription)
        val dialogCheckBoxCompleted = dialogView.findViewById<CheckBox>(R.id.dialogCheckBoxCompleted)

        dialogEditTextTaskName.setText(taskToEdit.name)
        dialogEditTextTaskDeadline.setText(dateFormat.format(taskToEdit.deadline))
        dialogEditTextTaskDuration.setText(taskToEdit.duration.toString())
        dialogEditTextTaskDescription.setText(taskToEdit.description)
        dialogCheckBoxCompleted.isChecked = taskToEdit.completed

        dialogEditTextTaskDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = taskToEdit.deadline 
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDateCalendar = Calendar.getInstance()
                    selectedDateCalendar.set(selectedYear, selectedMonth, selectedDay)
                    val selectedDate = selectedDateCalendar.time
                    dialogEditTextTaskDeadline.setText(dateFormat.format(selectedDate))
                }, year, month, day)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                datePickerDialog.datePicker.calendarViewShown = false
                datePickerDialog.datePicker.spinnersShown = true
            }
            datePickerDialog.show()
        }

        dialogBuilder.setPositiveButton("Update") { _, _ ->
            val updatedName = dialogEditTextTaskName.text.toString().trim()
            val updatedDeadlineStr = dialogEditTextTaskDeadline.text.toString().trim()
            val updatedDurationStr = dialogEditTextTaskDuration.text.toString().trim()
            val updatedDescription = dialogEditTextTaskDescription.text.toString().trim()
            val updatedCompleted = dialogCheckBoxCompleted.isChecked

            if (updatedName.isNotEmpty() && updatedDeadlineStr.isNotEmpty() && updatedDurationStr.isNotEmpty()) {
                try {
                    val updatedDeadline = dateFormat.parse(updatedDeadlineStr) ?: taskToEdit.deadline
                    val updatedDuration = updatedDurationStr.toInt()

                    val updatedTask = taskToEdit.copy(
                        name = updatedName,
                        deadline = updatedDeadline,
                        duration = updatedDuration,
                        description = updatedDescription,
                        completed = updatedCompleted
                    )
                    
                    taskToEdit.id?.let {
                        dbHelper.updateTask(updatedTask)
                        loadTasksFromDb()
                    } ?: Toast.makeText(this, "Error updating task: ID is null", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input format for update: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Name, deadline, and duration cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Delete") { _, _ ->
             AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete_title))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.delete_button_text)) { _, _ ->
                    taskToEdit.id?.let {
                        dbHelper.deleteTask(it)
                        loadTasksFromDb()
                    } ?: Toast.makeText(this, "Error deleting task: ID is null", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel_button_text), null)
                .show()
        }

        dialogBuilder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }
}
