package de.jackBeBack.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import kotlin.math.*
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import de.jackBeBack.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableRotation
import ovh.plrapps.mapcompose.api.maxScale
import ovh.plrapps.mapcompose.api.rotateTo
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setPreloadingPadding
import ovh.plrapps.mapcompose.api.snapScrollTo
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class MapViewModel(): ViewModel() {
    val client = HttpClient(CIO)
    lateinit var cacheDir: File

    companion object{
        private var instance: MapViewModel? = null

        fun getInstance(cacheDir: File): MapViewModel {
            if (instance == null){
                instance = MapViewModel().apply { this.cacheDir = cacheDir }
            }
            return instance!!
        }
    }

    init {
        if (instance == null){
            instance = this
        }
    }

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation:Flow<Location?> = _currentLocation


    fun getLastLocation(context: Context) {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener{
            _currentLocation.tryEmit(it)
            scrollTo(it)
        }.addOnFailureListener {
            Log.d("getLastLocation", it.toString())
        }
    }

    val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
        viewModelScope.async(Dispatchers.IO) {
            loadTile(row, col, zoomLvl)
        }.await()
    }
    val MAX_RETRY = 5

    suspend private fun loadTile(
        row: Int,
        col: Int,
        zoomLvl: Int,
        retry: Int = 0,
        caching: Boolean = true
    ): InputStream {
        val fileName = "tile_${zoomLvl}_${row}_$col.png"
        val localFile = File(cacheDir, fileName)

        return if (localFile.exists() && caching) {
            FileInputStream(localFile)
        } else {
            try {
                Log.d("tileStreamProvider", "Loading(x, y, zoom) -> ($row, $col, $zoomLvl)")
                val url = "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"
                val response: HttpStatement = client.get(url)
                response.execute {
                    if (caching){
                        it.content.copyToFile(localFile)
                        localFile.inputStream()
                    }else{
                        it.content.toInputStream()
                    }

                }
            } catch (e: ClientRequestException) {
                if (retry <= MAX_RETRY) {
                    loadTile(row, col, zoomLvl, retry + 1, caching)
                } else {
                    //when requesting wrong tile return empty File input stream
                    Log.d("loadTile", "ClientRequestException -> ($row, $col, $zoomLvl, ${retry + 1}, $caching)")
                    val tempFile = File.createTempFile("temp", null)
                    tempFile.deleteOnExit()
                    FileInputStream(tempFile)
                }
            }
        }
    }

    suspend fun ByteReadChannel.copyToFile(file: File) {
        file.outputStream().use { output ->
            this.copyTo(output)
        }
    }
    fun normalizeLatitude(latitude: Double): Double {
        return (1.0/180) * (latitude + 90)
    }

    fun normalizeLongitude(longitude: Double): Double {
        return (1.0/360) * (longitude + 180)
    }

    fun scrollTo(location: Location){
        val x = normalizeLongitude(location.longitude)
        val y = normalizeLatitude(location.latitude)

        val zoomLvl = scaleToZoom(state.scale)

        val maxWidth = 2.0.pow(19) - 1

        val tilePos = getXYTile(location.latitude, location.longitude, 19)
        state

        viewModelScope.launch {
            state.scrollTo(x, y)
            AddMarker(x, y)
            state.rotateTo(0f, TweenSpec(2000, easing = FastOutSlowInEasing))
        }
    }

    fun getXYTile(lat : Double, lon: Double, zoom : Int) : Pair<Int, Int> {
        val latRad = Math.toRadians(lat)
        var xtile = floor( (lon + 180) / 360 * (1 shl zoom) ).toInt()
        var ytile = floor( (1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom) ).toInt()

        if (xtile < 0) {
            xtile = 0
        }
        if (xtile >= (1 shl zoom)) {
            xtile= (1 shl zoom) - 1
        }
        if (ytile < 0) {
            ytile = 0
        }
        if (ytile >= (1 shl zoom)) {
            ytile = (1 shl zoom) - 1
        }

        return Pair(xtile, ytile)
    }

    fun scaleToZoom(scale: Float): Int {
        val minScale = 0.0
        val maxScale = 1.0

        val minZoom = 0
        val maxZoom = 19

        val zoom = (((scale - minScale) / (maxScale - minScale)) * (maxZoom - minZoom) + minZoom).toInt()
        return zoom
    }

    fun clearCache(context: Context) {
        try {
            val dir: File = context.cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success: Boolean = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else if(dir!= null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }

    fun AddMarker(x: Double, y: Double){
        viewModelScope.launch {
            state.addMarker("id", x = x, y = y) {
                Icon(
                    painter = painterResource(id = R.drawable.map_marker),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xCC2196F3)
                )
            }
        }
    }

    val localtileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
        FileInputStream(File("path/{$zoomLvl}/{$row}/{$col}.jpg")) // or it can be a remote HTTP fetch
    }

    private val mapSize = 268435456
    val state: MapState by mutableStateOf(
        MapState(20, mapSize, mapSize, workerCount = Runtime.getRuntime().availableProcessors()/2).apply {
            addLayer(tileStreamProvider)
            enableRotation()
            scale = 0.0001f
        }
    )
}