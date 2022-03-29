package daiana.proyectoalarma

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

class SinPoligono(map: MapView?) : Polygon(map) {
//Un polígono (Polygon) es una forma que consiste en una serie de coordenadas en una secuencia
// ordenada, similar a una polilínea (Polyline). La diferencia radica en que el polígono define
// un área cerrada con un interior que se puede rellenar, mientras que una polilínea tiene extremos
// abiertos.
    ///Un polígono en la superficie terrestre. Un polígono puede ser convexo o cóncavo, puede abarcar el meridiano 180 y puede tener huecos sin rellenar. Tiene las siguientes propiedades:
    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        return false
    }
}