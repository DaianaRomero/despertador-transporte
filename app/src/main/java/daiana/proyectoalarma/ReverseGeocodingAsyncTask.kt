package daiana.proyectoalarma

import android.location.Address
import android.os.AsyncTask
import android.util.Log
import org.osmdroid.bonuspack.location.GeocoderNominatim
import java.lang.Exception


class ReverseGeocodingAsyncTask(n:String, m:Int) : AsyncTask<String, Int, ArrayList<Address>>() {

    private var nombre:String = n
    private var maxRes:Int = m

    override fun doInBackground(vararg params: String?) : ArrayList<Address>{
        val direccionEncontrada = ArrayList<Address>()
        val geocoderNominatim = GeocoderNominatim("name=Daiana Romero,email=daianaar94@gmail.com,app=alarma_transporte")
        geocoderNominatim.setService("https://nominatim.openstreetmap.org/")
        try{
            direccionEncontrada.addAll(geocoderNominatim.getFromLocationName(nombre, maxRes))
            return direccionEncontrada
        }
        catch(e: Exception){
            Log.e("ReverseGeocodingAsyncTask", e.toString())
        }
        return direccionEncontrada
    }
}