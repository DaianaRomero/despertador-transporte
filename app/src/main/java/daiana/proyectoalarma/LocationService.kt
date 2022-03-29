package daiana.proyectoalarma

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.os.IBinder
import android.app.Service
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

//Un Service es un componente de una aplicación que puede realizar operaciones de larga ejecución en
// segundo plano y que no proporciona una interfaz de usuario. Otro componente de la aplicación puede
// iniciar un servicio y continuar ejecutándose en segundo plano aunque el usuario cambie a otra aplicació

class LocationService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        val GPSHabilitado = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

        val disposable = rxPermissions!!.request(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE)
                .subscribe { granted ->
                    if (granted) {
                        if (GPSHabilitado) {
                            if (locationManager != null) {

                                val gpsLocationListener = object : LocationListener {
                                    override fun onLocationChanged(loc: Location) {
                                        if (check) {
                                            val resultados = FloatArray(3)
                                            Location.distanceBetween(loc.latitude, loc.longitude, marcadorDeDestino!!.position.latitude, marcadorDeDestino!!.position.longitude, resultados)
                                            if (resultados[0] <= minDistancia) {
                                                sonidos!!.play()
                                                main!!.vibrar()
                                                mapa!!.overlays.remove(area)
                                                mapa!!.overlays.remove(marcadorDeDestino)
                                                check = false
                                                mapa!!.invalidate()
                                                val builder = AlertDialog.Builder(main)
                                                builder.setTitle("¡DESPIERTA!")
                                                        .setMessage("¿Desea detener la alarma?")
                                                        .setPositiveButton(android.R.string.yes) { dialog, _ ->
                                                            dialog.cancel()
                                                        }.setIcon(android.R.drawable.ic_dialog_alert)
                                                        .setOnCancelListener {
                                                            sonidos!!.stop()
                                                            vibracion!!.cancel()
                                                            vibrating = false
                                                        }
                                                        .show()
                                            }
                                        }
                                    }

                                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                                    override fun onProviderEnabled(provider: String) {}
                                    override fun onProviderDisabled(provider: String) {
                                        main!!.mostrarNotifGPS()
                                    }
                                }
                                try {
                                    locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                            1, 1f,
                                            gpsLocationListener)
                                    startForeground(16, notification)
                                }
                                catch (e: SecurityException) {
                                    // TODO handle this!
                                }
                            }
                        }
                        else {
                            main!!.mostrarNotifGPS()

                        }
                    }
                    else {
                        val builder = AlertDialog.Builder(main)
                        builder.setTitle("¡ERROR DE PERMISOS!")
                                .setMessage("La aplicación no puede funcionar sin los permisos necesarios. ")
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    main!!.finish()
                                }.setIcon(android.R.drawable.ic_dialog_alert)
                                .setOnCancelListener {
                                    main!!.finish()
                                }
                                .show()
                    }
                }
        return null
    }
}