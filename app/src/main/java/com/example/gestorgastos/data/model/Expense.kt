package com.example.gestorgastos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,           // Monto del gasto
    val description: String,      // Descripción del gasto
    val category: String,         // Categoría (comida, transporte, etc.)
    val date: Date,              // Fecha del gasto
    val isIncome: Boolean = false // Si es un ingreso o un gasto
) 