package com.example.gestorgastos.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.gestorgastos.data.database.AppDatabase
import com.example.gestorgastos.data.model.Expense
import kotlinx.coroutines.launch

class ExpenseListViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()

    val expenses: LiveData<List<Expense>> = expenseDao.getAllExpenses()

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.delete(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseDao.update(expense)
        }
    }
} 