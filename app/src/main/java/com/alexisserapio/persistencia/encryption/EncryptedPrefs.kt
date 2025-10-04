import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import android.util.Base64
import androidx.core.content.edit
import com.google.crypto.tink.Aead

//Implementación de un Wrapper de SharedPreferences
//Cifrado y Descifrado de valores con Tink (AEAD)
class EncryptedPrefs (
    private val sp: SharedPreferences, // prefs "planas" para almacenar
    private val aead: Aead, //primitiva de Tink para encriptar/desencriptar
    private val aadNamespace: String = "prefs" // se “ata” cada valor a su clave
){

    //La AAD (Associated Authenticated Data) ata cada valor a su clave y a un namespace,
    private fun aad(key: String) = (aadNamespace + ":" + key).toByteArray(StandardCharsets.UTF_8)

    //La AAD es como una etiqueta que no es secreta, pero se usa para atar el cifrado a un contexto
    //Son bytes adicionales que no se cifran, pero sí se autentican junto con el mensaje
    //Si no se usa el AAD con el que se cifró, no se puede descifrar exitosamente
    //Ejemplos de AAD: "prefs:token", "tabla:usuarios", "versión:1"

    fun putString(key: String, value: String?) {
        sp.edit {
            if (value == null) remove(key) else {
                // Cifra el texto plano usando AEAD y AAD específica de la clave.
                val ct = aead.encrypt(value.toByteArray(StandardCharsets.UTF_8), aad(key))
                // Base64 sin saltos de línea (NO_WRAP) para que sea un valor "limpio".
                putString(key, Base64.encodeToString(ct, Base64.NO_WRAP))
            }
        }
    }

    fun getString(key: String, default: String? = null): String? {
        val b64 = sp.getString(key, null) ?: return default
        return try {
            // Decodifica Base64 y descifra con la MISMA AAD usada al cifrar.
            val pt = aead.decrypt(Base64.decode(b64, Base64.NO_WRAP), aad(key))
            String(pt, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Si hubo error o llave no disponible, regresa default.
            default
        }
    }

    fun putInt(key: String, value: Int) = putString(key, value.toString())
    fun getInt(key: String, default: Int = 0) = getString(key)?.toIntOrNull() ?: default

    fun putLong(key: String, value: Long) = putString(key, value.toString())
    fun getLong(key: String, default: Long = 0L) = getString(key)?.toLongOrNull() ?: default

    fun putBoolean(key: String, value: Boolean) = putString(key, if (value) "1" else "0")
    fun getBoolean(key: String, default: Boolean = false) = when (getString(key)) {
        "1" -> true; "0" -> false; else -> default
    }

    fun putFloat(key: String, value: Float) = putString(key, value.toString())
    fun getFloat(key: String, default: Float = 0f) = getString(key)?.toFloatOrNull() ?: default
}