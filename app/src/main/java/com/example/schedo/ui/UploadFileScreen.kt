//package com.example.schedo.ui
//
//import android.app.Activity
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInClient
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
//import com.google.api.client.json.gson.GsonFactory
//import com.google.api.services.drive.Drive
//import com.google.api.services.drive.model.File
//import java.io.InputStream
//import java.util.Collections
//
//class UploadToDriveActivity : ComponentActivity() {
//    private lateinit var googleSignInClient: GoogleSignInClient
//    private var driveService: Drive? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            UploadFileScreen()
//        }
//    }
//
//    @Composable
//    fun UploadFileScreen() {
//        var selectedUri by remember { mutableStateOf<Uri?>(null) }
//        val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            selectedUri = uri
//        }
//
//        Column(modifier = Modifier.padding(16.dp)) {
//            Button(onClick = { pickFileLauncher.launch("*/*") }) {
//                Text("Pilih File")
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            selectedUri?.let {
//                Text("File dipilih: ${it.path}")
//                Button(onClick = { uploadFileToDrive(it) }) {
//                    Text("Upload ke Google Drive")
//                }
//            }
//        }
//    }
//
//    private fun uploadFileToDrive(uri: Uri) {
//        if (driveService == null) {
//            setupGoogleDrive()
//        }
//
//        val inputStream: InputStream? = contentResolver.openInputStream(uri)
//        val fileMetadata = File().apply {
//            name = "UploadedFile"
//            parents = listOf("root")
//        }
//        val mediaContent = com.google.api.client.http.InputStreamContent("application/octet-stream", inputStream)
//
//        val uploadedFile = driveService?.files()?.create(fileMetadata, mediaContent)
//            ?.setFields("id")
//            ?.execute()
//
//        Log.d("Drive Upload", "File uploaded with ID: ${uploadedFile?.id}")
//    }
//
//    private fun setupGoogleDrive() {
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestEmail()
//            .requestScopes(com.google.api.services.drive.DriveScopes.DRIVE_FILE)
//            .build()
//        googleSignInClient = GoogleSignIn.getClient(this, gso)
//
//        val account = GoogleSignIn.getLastSignedInAccount(this)
//        val credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
//        credential.selectedAccount = account?.account
//
//        driveService = Drive.Builder(
//            GoogleNetHttpTransport.newTrustedTransport(),
//            GsonFactory(),
//            credential
//        ).setApplicationName("Schedo").build()
//    }
//}
