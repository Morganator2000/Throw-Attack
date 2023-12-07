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
                        "The target you want to hit can take the Dex Saving Throw without using its reaction to also halve the damage. If the target fails the Dex Saving Throw it is also knocked prone.\n" +
                        "\n" +
                        "The damage is calculated as if the opponent is falling to the ground. That means 1d6 for every 10ft. You may add your STR modifier to the damage roll. Both the thrown and the hit opponent take the damage.")
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
                binding.damageOutput.text = calculateDamage(strength, weight)
                binding.damageOutput.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                binding.rangeOutput.text = String.format("Cannot Throw")
                binding.rangeOutput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                binding.damageOutput.text = String.format("Cannot Throw")
                binding.damageOutput.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
        }
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

//    private fun calculateRangeShort(weight: Int, capacity: Int): Int {
//        val fraction = capacity/4
//        var difference = capacity-weight
//        val remainder = difference%fraction
//        difference -= remainder
//        val additionalRange = difference/fraction
//        return 5 + 5 * additionalRange
//    }

    private fun calculateRangeShort(weight: Int, capacity: Int): Int {
        var rangeMultiplier: Int =
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
    //
    //20 str + 40 lb + huge = 60 ft
    //3 str + 5 lb + small = 20 ft

    private fun calculateRangeLong(weight: Int, pdl: Int): Int {
        val fraction = pdl/8
        var difference = pdl/2-weight
        val remainder = difference%fraction
        difference -= remainder
        val additionalRange = difference/fraction
        return 10 + 10 * additionalRange
    }

    private fun calculateRangeLong(shortRange: Int): Int {
        return shortRange * 3
    }

    private fun calculateDamage(strength: Int, shortRange: Int, longRange: Int): String {
        var baseStrength = strength
        if (strength%2 == 1) {--baseStrength}
        val strengthModifier = ((baseStrength - 10)/2)
        val isIncrease: Boolean = strengthModifier >= 0

        var shortTrimmed = shortRange%10
        shortTrimmed = shortRange-shortTrimmed
        val shortDamageDice = (shortTrimmed / 10)

        var longTrimmed = longRange%10
        longTrimmed = longRange-longTrimmed
        val longDamageDice = (longTrimmed / 10)
        return ("$shortDamageDice" + "d6"+ if (isIncrease) {"+"} else {""} + "$strengthModifier"
                + "/" +
                "$longDamageDice" + "d6"+ if (isIncrease) {"+"} else {""} + "$strengthModifier")
    }

    private fun calculateDamage(strength: Int, weight: Int): String {
        var baseStrength = strength
        if (strength%2 == 1) {--baseStrength}
        val strengthModifier = ((baseStrength - 10)/2)
        val isIncrease: Boolean = strengthModifier >= 0

        val dice = weight/50 + 1
        return ("$dice" + "d6"+ if (isIncrease) {"+"} else {""} + "$strengthModifier")
        //200 = 8d10
        // 45 = 3d10
        // 70 = 4d10
    }
}
