package com.example.gestorgastos.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gestorgastos.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import com.example.gestorgastos.data.database.AppDatabase
import com.example.gestorgastos.data.model.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import java.io.FileOutputStream

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNotifications(isChecked)
        }

        binding.buttonExport.setOnClickListener {
            showExportTypeDialog()
        }

        binding.buttonClear.setOnClickListener {
            viewModel.clearData()
        }

        // Botón para cerrar sesión
        binding.root.findViewById<com.google.android.material.button.MaterialButton?>(R.id.button_logout)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }
    }

    private fun showExportTypeDialog() {
        val options = arrayOf("General (todos los gastos)", "Por filtro (fecha/categoría)")
        AlertDialog.Builder(requireContext())
            .setTitle("¿Qué deseas exportar?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFormatDialog(exportAll = true)
                    1 -> showFilterDialog()
                }
            }
            .show()
    }

    private fun showFormatDialog(exportAll: Boolean, filter: ExportFilter? = null) {
        val formats = arrayOf("PDF", "Excel (.xlsx)")
        AlertDialog.Builder(requireContext())
            .setTitle("Elige el formato de exportación")
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> exportDataToPdf(exportAll, filter)
                    1 -> exportDataToExcel(exportAll, filter)
                }
            }
            .show()
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_export_filter, null)
        val editStartDate = dialogView.findViewById<EditText>(R.id.edit_start_date)
        val editEndDate = dialogView.findViewById<EditText>(R.id.edit_end_date)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)

        // Configurar categorías (puedes personalizar la lista)
        val categories = listOf("Todas", "Comida", "Transporte", "Entretenimiento", "Servicios", "Otros")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // DatePickers
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        editStartDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                editStartDate.setText(dateFormat.format(calendar.time))
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        editEndDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                editEndDate.setText(dateFormat.format(calendar.time))
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar gastos")
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                val startDate = editStartDate.text.toString().takeIf { it.isNotEmpty() }?.let { dateFormat.parse(it) }
                val endDate = editEndDate.text.toString().takeIf { it.isNotEmpty() }?.let { dateFormat.parse(it) }
                val category = spinnerCategory.selectedItem.toString().takeIf { it != "Todas" }
                val filter = ExportFilter(startDate, endDate, category)
                showFormatDialog(exportAll = false, filter = filter)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportDataToPdf(exportAll: Boolean, filter: ExportFilter?) {
        lifecycleScope.launch {
            val expenses = withContext(Dispatchers.IO) { getExpensesForExport(exportAll, filter) }
            if (expenses.isEmpty()) {
                Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = withContext(Dispatchers.IO) { createPdfFile(expenses) }
            shareFile(file)
        }
    }

    private fun exportDataToExcel(exportAll: Boolean, filter: ExportFilter?) {
        lifecycleScope.launch {
            val expenses = withContext(Dispatchers.IO) { getExpensesForExport(exportAll, filter) }
            if (expenses.isEmpty()) {
                Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = withContext(Dispatchers.IO) { createExcelFile(expenses) }
            shareFile(file)
        }
    }

    private fun getExpensesForExport(exportAll: Boolean, filter: ExportFilter?): List<Expense> {
        val db = AppDatabase.getDatabase(requireContext().applicationContext)
        val dao = db.expenseDao()
        val allExpenses = dao.getAllExpenses().value ?: emptyList()
        if (exportAll) return allExpenses
        return allExpenses.filter { expense ->
            val inDate = (filter?.startDate == null || !expense.date.before(filter.startDate)) &&
                         (filter?.endDate == null || !expense.date.after(filter.endDate))
            val inCategory = filter?.category == null || expense.category == filter.category
            inDate && inCategory
        }
    }

    private fun createPdfFile(expenses: List<Expense>): File {
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "gastos_exportados.pdf")
        val writer = PdfWriter(file)
        val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = Document(pdfDoc)
        document.add(Paragraph("Reporte de Gastos"))
        val table = Table(floatArrayOf(2f, 4f, 3f, 3f, 2f))
        table.addCell("Monto")
        table.addCell("Descripción")
        table.addCell("Categoría")
        table.addCell("Fecha")
        table.addCell("Tipo")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        for (e in expenses) {
            table.addCell(String.format("$%.2f", e.amount))
            table.addCell(e.description)
            table.addCell(e.category)
            table.addCell(dateFormat.format(e.date))
            table.addCell(if (e.isIncome) "Ingreso" else "Gasto")
        }
        document.add(table)
        document.close()
        return file
    }

    private fun createExcelFile(expenses: List<Expense>): File {
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "gastos_exportados.xlsx")
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Gastos")
        val header = sheet.createRow(0)
        val headers = listOf("Monto", "Descripción", "Categoría", "Fecha", "Tipo")
        for ((i, h) in headers.withIndex()) {
            val cell = header.createCell(i)
            cell.setCellValue(h)
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        for ((rowIdx, e) in expenses.withIndex()) {
            val row = sheet.createRow(rowIdx + 1)
            row.createCell(0).setCellValue(e.amount)
            row.createCell(1).setCellValue(e.description)
            row.createCell(2).setCellValue(e.category)
            row.createCell(3).setCellValue(dateFormat.format(e.date))
            row.createCell(4).setCellValue(if (e.isIncome) "Ingreso" else "Gasto")
        }
        val fos = FileOutputStream(file)
        workbook.write(fos)
        fos.close()
        workbook.close()
        return file
    }

    private fun shareFile(file: File) {
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/octet-stream"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(intent, "Compartir archivo"))
    }

    data class ExportFilter(
        val startDate: Date?,
        val endDate: Date?,
        val category: String?
    )

    private fun loadSettings() {
        viewModel.isDarkMode.observe(viewLifecycleOwner) { isDarkMode ->
            binding.switchDarkMode.isChecked = isDarkMode
        }

        viewModel.isNotificationsEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.switchNotifications.isChecked = isEnabled
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 