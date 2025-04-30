package com.example.gestorgastos.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestorgastos.R
import com.example.gestorgastos.data.model.Expense
import com.example.gestorgastos.databinding.FragmentAddExpenseBinding
import com.example.gestorgastos.ui.ai.AIViewModel
import java.util.*

class AddExpenseFragment : Fragment() {
    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddExpenseViewModel by viewModels()
    private val aiViewModel: AIViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryDropdown()
        setupListeners()
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Comida", "Transporte", "Entretenimiento", "Servicios", "Otros")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.editCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            saveExpense()
        }

        // Agregar listener para categorización automática
        binding.editDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val description = binding.editDescription.text.toString()
                if (description.isNotEmpty()) {
                    aiViewModel.categorizeExpense(description) { category ->
                        binding.editCategory.setText(category, false)
                    }
                }
            }
        }
    }

    private fun saveExpense() {
        val amount = binding.editAmount.text.toString().toDoubleOrNull() ?: 0.0
        val description = binding.editDescription.text.toString()
        val category = binding.editCategory.text.toString()
        val isIncome = binding.switchIncome.isChecked

        if (amount > 0 && description.isNotEmpty() && category.isNotEmpty()) {
            val expense = Expense(
                amount = amount,
                description = description,
                category = category,
                date = Date(),
                isIncome = isIncome
            )
            viewModel.saveExpense(expense)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 