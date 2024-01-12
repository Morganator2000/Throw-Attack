package algonquin.cst2335.ddthrowattack

import algonquin.cst2335.ddthrowattack.databinding.MainActivityBinding
import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {
    //Establish binding.
    private lateinit var binding: MainActivityBinding
    //onCreate sets up the initial app state.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Retrieve binding
        binding = MainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val infoButton = binding.infoButton
        val calculateButton = binding.calculateButton

        //Listener for the information button.
        infoButton.setOnClickListener{
            val builder = AlertDialog.Builder(this, R.style.AlertDialog)
            builder.setTitle("About")
                .setMessage("The thrown opponent will be knocked prone upon landing even if the " +
                        "target is not hit. It can decide to use its reaction to make a Dex Saving " +
                        "Throw which equals to 10 + your proficiency bonus + your STR modifier to " +
                        "halve the damage it will take.\n" +
                        "\n" +
                        "The target you want to hit can take the Dex Saving Throw without using its " +
                        "reaction to also halve the damage. If the target fails the Dex Saving Throw " +
                        "it is also knocked prone.\n")
                .setPositiveButton("Dismiss") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        //Listener for the calculate button
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

            //Verify Size mod
            val size = binding.sizeInput.selectedItem.toString()
            var sizeModifier = 1
            when (size) {
                "Small/Medium" -> sizeModifier = 1
                "Large/Powerful Build" -> sizeModifier = 2
                "Huge" -> sizeModifier = 4
                "Gargantuan" -> sizeModifier = 8
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
            //Calculate and display capacity
            val capacity = calculateCapacity(strength, sizeModifier)
            binding.capacityOutput.text = String.format("$capacity lbs.")
            //Calculate and display Push/Drag/Lift Limit
            val pushLiftDrag = calculatePushDragLiftLimit(strength, sizeModifier)
            binding.pushDragLiftOutput.text = String.format("$pushLiftDrag lbs.")
            //Calculate range, if any.
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
    //App state when the user comes back to the app.
    override fun onResume() {
        super.onResume()
        //Load saved preferences. Specifically the strength score and size (not weight).
        val loadedPreferences = getSharedPreferences("ThrowData", MODE_PRIVATE)
        val savedStrength = loadedPreferences.getInt("strength", 15)
        binding.strengthInput.setText(savedStrength.toString())
        val savedSpinnerPosition = loadedPreferences.getInt("spinner_position", 0)
        binding.sizeInput.setSelection(savedSpinnerPosition)
    }
    //onPause runs when the user leaves the app
    override fun onPause() {
        super.onPause()
        //Save preferences. Specifically the strength score and size (not weight).
        val sharedPreferences = getSharedPreferences("ThrowData", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("strength", binding.strengthInput.text.toString().toInt())
        editor.putInt("spinner_position", binding.sizeInput.selectedItemPosition)
        editor.apply()
    }


    //Calculate the player's carrying capacity.
    private fun calculateCapacity(strength: Int, sizeModifier: Int): Int {
        return strength * 15 * sizeModifier
    }
    //Calculate the push/drag/lift limit of the player.
    private fun calculatePushDragLiftLimit(strength: Int, sizeModifier: Int): Int {
        return strength * 30 * sizeModifier
    }
    //Check to make sure the weight of the thrown object does not exceed their carrying capacity.
    private fun canThrow(weight: Int, pdl: Int): Boolean {
        val maxWeight = pdl / 2
        return weight <= maxWeight
    }
    //Calculate the throwing range.
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
    //Calculate the range when throwing long, with disadvantage.
    private fun calculateRangeLong(shortRange: Int): Int {
        //If you can't throw more than 5 ft, it's a melee attack.
        if (shortRange == 5) {
            return 5
        } else {
            return shortRange * 3
        }
    }
    //The damage calculation delivered to the target and thrown person.
    private fun calculateDamage(strength: Int, weight: Int, capacity: Int): String {
        var baseStrength = strength
        if (strength%2 == 1) {--baseStrength}
        val strengthModifier = ((baseStrength - 10)/2)
        val isIncrease: Boolean = strengthModifier >= 0

        if (calculateRangeShort(weight, capacity) == 5) {
            //If you can't throw more than 5 ft, treat them as an improvised greatclub.
            return ("1d8" + if (isIncrease) {"+"} else {""} + "$strengthModifier")
        } else {
            //Otherwise, damage is based on weight.
            val dice = weight/50 + 1
            return ("$dice" + "d6" + if (isIncrease) {"+"} else {""} + "$strengthModifier")
        }

    }
}
