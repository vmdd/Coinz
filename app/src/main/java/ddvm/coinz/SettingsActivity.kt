package ddvm.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        autocollection_switch.isChecked = Utils.getAutocollectionState(this)
        autocollection_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            Utils.saveAutocollectionState(this, isChecked)
        }
    }
}
