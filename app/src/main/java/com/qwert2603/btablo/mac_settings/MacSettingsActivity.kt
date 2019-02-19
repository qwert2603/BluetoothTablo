package com.qwert2603.btablo.mac_settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.qwert2603.btablo.R
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.tablo.TabloActivity
import com.qwert2603.btablo.utils.doOnTextChange
import com.qwert2603.btablo.utils.isMacAddress
import kotlinx.android.synthetic.main.activity_mac_settings.*
import kotlinx.android.synthetic.main.item_mac_recent.view.*

class MacSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mac_settings)

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
        toolbar.setNavigationOnClickListener { finish() }

        val macSettings = DIHolder.settingsRepo.macSettings

        mac_EditText.doOnTextChange {
            val correct = it.isMacAddress()
            save_Button.isEnabled = correct

            mac_TextInputLayout.isErrorEnabled = !correct
            mac_TextInputLayout.error = if (correct) {
                null
            } else {
                getString(R.string.text_mac_address_incorrect)
            }

            TransitionManager.beginDelayedTransition(macSettings_LinearLayout)
        }

        mac_EditText.setText(macSettings.mac)

        save_Button.setOnClickListener {
            val newMac = mac_EditText.text.toString()
            DIHolder.settingsRepo.macSettings = macSettings.copy(
                mac = newMac,
                recent = listOf(newMac)
                    .plus(macSettings.recent.take(4))
                    .distinct()
            )

            val intent = Intent(this, TabloActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            System.exit(0)
        }

        recent_TextView.isVisible = macSettings.recent.isNotEmpty()

        val recentAdapter = RecentAdapter(macSettings.recent) {
            mac_EditText.setText(it)
            mac_EditText.setSelection(it.length)
        }
        recent_RecyclerView.adapter = recentAdapter
        recent_RecyclerView.itemAnimator = null
    }
}

private class RecentAdapter(
    private val recent: List<String>,
    private val onMacAddressClicked: (String) -> Unit
) : RecyclerView.Adapter<RecentAdapter.RecentVH>() {

    override fun getItemCount() = recent.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RecentVH(parent)

    override fun onBindViewHolder(holder: RecentVH, position: Int) {
        holder.bind(recent[position])
    }

    private inner class RecentVH(parent: ViewGroup) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_mac_recent, parent, false)) {

        init {
            itemView.setOnClickListener {
                val macAddress = recent.getOrNull(adapterPosition) ?: return@setOnClickListener
                onMacAddressClicked(macAddress)
            }
        }

        fun bind(macAddress: String) {
            itemView.macAddress_TextView.text = macAddress
        }
    }
}