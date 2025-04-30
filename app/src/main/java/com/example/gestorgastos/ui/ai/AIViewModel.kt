package com.example.gestorgastos.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gestorgastos.ai.AIService
import com.example.gestorgastos.data.model.Expense
import kotlinx.coroutines.launch

class AIViewModel(application: Application) : AndroidViewModel(application) {
    private val aiService = AIService(application)

    private val _predictedExpense = MutableLiveData<Double>()
    val predictedExpense: LiveData<Double> = _predictedExpense

    private val _savingsRecommendations = MutableLiveData<List<String>>()
    val savingsRecommendations: LiveData<List<String>> = _savingsRecommendations

    private val _spendingPatterns = MutableLiveData<Map<String, Any>>()
    val spendingPatterns: LiveData<Map<String, Any>> = _spendingPatterns

    fun predictNextExpense() {
        viewModelScope.launch {
            _predictedExpense.value = aiService.predictNextExpense()
        }
    }

    fun getSavingsRecommendations() {
        viewModelScope.launch {
            _savingsRecommendations.value = aiService.getSavingsRecommendations()
        }
    }

    fun detectSpendingPatterns() {
        viewModelScope.launch {
            _spendingPatterns.value = aiService.detectSpendingPatterns()
        }
    }

    fun addExpense(expense: Expense) {
        aiService.addExpense(expense)
    }

    fun categorizeExpense(description: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            val category = aiService.categorizeExpense(description)
            callback(category)
        }
    }
} 