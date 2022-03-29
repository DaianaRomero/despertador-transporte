package daiana.proyectoalarma

import android.app.Notification
import android.location.LocationManager
import android.media.Ringtone
import android.os.Vibrator
import com.tbruyelle.rxpermissions2.RxPermissions
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

//?: Esto indica que los argumentos que se pasaron para esos parámetros pueden ser nulos.

var mapa: MapView? = null
var locationManager: LocationManager? = null
var sonidos: Ringtone? = null
var marcadorDeDestino: Marker? = null
var minDistancia:Int = 1000
var area = SinPoligono(null)
var check:Boolean = false
var notification: Notification? = null
var vibracion: Vibrator? = null
var vibrating = false
var rxPermissions: RxPermissions? = null
//Kotlin proporciona reglas estrictas de nulabilidad que mantienen la seguridad de tipo en toda tu app.
// En Kotlin, las referencias a objetos no pueden contener valores nulos de forma predeterminada.
// A fin de asignar un valor nulo a una variable, debes declarar un tipo de variable anulable. Para ello,
// agrega ? al final del tipo de base
var main: MainActivity? = null