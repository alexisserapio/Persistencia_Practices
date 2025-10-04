import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

object TinkAeadProvider {
    private const val KEYSET_NAME = "tink_prefs_keyset" // Nombre del conjunto de claves que se almacenará en SharedPreferences
    private const val PREF_FILE = "tink_keyset_prefs" // Archivo SharedPreferences donde Tink guardará el keyset (ya CIFRADO).
    private const val MASTER_KEY_URI = "android-keystore://tink_master_key" // Alias/URI de la master key en Android Keystore. Si no existe, Tink la crea automáticamente.

    //AEAD: Authenticated Encryption with Associated Data (Cifrado Autenticado con Datos Asociados)
    fun aead(context: Context): Aead {
        AeadConfig.register()  // Registra las configuraciones/algoritmos AEAD disponibles en Tink.

        // Creamos o recuperamos el keyset asociado a KEYSET_NAME/PREF_FILE.
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE) // keyset cifrado
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate()) // Elegimos la plantilla criptográfica para la clave simétrica del keyset (256 en este caso)
            .withMasterKeyUri(MASTER_KEY_URI) // Indica la master key en el Keystore que se usará para cifrar el keyset
            .build()  // Construye el manejador del conjunto de llaves. Si el keyset no existe lo crea, si existe lo carga.
            .keysetHandle

        // Obtenemos la primitiva criptográfica Aead del conjunto de claves
        return handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}