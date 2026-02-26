package otus.homework.customview

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import otus.homework.customview.databinding.ActivityMainBinding
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonString = resources.openRawResource(R.raw.payload).bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val data = mutableMapOf<String, Int>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val category = item.getString("category")
            val amount = item.getInt("amount")
            data[category] = data.getOrDefault(category, 0) + amount
        }

        binding.pieChart.setData(data)
        binding.pieChart.setOnSegmentClickListener { category ->
            Toast.makeText(this, "Clicked on: $category", Toast.LENGTH_SHORT).show()
        }
    }
}
