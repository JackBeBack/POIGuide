package de.jackBeBack.ui.composables

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import de.jackBeBack.ui.theme.Green80
import de.jackBeBack.viewmodel.MapViewModel
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.ui.MapUI

@Preview
@Composable
fun MapView() {
    val context = LocalContext.current

    val mapVM = remember {
        MapViewModel.getInstance(context.cacheDir)
    }

    val location = mapVM.currentLocation.collectAsState(initial = null)
    val scroll = mapVM.state.scroll

    Box(Modifier.fillMaxSize()) {
        MapContainer(Modifier.fillMaxSize(), mapVM)
        RequestPermission(Modifier.align(Alignment.TopCenter)){
            Text(text = mapVM.state.scroll.toString() + "  " + mapVM.state.scale.toString())
        }
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .width(70.dp)
        ) {
            Button(onClick = {
                mapVM.getLastLocation(context)
            }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.LocationOn, contentDescription = "")
            }
            Button(onClick = {
                mapVM.clearCache(context)
            }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = "")
            }
        }

    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermission(modifier: Modifier, onSuccess: @Composable () -> Unit) {
    Box(modifier) {
        val permissionState =
            rememberPermissionState(android.Manifest.permission.ACCESS_COARSE_LOCATION)


        val requester =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                permissionState.status
            }

        if (permissionState.status.isGranted) {
            onSuccess()
        } else {
            if (permissionState.status.shouldShowRationale) {
                /* This will be shown if the user denied the permission request in the past, so we should
            * explain why we need it and then request the permission again */
                Text("We need this permission to show your location on the map")
            }

            Button(onClick = {
                requester.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }) {
                Text("Request permission")
            }
        }
    }
}


/* Inside a composable */
@Composable
fun MapContainer(
    modifier: Modifier = Modifier, viewModel: MapViewModel
) {
    MapUI(modifier, state = viewModel.state)
}