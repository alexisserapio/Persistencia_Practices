package com.alexisserapio.persistencia

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.alexisserapio.persistencia.sharedPreferences.Constants

object Extensions {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = Constants.DS_FILE
    )
}