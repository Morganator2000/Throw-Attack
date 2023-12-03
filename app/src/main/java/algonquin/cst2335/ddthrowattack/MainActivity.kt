package algonquin.cst2335.ddthrowattack

import algonquin.cst2335.ddthrowattack.databinding.MainActivityBinding
import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var binding: MainActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val infoButton = binding.infoButton
        val calculateButton = binding.calculateButton
        val strength = binding.strengthInput.text.toString().toInt()
        val size = binding.sizeInput.selectedItem.toString()
        var isLarge = false
        if (size == "Large/Powerful Build") {
            isLarge = true
        }
        val weight = binding.enemyWeightInput.text.toString().toInt()

        infoButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("About")
                .setMessage("The thrown opponent will be knocked prone upon landing even if the target is not hit. It can decide to use its reaction to make a Dex Saving Throw which equals to 10 + your proficiency bonus + your STR modifier to halve the damage it will take.\n" +
                        "\n" +
                        "The target you want to hit can take the Dex Saving Throw without using its reaction to also halve the damage. If the target fails the Dex Saving Throw it is also knocked prone.") //TODO
                .setPositiveButton("Dismiss") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        calculateButton.setOnClickListener{
            val capacity = calculateCapacity(strength, isLarge)
            binding.capacityOutput.text = String.format("$capacity lbs.")
            val pushLiftDrag = calculatePushDragLiftLimit(strength, isLarge)
            binding.pushDragLiftOutput.text = String.format("$pushLiftDrag lbs.")
            if (canThrow(weight, pushLiftDrag)) {
                val throwShort = calculateRangeShort(weight, pushLiftDrag)
                val throwLong = calculateRangeLong(weight, pushLiftDrag)
                binding.rangeOutput.text = String.format("$throwShort" + "ft./" + "$throwLong" + "ft.")
            } else {
                binding.rangeOutput.text = String.format("Cannot Throw")
                binding.rangeOutput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
        }
    }

    private fun calculateCapacity(strength: Int, isLarge: Boolean): Int {
        var modifier = 1
        if (isLarge) {
            modifier = 2
        }
        return strength * 15 * modifier
    }

    private fun calculatePushDragLiftLimit(strength: Int, isLarge: Boolean): Int {
        var modifier = 1
        if (isLarge) {
            modifier = 2
        }
        return strength * 30 * modifier
    }

    private fun canThrow(weight: Int, pdl: Int): Boolean {
        val maxWeight = pdl / 2
        return weight <= maxWeight
    }

    private fun calculateRangeShort(weight: Int, pdl: Int): Int {
        val fraction = pdl/4
        var difference = pdl-weight
        val remainder = difference%fraction
        difference -= remainder
        val additionalRange = difference/fraction
        return 10 + 5 * additionalRange
    }

    private fun calculateRangeLong(weight: Int, pdl: Int): Int {
        val fraction = pdl/4
        var difference = pdl-weight
        val remainder = difference%fraction
        difference -= remainder
        val additionalRange = difference/fraction
        return 30 + 10 * additionalRange
    }
}