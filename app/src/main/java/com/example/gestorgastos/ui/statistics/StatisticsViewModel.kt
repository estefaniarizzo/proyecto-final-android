package com.example.gestorgastos.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestorgastos.data.database.AppDatabase
import com.example.gestorgastos.data.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()

    private val _expensesByCategory = MutableLiveData<Map<String, Double>>()
    val expensesByCategory: LiveData<Map<String, Double>> = _expensesByCategory

    private val _monthlyExpenses = MutableLiveData<Map<String, Double>>()
    val monthlyExpenses: LiveData<Map<String, Double>> = _monthlyExpenses

    private val _totalIncome = MutableLiveData<Double>()
    val totalIncome: LiveData<Double> = _totalIncome

    private val _totalExpenses = MutableLiveData<Double>()
    val totalExpenses: LiveData<Double> = _totalExpenses

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> = _balance

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val expenses = expenseDao.getAllExpenses().first()
            calculateStatistics(expenses)
        }
    }

    private fun calculateStatistics(expenses: List<Expense>) {
        // Gastos por categoría
        val categoryMap = expenses
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }
        _expensesByCategory.value = categoryMap

        // Gastos mensuales
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthlyMap = expenses
            .filter { !it.isIncome }
            .groupBy { dateFormat.format(it.date) }
            .mapValues { it.value.sumOf { expense -> expense.amount } }
        _monthlyExpenses.value = monthlyMap

        // Totales
        val totalIncome = expenses.filter { it.isIncome }.sumOf { it.amount }
        val totalExpenses = expenses.filter { !it.isIncome }.sumOf { it.amount }
        _totalIncome.value = totalIncome
        _totalExpenses.value = totalExpenses
        _balance.value = totalIncome - totalExpenses
    }
} 