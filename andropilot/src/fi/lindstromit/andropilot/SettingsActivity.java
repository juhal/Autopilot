package fi.lindstromit.andropilot;

import java.util.ArrayList;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		fillHelmIdPref();		
	}
	
	private void fillHelmIdPref() {
		ListPreference helmId = (ListPreference)findPreference("helmId");
		ArrayList<CharSequence> entryList = new ArrayList<CharSequence>();
		BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
		if (bta != null) {
			Set<BluetoothDevice> devs = bta.getBondedDevices();
			for (BluetoothDevice dev : devs) {
				entryList.add(dev.getName());
			}
		}
		CharSequence entries[] = entryList.toArray(
				new CharSequence[entryList.size()]);
		CharSequence values[] = entries.clone();
		helmId.setEntries(entries);
		helmId.setEntryValues(values);				
	}
}
