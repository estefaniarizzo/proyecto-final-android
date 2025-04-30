package com.example.gestorgastos.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestorgastos.databinding.FragmentAiInsightsBinding
import java.text.NumberFormat
import java.util.*

class AIInsightsFragment : Fragment() {
    private var _binding: FragmentAiInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AIViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    private val recommendationsAdapter = RecommendationsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        loadData()
    }

    private fun setupRecyclerView() {
        binding.recyclerRecommendations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendationsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.predictedExpense.observe(viewLifecycleOwner) { amount ->
            binding.textPredictedAmount.text = "Predicción: ${currencyFormat.format(amount)}"
        }

        viewModel.spendingPatterns.observe(viewLifecycleOwner) { patterns ->
            val patternText = buildString {
                patterns["maxSpendingDay"]?.let {
                    append("Día con más gastos: $it\n")
                }
                patterns["trend"]?.let {
                    append("Tendencia de gastos: $it")
                }
            }
            binding.textSpendingPatterns.text = patternText
        }

        viewModel.savingsRecommendations.observe(viewLifecycleOwner) { recommendations ->
            recommendationsAdapter.submitList(recommendations)
        }
    }

    private fun loadData() {
        viewModel.predictNextExpense()
        viewModel.detectSpendingPatterns()
        viewModel.getSavingsRecommendations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class RecommendationsAdapter : androidx.recyclerview.widget.ListAdapter<String, RecommendationsAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)

        fun bind(recommendation: String) {
            textView.text = recommendation
        }
    }
} 