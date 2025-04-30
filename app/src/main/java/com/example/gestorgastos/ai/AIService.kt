package com.example.gestorgastos.ai

import android.content.Context
import com.example.gestorgastos.data.database.AppDatabase
import com.example.gestorgastos.data.model.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class AIService(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val database = AppDatabase.getDatabase(context)
    private val expenseDao = database.expenseDao()

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val model = loadModelFile("expense_prediction_model.tflite")
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun predictNextExpense(): Double = withContext(Dispatchers.Default) {
        val expenses = expenseDao.getAllExpenses().first()
        if (expenses.isEmpty()) return@withContext 0.0

        // Implementación simple de predicción basada en el promedio móvil
        val recentExpenses = expenses.takeLast(5)
        return@withContext recentExpenses.map { it.amount }.average()
    }

    suspend fun categorizeExpense(description: String): String = withContext(Dispatchers.Default) {
        // Implementación simple de categorización basada en palabras clave
        return@withContext when {
            description.contains("comida", ignoreCase = true) ||
            description.contains("restaurante", ignoreCase = true) ||
            description.contains("super", ignoreCase = true) -> "Comida"
            
            description.contains("transporte", ignoreCase = true) ||
            description.contains("uber", ignoreCase = true) ||
            description.contains("taxi", ignoreCase = true) -> "Transporte"
            
            description.contains("entretenimiento", ignoreCase = true) ||
            description.contains("cine", ignoreCase = true) ||
            description.contains("netflix", ignoreCase = true) -> "Entretenimiento"
            
            description.contains("servicio", ignoreCase = true) ||
            description.contains("luz", ignoreCase = true) ||
            description.contains("agua", ignoreCase = true) -> "Servicios"
            
            else -> "Otros"
        }
    }

    suspend fun getSavingsRecommendations(): List<String> = withContext(Dispatchers.Default) {
        val recommendations = mutableListOf<String>()
        val expenses = expenseDao.getAllExpenses().first()
        
        // Analizar gastos por categoría
        val expensesByCategory = expenses
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        // Generar recomendaciones basadas en patrones
        expensesByCategory.forEach { (category, amount) ->
            when (category) {
                "Comida" -> if (amount > 5000) {
                    recommendations.add("Considera cocinar más en casa para reducir gastos en comida")
                }
                "Entretenimiento" -> if (amount > 2000) {
                    recommendations.add("Podrías reducir gastos de entretenimiento buscando alternativas gratuitas")
                }
                "Transporte" -> if (amount > 3000) {
                    recommendations.add("Considera usar transporte público o compartir viajes para reducir gastos")
                }
            }
        }

        return@withContext recommendations
    }

    suspend fun detectSpendingPatterns(): Map<String, Any> = withContext(Dispatchers.Default) {
        val patterns = mutableMapOf<String, Any>()
        val expenses = expenseDao.getAllExpenses().first()
        
        // Analizar gastos por día de la semana
        val expensesByDay = expenses
            .filter { !it.isIncome }
            .groupBy { Calendar.getInstance().apply { time = it.date }.get(Calendar.DAY_OF_WEEK) }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        // Encontrar el día con más gastos
        val maxSpendingDay = expensesByDay.maxByOrNull { it.value }
        if (maxSpendingDay != null) {
            patterns["maxSpendingDay"] = when (maxSpendingDay.key) {
                Calendar.SUNDAY -> "Domingo"
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                else -> "Desconocido"
            }
        }

        // Calcular tendencia de gastos
        val monthlyExpenses = expenses
            .filter { !it.isIncome }
            .groupBy { Calendar.getInstance().apply { time = it.date }.get(Calendar.MONTH) }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        val trend = if (monthlyExpenses.size >= 2) {
            val lastTwoMonths = monthlyExpenses.values.takeLast(2)
            if (lastTwoMonths[1] > lastTwoMonths[0]) "Aumentando" else "Disminuyendo"
        } else "Estable"

        patterns["trend"] = trend

        return@withContext patterns
    }

    fun addExpense(expense: Expense) {
        expenseDao.insert(expense)
    }
} 