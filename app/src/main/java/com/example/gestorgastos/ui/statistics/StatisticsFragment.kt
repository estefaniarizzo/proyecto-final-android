package com.example.gestorgastos.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gestorgastos.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.*

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupObservers()
    }

    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            legend.textSize = 12f
        }

        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            legend.textSize = 12f
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
        }
    }

    private fun setupObservers() {
        viewModel.expensesByCategory.observe(viewLifecycleOwner) { expenses ->
            updatePieChart(expenses)
        }

        viewModel.monthlyExpenses.observe(viewLifecycleOwner) { expenses ->
            updateBarChart(expenses)
        }

        viewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            binding.textTotalIncome.text = "Ingresos: ${currencyFormat.format(income)}"
        }

        viewModel.totalExpenses.observe(viewLifecycleOwner) { expenses ->
            binding.textTotalExpenses.text = "Gastos: ${currencyFormat.format(expenses)}"
        }

        viewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.textBalance.text = "Balance: ${currencyFormat.format(balance)}"
        }
    }

    private fun updatePieChart(expenses: Map<String, Double>) {
        val entries = expenses.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Gastos por Categoría")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    private fun updateBarChart(expenses: Map<String, Double>) {
        val entries = expenses.map { BarEntry(it.key.toFloat(), it.value.toFloat()) }
        val dataSet = BarDataSet(entries, "Gastos Mensuales")
        dataSet.color = Color.BLUE
        binding.barChart.data = BarData(dataSet)
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 