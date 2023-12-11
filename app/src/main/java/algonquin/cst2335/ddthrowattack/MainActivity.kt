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

        infoButton.setOnClickListener{
            val builder = AlertDialog.Builder(this, R.style.AlertDialog)
            builder.setTitle("About")
                .setMessage("The thrown opponent will be knocked prone upon landing even if the target is not hit. It can decide to use its reaction to make a Dex Saving Throw which equals to 10 + your proficiency bonus + your STR modifier to halve the damage it will take.\n" +
                        "\n" +
                        "The target you want to hit can take the Dex Saving Throw without using its reaction to also halve the damage. If the target fails the Dex Saving Throw it is also knocked prone.\n")
                .setPositiveButton("Dismiss") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        calculateButton.setOnClickListener{
            val strengthInputText = binding.strengthInput.text.toString()
            val strength: Int? = strengthInputText.toIntOrNull()
            if (strength == null) {
                val builder = AlertDialog.Builder(this, R.style.AlertDialog)
                builder.setTitle("Error")
                    .setMessage("Strength score cannot be blank.")
                    .setPositiveButton("Dismiss") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }
            val size = binding.sizeInput.selectedItem.toString()
            var sizeModifier = 1
            when (size) {
                "Small/Medium" -> sizeModifier = 1
                "Large/Powerful Build" -> sizeModifier = 2
                "Huge" -> sizeModifier = 4
            }
            val weightInputText = binding.enemyWeightInput.text.toString()
            val weight: Int? = weightInputText.toIntOrNull()
            if (weight == null) {
                val builder = AlertDialog.Builder(this, R.style.AlertDialog)
                builder.setTitle("Error")
                    .setMessage("Weight cannot be blank.")
                    .setPositiveButton("Dismiss") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }


            val capacity = calculateCapacity(strength, sizeModifier)
            binding.capacityOutput.text = String.format("$capacity lbs.")
            val pushLiftDrag = calculatePushDragLiftLimit(strength, sizeModifier)
            binding.pushDragLiftOutput.text = String.format("$pushLiftDrag lbs.")
            if (canThrow(weight, pushLiftDrag)) {
                val throwShort = calculateRangeShort(weight, capacity)
                val throwLong = calculateRangeLong(throwShort)
                binding.rangeOutput.text = String.format("$throwShort" + "ft./" + "$throwLong" + "ft.")
                binding.rangeOutput.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                binding.damageOutput.text = calculateDamage(strength, weight, capacity)
                binding.damageOutput.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                binding.rangeOutput.text = String.format("Cannot Throw")
                binding.rangeOutput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                binding.damageOutput.text = String.format("Cannot Throw")
                binding.damageOutput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val loadedPreferences = getSharedPreferences("ThrowData", MODE_PRIVATE)
        val savedStrength = loadedPreferences.getInt("strength", 15)
        binding.strengthInput.setText(savedStrength.toString())
        val savedSpinnerPosition = loadedPreferences.getInt("spinner_position", 0)
        binding.sizeInput.setSelection(savedSpinnerPosition)
    }

    override fun onPause() {
        super.onPause()
        val sharedPreferences = getSharedPreferences("ThrowData", MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putInt("strength", binding.strengthInput.text.toString().toInt())
        edit.putInt("spinner_position", binding.sizeInput.selectedItemPosition)
        edit.apply()
    }

    private fun calculateCapacity(strength: Int, sizeModifier: Int): Int {
        return strength * 15 * sizeModifier
    }

    private fun calculatePushDragLiftLimit(strength: Int, sizeModifier: Int): Int {
        return strength * 30 * sizeModifier
    }

    private fun canThrow(weight: Int, pdl: Int): Boolean {
        val maxWeight = pdl / 2
        return weight <= maxWeight
    }

    private fun calculateRangeShort(weight: Int, capacity: Int): Int {
        val rangeMultiplier: Int =
            if (weight*4 <= capacity) {
                4
            } else if (weight*3 <= capacity) {
                3
            }else if (weight*2 <= capacity) {
                2
            } else {
                1
            }
        return 5 * rangeMultiplier
    }

    private fun calculateRangeLong(shortRange: Int): Int {
        if (shortRange == 5) {
            return 5
        } else {
            return shortRange * 3
        }
    }

    private fun calculateDamage(strength: Int, weight: Int, capacity: Int): String {
        var baseStrength = strength
        if (strength%2 == 1) {--baseStrength}
        val strengthModifier = ((baseStrength - 10)/2)
        val isIncrease: Boolean = strengthModifier >= 0

        if (calculateRangeShort(weight, capacity) == 5) {
            return ("1d8" + if (isIncrease) {"+"} else {""} + "$strengthModifier")
        } else {
            val dice = weight/50 + 1
            return ("$dice" + "d6" + if (isIncrease) {"+"} else {""} + "$strengthModifier")
        }

    }
}
