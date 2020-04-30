package com.br.ml.brpathfinder.settings

import android.app.Activity
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.graphics.Color
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.br.ml.brpathfinder.R
import com.br.ml.brpathfinder.settings.SettingsFragment.FeedbackOption.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.main_activity.*

class SettingsFragment : Fragment(), OnItemSelectedListener {

    private var mediaPlayer: MediaPlayer = MediaPlayer()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val vibrateSwitch: SwitchMaterial = view.findViewById(R.id.settings_feedback_vibrate_switch)
        val soundSwitch: SwitchMaterial = view.findViewById(R.id.settings_feedback_sound_switch)
        val vibrateImage: ImageView = view.findViewById(R.id.vibrate_icon_image_view)
        val soundImage: ImageView = view.findViewById(R.id.sound_icon_image_view)

        val alertToneSpinner: Spinner = view.findViewById(R.id.settings_feedback_alert_tone_spinner)
        val noHeadphoneModeSwitch: SwitchMaterial = view.findViewById(R.id.settings_fragment_no_headphone_mode_switch)
        val noHeadphoneSuggestion: MaterialTextView = view.findViewById(R.id.settings_feedback_no_headphone_suggestion)
        val buttonLeft: MaterialButton = view.findViewById(R.id.settings_feedback_sound_test_left_side)
        val buttonCenter: MaterialButton = view.findViewById(R.id.settings_feedback_sound_test_center)
        val buttonRight: MaterialButton = view.findViewById(R.id.settings_feedback_sound_test_right_side)

        setUpOptionsFromSharedPrefs(
            vibrateSwitch,
            soundSwitch,
            vibrateImage,
            soundImage,
            alertToneSpinner,
            noHeadphoneModeSwitch,
            noHeadphoneSuggestion
        )

        // Switch controlling vibration feedback
        vibrateSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                vibrateImage.apply {
                    // Change icon color to accent color to show active
                    setAsAccentColor(isChecked)
                    // Shake the icon because why not
                    startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
                    // Create a vibration
                    this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            } else {
                vibrateImage.setAsAccentColor(isChecked)
            }

            when {
                isChecked && !soundSwitch.isChecked -> {
                    // Only vibrate is selected
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(VIBRATE)
                }
                isChecked && soundSwitch.isChecked -> {
                    // Both vibrate and sound is selected
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(BOTH)
                }
                !isChecked && soundSwitch.isChecked -> {
                    // No longer vibrate but sound is still active
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(SOUND)
                }
                else -> {
                    // Nothing is selected
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(NONE)
                }
            }
        }

        // Switch controlling sound feedback
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                soundImage.apply {
                    // Change the layout based on if the checkbox is checked
                    enableOrDisableSoundSettings(isChecked)
                    // Shake the icon because why not
                    startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
                    // Check if headphones are connected, if not, suggest NoHeadphone Mode
                    if (!areHeadphonesConnected()) {
                        noHeadphoneSuggestion.visibility = View.VISIBLE
                    } else {
                        noHeadphoneSuggestion.visibility = View.INVISIBLE
                    }
                    // Create a mediaPlayer to play the beep when the sound is enabled
                    val mediaPlayer =
                        MediaPlayer.create(
                            context,
                            pullAlertToneFromSharedPreferences(activity).soundFile
                        )
                    // Play the beep
                    mediaPlayer.start()
                }
            } else {
                enableOrDisableSoundSettings(isChecked)
            }

            when {
                isChecked && !vibrateSwitch.isChecked -> {
                    // Only sound is selected
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(SOUND)
                }
                isChecked && vibrateSwitch.isChecked -> {
                    // Both vibrate and sound is selected
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(BOTH)
                }
                !isChecked && vibrateSwitch.isChecked -> {
                    // No longer sound but vibrate is still active
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(VIBRATE)
                }
                else -> {
                    // Nothing is selected
                    // Save the users setting in SharedPreferences
                    saveFeedbackOptionsToSharedPrefs(NONE)
                }
            }
        }

        // Spinner controlling options of alert tone
        alertToneSpinner.apply {
            adapter = context.run {
                ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    AlertTone.values()
                )
            }
            onItemSelectedListener = this@SettingsFragment
            alertToneSpinner.setSelection(pullAlertToneFromSharedPreferences(activity).ordinal)
        }

        // Switch controlling No Headphone mode
        noHeadphoneModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNoHeadphoneModeToSharedPrefs(isChecked)
        }

        // Buttons for user to test sound options
        buttonLeft.setOnClickListener {
            if (!pullNoHeadphonesModeFromSharedPreferences(activity)) {
                mediaPlayer =
                    MediaPlayer.create(
                        context,
                        pullAlertToneFromSharedPreferences(activity).soundFile
                    )
                mediaPlayer.setVolume(1F, 0F)
            } else {
                mediaPlayer = MediaPlayer.create(context, R.raw.piano_left)
            }
            mediaPlayer.start()
        }
        buttonCenter.setOnClickListener {
            mediaPlayer = if (!pullNoHeadphonesModeFromSharedPreferences(activity)) {
                MediaPlayer.create(context, pullAlertToneFromSharedPreferences(activity).soundFile)
            } else {
                MediaPlayer.create(context, R.raw.piano_center)
            }
            mediaPlayer.start()
        }
        buttonRight.setOnClickListener {
            if (!pullNoHeadphonesModeFromSharedPreferences(activity)) {
                mediaPlayer =
                    MediaPlayer.create(
                        context,
                        pullAlertToneFromSharedPreferences(activity).soundFile
                    )
                mediaPlayer.setVolume(0F, 1F)
            } else {
                mediaPlayer = MediaPlayer.create(context, R.raw.piano_right)
            }
            mediaPlayer.start()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).apply {
            supportActionBar?.apply {
                this.title = resources.getString(R.string.feedback_settings_title)
            }
        }
    }

    /*
    *   Set the switches to be checked based on what the saved options are
    *  */
    private fun setUpOptionsFromSharedPrefs(
        vibrateSwitch: SwitchMaterial,
        soundSwitch: SwitchMaterial,
        vibrateIcon: ImageView,
        soundIcon: ImageView,
        alertToneSpinner: Spinner,
        noHeadphoneSwitch: SwitchMaterial,
        noHeadphoneSuggestion: TextView
    ) {
        when (pullFeedbackOptionFromSharedPreferences(activity)) {
            VIBRATE -> {
                // Only the vibrate option should be selected
                vibrateSwitch.isChecked = true
                soundSwitch.isChecked = false
            }
            SOUND -> {
                // Only the sound option should be selected
                vibrateSwitch.isChecked = false
                soundSwitch.isChecked = true
            }
            BOTH -> {
                // Both the vibrate and sound should be selected
                vibrateSwitch.isChecked = true
                soundSwitch.isChecked = true
            }
            NONE, NEWUSER -> {
                // Nothing selected
                vibrateSwitch.isChecked = false
                soundSwitch.isChecked = false
            }
        }

        when (pullAlertToneFromSharedPreferences(activity)) {
            AlertTone.Beep -> {
                alertToneSpinner.prompt = AlertTone.Beep.name
            }
            AlertTone.Jazz -> {
                alertToneSpinner.prompt = AlertTone.Jazz.name
            }
            AlertTone.Snippy -> {
                alertToneSpinner.prompt = AlertTone.Snippy.name
            }
            AlertTone.Voice -> {
                alertToneSpinner.prompt = AlertTone.Voice.name
            }
        }

        noHeadphoneSwitch.isChecked = pullNoHeadphonesModeFromSharedPreferences(activity)

        // Set the icon color based on if the switch is on or off
        vibrateIcon.setAsAccentColor(vibrateSwitch.isChecked)
        soundIcon.setAsAccentColor(soundSwitch.isChecked)

        if (!areHeadphonesConnected() || soundSwitch.isChecked) {
            noHeadphoneSuggestion.visibility = View.VISIBLE
        }
    }

    /*
    *  Disable the sound sub-options when the sound switch is turned off
    *  */
    private fun enableOrDisableSoundSettings(isChecked: Boolean) {
        sound_icon_image_view.setAsAccentColor(isChecked)
        settings_feedback_alert_tone_spinner.isEnabled = isChecked
        settings_fragment_no_headphone_mode_switch.isEnabled = isChecked
        settings_feedback_sound_test_left_side.isEnabled = isChecked
        settings_feedback_sound_test_center.isEnabled = isChecked
        settings_feedback_sound_test_right_side.isEnabled = isChecked
        if (!isChecked) {
            settings_feedback_select_alert_tone_title.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            settings_feedback_no_headphones_mode_title.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            settings_feedback_sound_test_title.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            settings_feedback_no_headphone_suggestion.visibility = View.INVISIBLE
        } else {
            settings_feedback_select_alert_tone_title.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            settings_feedback_no_headphones_mode_title.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            settings_feedback_sound_test_title.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    /*
    *   Save the user selected option in SharedPreferences and shows a confirmation Snackbar, If the
    *   option that is selected already is saved, do not show the Snackbar
    * */
    private fun saveFeedbackOptionsToSharedPrefs(feedbackOption: FeedbackOption) {
        val sharedPrefs = activity?.getSharedPreferences(
            FEEDBACK_SHARED_PREF_KEY,
            Context.MODE_PRIVATE
        ) ?: return

        // Get previously saved option, in future we could have this also "undo" the change
        val previouslySavedOption = pullFeedbackOptionFromSharedPreferences(activity)

        // Save selected option to SharedPreferences
        with(sharedPrefs.edit()) {
            putString(FEEDBACK_KEY, feedbackOption.saveKey)
            commit()
        }

        // Snack bar to tell the user that the selected option was saved
        val snackBar =
            activity?.container?.let {
                Snackbar.make(
                    it,
                    feedbackOption.snackBarMessage,
                    Snackbar.LENGTH_SHORT
                )
            }?.apply {
                setTextColor(Color.WHITE)
                animationMode = Snackbar.ANIMATION_MODE_FADE
                duration = Snackbar.LENGTH_LONG
            }

        snackBar?.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        if (previouslySavedOption != feedbackOption) {
            // Only show the snackBar if a new option has been selected
            snackBar?.show()
        }
    }

    /*
    *   Save the user selected option in SharedPreferences and shows a confirmation Snackbar, If the
    *   option that is selected already is saved, do not show the Snackbar
    * */
    private fun saveSoundOptionToSharedPrefs(alertTone: AlertTone) {
        val sharedPrefs = activity?.getSharedPreferences(
            FEEDBACK_SHARED_PREF_KEY,
            Context.MODE_PRIVATE
        ) ?: return

        // Save selected option to SharedPreferences
        with(sharedPrefs.edit()) {
            putString(ALERT_TONE_KEY, alertTone.saveKey)
            commit()
        }
    }

    /*
    *   Save the no headphone mode to SharedPreferences
    * */
    private fun saveNoHeadphoneModeToSharedPrefs(isChecked: Boolean) {
        val sharedPrefs = activity?.getSharedPreferences(
            FEEDBACK_SHARED_PREF_KEY,
            Context.MODE_PRIVATE
        ) ?: return

        // Save selected option to SharedPreferences
        with(sharedPrefs.edit()) {
            putBoolean(NO_HEADPHONE_MODE, isChecked)
            commit()
        }
    }

    /*
    *   Possible feedback options, These are controlled by the switches for Vibrate and Sound
    * */
    enum class FeedbackOption(val saveKey: String, val snackBarMessage: String) {
        VIBRATE("vibrate", "Vibration only has been saved"),
        SOUND("sound", "Only sound option has been saved"),
        BOTH("both", "Both vibration and sound has been saved"),
        NONE("none", "No feedback saved"),
        NEWUSER("newUser", "")
    }

    /*
    * Possible alert tones the user can select
    * */
    enum class AlertTone(val saveKey: String, val soundFile: Int) {
        Beep("beep", R.raw.alert_beep),
        Jazz("jazz", R.raw.alert_jazz),
        Snippy("snippy", R.raw.alert_snippy),
        Voice("voice", R.raw.alert_voice)
    }

    /*
    *   Extension function to change the color of the icon
    * */
    private fun ImageView.setAsAccentColor(active: Boolean) {
        if (active) {
            this.setColorFilter(resources.getColor(R.color.colorPrimaryPurple, null))
        } else {
            this.setColorFilter(Color.BLACK)
        }
    }

    /*
    *  Check if the headphones are connected
    * */
    private fun areHeadphonesConnected(): Boolean {
        val audioManager: AudioManager =
            requireContext().getSystemService(AUDIO_SERVICE) as AudioManager
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)

        for (deviceInfo in audioDevices) {
            if (deviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                || deviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || deviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            ) {
                return true
            }
        }

        return false
    }

    // required override for spinner
    override fun onNothingSelected(parent: AdapterView<*>?) {
        // empty on purpose
    }

    // What to do when a sound is chosen from the spinner
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedSound: AlertTone = parent?.getItemAtPosition(position) as AlertTone
        saveSoundOptionToSharedPrefs(selectedSound)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    companion object {
        const val FEEDBACK_SHARED_PREF_KEY: String = "settings.settings_feedback_key_"
        const val FEEDBACK_KEY: String = "settings.feedback_key"
        const val ALERT_TONE_KEY: String = "settings.settings_alert_tone_key"
        const val NO_HEADPHONE_MODE: String = "settings.settings_no_headphones_mode"

        /*
        *   This can be used throughout the app to verify what the current selected options are
        * */
        fun pullFeedbackOptionFromSharedPreferences(activity: Activity?): FeedbackOption? {
            val sharedPrefs =
                activity?.getSharedPreferences(FEEDBACK_SHARED_PREF_KEY, Context.MODE_PRIVATE)
                    ?: return null

            // Using "NEWUSER" as default
            val savedOptionKey = sharedPrefs.getString(FEEDBACK_KEY, NEWUSER.saveKey)

            // Return the feedback option
            for (option in values()) {
                if (option.saveKey == savedOptionKey) {
                    return option
                }
            }

            // This should not get hit since the default value is set when pulling shared prefs
            return null
        }

        /*
        *  This can be used to pull the users selected alert tone.
        * */
        fun pullAlertToneFromSharedPreferences(activity: Activity?): AlertTone {
            val sharedPrefs =
                activity?.getSharedPreferences(FEEDBACK_SHARED_PREF_KEY, Context.MODE_PRIVATE)
                    ?: return AlertTone.Beep

            val savedAlertTone = sharedPrefs.getString(ALERT_TONE_KEY, AlertTone.Beep.saveKey)

            for (tone in AlertTone.values()) {
                if (tone.saveKey == savedAlertTone) {
                    return tone
                }
            }

            // This should not get hit But if it does, well use Beep as default
            return AlertTone.Beep
        }

        /*
        *  This can be used to see if the user is using NoHeadphoneMode
        * */
        fun pullNoHeadphonesModeFromSharedPreferences(activity: Activity?): Boolean {
            val sharedPrefs =
                activity?.getSharedPreferences(FEEDBACK_SHARED_PREF_KEY, Context.MODE_PRIVATE)
                    ?: return false

            // Default value is false
            return sharedPrefs.getBoolean(NO_HEADPHONE_MODE, false)
        }
    }
}