package com.alexisserapio.persistencia

import EncryptedPrefs
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.alexisserapio.persistencia.model.Settings
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alexisserapio.persistencia.Extensions.dataStore
import com.alexisserapio.persistencia.databinding.ActivityMainBinding
import com.alexisserapio.persistencia.sharedPreferences.Constants
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: Fragment
    //Para las shared Preferences
    private lateinit var sp: SharedPreferences

    //Para el cifrado
    private lateinit var aead: Aead
    private lateinit var encryptedPrefs: EncryptedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as Fragment
        //Instanciamos las sharedPreferences
        sp = getSharedPreferences(Constants.SP_FILE, Context.MODE_PRIVATE)

        //Instanciar crypto
        aead = TinkAeadProvider.aead(this)
        encryptedPrefs = EncryptedPrefs(sp, aead)

        //val bgColor = sp.getInt(Constants.BG_COLOR, R.color.black)
//        val bgColor = encryptedPrefs.getInt(Constants.BG_COLOR, R.color.black)
//        val name = sp.getString("name", "No hay un archivo Guardado")
//        Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
//        navHostFragment.view?.setBackgroundColor(getColor(bgColor))

        lifecycleScope.launch {
            readSettingsDS().collect { settings ->
                navHostFragment.view?.setBackgroundColor(getColor(settings.bgColor))
                Log.d(Constants.LOGTAG, "Usuario con dataStore: ${settings.user}")
            }
        }

        //Encriptando cualquier cadena
        val textoACifrar = "Te amo Sandrita <3"
        val textoCifrado = aead.encrypt(
            textoACifrar.toByteArray(Charsets.UTF_8),
            "user:name".toByteArray()
        )

        val textoDescifrado = aead.decrypt(textoCifrado, "user:name".toByteArray())

        Log.d(Constants.LOGTAG, "Texto cifrado con tink: ${textoCifrado.toString(Charsets.UTF_8)}")
        Log.d(Constants.LOGTAG, "Texto descifrado con tink: ${textoDescifrado.toString(Charsets.UTF_8)}")

        binding.fab.setOnClickListener { view ->
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()*/
            sp.edit {
                putString("name","Alexis Arturo Serapio Hernández")
            }
            Toast.makeText(this, "Nombre Guardado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_red -> {
                changeColor(R.color.my_red)
                true
            }
            R.id.action_blue -> {
                changeColor(R.color.my_blue)
                true
            }
            R.id.action_green -> {
                changeColor(R.color.my_green)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun changeColor(@ColorRes color: Int) {//Color Res Es para permitir solo enteros que pertencen a un id de color en la carpeta res.
        navHostFragment.view?.setBackgroundColor(getColor(color))
        saveColor(color)
    }

    private fun saveColor(@ColorRes color: Int){
        /*sp.edit().putInt(Constants.BG_COLOR, color)
        sp.edit().putString("user", "amaurybb").apply()*///En el ultimo valor almacenado se ccoloca el .apply()

        //La buena, el de arriba quien sabe si funcione
        /*sp.edit {
            putInt(Constants.BG_COLOR, color)
            putString("user","Amaury")
        }*/

        //Guardando con encriptación
        //encryptedPrefs.putInt(Constants.BG_COLOR, color)

        //Esta ligado al ciclo de vida, pero permite guardar de forma asincrona
        //Co-rutinas *buscar*
        lifecycleScope.launch {
            saveColorDS(color)
        }

    }

    private suspend fun saveColorDS(@ColorRes color:Int){
        //La flecha indica que es una funcion suspendida, solo se puede llamar en una co-rutina o en otra funcion suspendida
        //Guardar en Asincrono permite que la UI siga activa mientras se almacena la información
        //Con suspend, se puede ocupar los recursos en otra tarea y se reanuda después
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(Constants.BG_COLOR)] = color
            preferences[stringPreferencesKey("user")] = "Alexis"
        }
    }
    //La evolución de las sharedPreferences
    private fun readSettingsDS() =
        dataStore.data.map { preferences ->
            Settings(
                preferences[intPreferencesKey(Constants.BG_COLOR)] ?: R.color.black,
                preferences[stringPreferencesKey("user")].orEmpty()
            )
            //preferences[intPreferencesKey(Constants.BG_COLOR)] ?: R.color.black
            //Así como esta el code no lee las dos llaves
            //preferences[stringPreferencesKey("user")].orEmpty()
        }

}