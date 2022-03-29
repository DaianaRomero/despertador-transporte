package daiana.proyectoalarma

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.media.*
import android.net.Uri
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.tbruyelle.rxpermissions2.RxPermissions
import gstavrinos.destinationalarm.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

class MainActivity : AppCompatActivity(){
    private var configuracion:SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    private var superposicionDeUbicacion:MyLocationNewOverlay? = null
    private var notification: Uri? = null
    private var mConnection: ServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        main = this
        Configuration.getInstance().load(main, PreferenceManager.getDefaultSharedPreferences(main))
        setContentView(R.layout.activity_main)//inflar y crear el mapa


        //Notificaciones de la app
        val intencionPendiente: PendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                }
        daiana.proyectoalarma.notification = NotificationCompat.Builder(this, createNotificationChannel())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Alarma de transporte")//notificacioes de segundo plano
                .setContentText("¡La alarma sigue funcionando!")
                .setContentIntent(intencionPendiente)
                .setTicker("Alarma de transporte")
                .build()
        daiana.proyectoalarma.notification!!.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

      /* configuracion = getSharedPreferences("destinationAlarmU.P", Context.MODE_PRIVATE)
       @SuppressLint("CommitPrefEdits")
        editor = configuracion!!.edit()*/

        //ringtone
        val sonido = configuracion!!.getString("alarm_sound", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())
        val archivoAlarma = File(sonido)
        notification = if (!archivoAlarma.exists()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else Uri.parse(sonido)

        sonidos = RingtoneManager.getRingtone(main, notification)
        sonidos!!.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build()



        minDistancia = configuracion!!.getInt("minDist", 1000)
        vibracion = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


                                /*visualizacion del mapa*/
        mapa = findViewById(R.id.mapview)//trae el recurso de la interfaz
        mapa!!.setTileSource(TileSourceFactory.MAPNIK)
        mapa!!.setBuiltInZoomControls(true)//habilitar zoom
        mapa!!.setMultiTouchControls(true)//permite movimiento tactil
        mapa!!.isTilesScaledToDpi = true
        mapa!!.isFlingEnabled = true
        mapa!!.minZoomLevel = 5.0
        mapa!!.setUseDataConnection(!configuracion!!.getBoolean("offline_mode", false))

        val botonConfig:ImageButton = findViewById(R.id.boton_configuracion)

        botonConfig.setOnClickListener {
            mostrarConfigEmergente()
        }

        area = SinPoligono(mapa)
       // Podemos mover el mapa a un punto de vista predeterminado. Para esto, necesitamos acceso
        // al controlador de mapas:
        val controladorMapa = mapa!!.controller
        controladorMapa.setZoom(15.0)
        val puntoDePartida = GeoPoint(-34.601325197684744,-58.41811515989849)
        controladorMapa.setCenter(puntoDePartida)

        //Usando MyLocationNewOverlay puedo mostrar la ubicación actual del usuario en mi MapView.
        superposicionDeUbicacion = MyLocationNewOverlay(GpsMyLocationProvider(this), mapa)
        superposicionDeUbicacion!!.enableMyLocation()//Habilite la recepción de actualizaciones de ubicación desde el IMyLocationProvider proporcionado y muestre su ubicación en los mapas.
        superposicionDeUbicacion!!.enableFollowLocation()//Habilita la funcionalidad "seguir".
        superposicionDeUbicacion!!.isOptionsMenuEnabled = true
        mapa!!.overlays.add(superposicionDeUbicacion)


        //
        marcadorDeDestino = Marker(mapa)
        marcadorDeDestino!!.icon =  resources.getDrawable(R.drawable.map_marker_icon, this.theme)
       //Superposición vacía que se puede usar para detectar eventos en el mapa y enviarlos a MapEventsReceiver.
        val mapaSuperpDeEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun longPressHelper(p: GeoPoint?): Boolean {
                agregarMarcadorDestino(p!!.latitude, p.longitude)
                return true
            }

            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return true
            }

        })

        mapa!!.overlays.add(0, mapaSuperpDeEventos)

        val btCentrarMapa = findViewById<ImageButton>(R.id.ic_center_map)

        btCentrarMapa.setOnClickListener {
            superposicionDeUbicacion!!.enableFollowLocation()//Habilita la funcionalidad "seguir"
        }

        rxPermissions = RxPermissions(this)
        mConnection = object : ServiceConnection {

            override fun onServiceConnected(className: ComponentName, service: IBinder) {
            }

            override fun onServiceDisconnected(className: ComponentName) {
            }
        }

        val receptorDeVibracion = object: BroadcastReceiver() {
            override fun onReceive(context:Context, intent:Intent) {
                if(intent.action == Intent.ACTION_SCREEN_OFF && vibrating) {
                    vibrar()
                }
            }
        }

   /*     Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().tileDownloadThreads = 12
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 10000000000 // 10TB*/

        val filtro = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receptorDeVibracion, filtro)//Acción de transmisión: se envía cuando el dispositivo entra en suspensión y deja de ser interactivo

        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val serviceIntent = Intent(this, LocationService().javaClass)
        bindService(serviceIntent, mConnection!!, Context.BIND_AUTO_CREATE)

    }

    public override fun onResume() {
        super.onResume()
       /*  esto actualizará la configuración de osmdroid al reanudar.
         si realiza cambios en la configuración, utilice
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (this);
         Configuration.getInstance (). Load (this, PreferenceManager.getDefaultSharedPreferences (esto));
        map.onResume (); // necesario para brújula, superposiciones de mi ubicación, v6.0.0 y posteriores*/
    }

    public override fun onPause() {
        super.onPause()
       /*  esto actualizará la configuración de osmdroid al reanudar.
         si realiza cambios en la configuración, utilice
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences (this);
         Configuration.getInstance (). Save (this, prefs);
       map.onPause ();   necesario para brújula, superposiciones de mi ubicación, v6.0.0 y posteriores*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == -1 && requestCode == 5) {
            val tmp:Uri? = data!!.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (tmp != null) {
                notification = tmp
                editor!!.putString("alarm_sound", notification.toString())
                editor!!.apply()
                sonidos = RingtoneManager.getRingtone(main, notification)
                sonidos!!.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build()
                Toast.makeText(main, "Nueva alarma elegida!", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(main, "Error!", Toast.LENGTH_LONG).show()
            }
        }
        if(requestCode == 1){
            if(!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                mostrarNotifGPS()
            }
            else{
                recreate()
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        unbindService(mConnection!!)
    }


    fun vibrar(){
        vibrating = true
        if (Build.VERSION.SDK_INT >= 26) {
            vibracion!!.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 1000), 1))
        }
        else {
            @Suppress("error")
            vibracion!!.vibrate(360000000) // 100 horas
        }
    }
            /*todo lo referido a la configuracion*/
    private fun mostrarConfigEmergente() {

        val vistaEmergente:View  = layoutInflater.inflate(R.layout.configuracion, null)
        val ventanaEmergente = PopupWindow(vistaEmergente, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ventanaEmergente.isFocusable = true
        val radioValor = vistaEmergente.findViewById<TextView>(R.id.radius_value)
        val barraDeDistancia = vistaEmergente.findViewById<SeekBar>(R.id.radius_seekBar)
        barraDeDistancia.progress = minDistancia
        radioValor.text = minDistancia.toString()

        barraDeDistancia.setOnSeekBarChangeListener(object:OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar:SeekBar) {
                minDistancia = seekBar.progress
                editor!!.clear()
                editor!!.putInt("minDist", minDistancia)
                editor!!.apply()
            }

            override fun onStartTrackingTouch(seekBar:SeekBar) {}

            override fun onProgressChanged(seekBar:SeekBar, progress:Int,fromUser:Boolean) {
                radioValor.text = (progress+20).toString()
            }
        })

                    //ringtone de alARMA//
        val RingtoneBoton = vistaEmergente.findViewById<Button>(R.id.alarm_sound_button)
        RingtoneBoton.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                val sndmngtIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                sndmngtIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                sndmngtIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecciona la alarma por favor")
                sndmngtIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, notification)
                sndmngtIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                startActivityForResult(sndmngtIntent, 5)
            }

        })

        val SeleccionDeRingtone: RadioGroup = vistaEmergente.findViewById(R.id.sound_source_selection)
        if(configuracion!!.getBoolean("useSpeaker", true)){
            SeleccionDeRingtone.check((R.id.speaker_radio))
        }
        else{
            SeleccionDeRingtone.check((R.id.headphones_radio))
        }

        SeleccionDeRingtone.setOnCheckedChangeListener(object: RadioGroup.OnCheckedChangeListener{
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
                editor!!.putBoolean("useSpeaker", checkedId == R.id.speaker_radio)
                editor!!.apply()
            }

        })


        val bg = ColorDrawable(0xCC333333.toInt())
        ventanaEmergente.setBackgroundDrawable(bg)
        ventanaEmergente.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)

    }


    fun mostrarNotifGPS(){
        val builder = AlertDialog.Builder(main)
        builder.setTitle("¡GPS no habilitado!")
                .setMessage("La aplicación requiere GPS para funcionar correctamente. Presione ok para continuar con la configuración para habilitar el GPS o cancelar para salir.\n")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val locIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(locIntent, 1)
                }
                .setNegativeButton(android.R.string.no) { _, _ ->
                    finish()
                }.setIcon(android.R.drawable.ic_dialog_alert)
                .setOnCancelListener {
                    builder.show()
                }
                .show()
    }

    private fun agregarMarcadorDestino(lat:Double, lon:Double){
        val p = GeoPoint(lat, lon)
        mapa!!.overlays.remove(area)
        marcadorDeDestino!!.position = p
        val oml = Marker.OnMarkerClickListener { _, _ ->
            true
        }
        marcadorDeDestino!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marcadorDeDestino!!.setOnMarkerClickListener(oml)
        area.points = Polygon.pointsAsCircle(p, minDistancia.toDouble())
        area.fillColor = 0x12121212
        area.strokeColor = Color.YELLOW
        area.strokeWidth = 2.0f
        mapa!!.overlays.add(marcadorDeDestino)
        mapa!!.overlays.add(area)
        mapa!!.invalidate()
        check = true
        Toast.makeText(main, "¡Nuevo destino de alarma establecido!", Toast.LENGTH_SHORT).show()
    }

   private fun createNotificationChannel(): String{
        var channelId = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "Alarma de transporte!"
            val channelName = "Alarma de transporte"
            val chan = NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }
        return channelId
    }

}
