//package com.example.schedo.ui
//
//import android.content.Context
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
//import com.google.api.client.http.FileContent
//import com.google.api.client.http.javanet.NetHttpTransport
//import com.google.api.client.json.gson.GsonFactory
//import com.google.api.services.drive.Drive
//import com.google.api.services.drive.DriveScopes
//import com.google.api.services.drive.model.File
//import java.io.File as JavaFile
//
//suspend fun uploadFileToDrive(
//    context: Context,
//    credential: GoogleAccountCredential,
//    fileUri: android.net.Uri,
//    fileName: String
//): String? {
//    return try {
//        val driveService = Drive.Builder(
//            NetHttpTransport(),
//            GsonFactory.getDefaultInstance(),
//            credential
//        ).setApplicationName("Schedo").build()
//
//        // Membuat metadata file
//        val fileMetadata = File().apply {
//            name = fileName
//            // Membuat file publik
//            set("permissions", listOf(mapOf("type" to "anyone", "role" to "reader")))
//        }
//
//        // Mendapatkan file dari URI
//        val inputStream = context.contentResolver.openInputStream(fileUri)
//        val tempFile = JavaFile(context.cacheDir, fileName)
//        inputStream?.use { input ->
//            tempFile.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//
//        val mediaContent = FileContent(getMimeType(fileName), tempFile)
//
//        // Mengunggah file ke Google Drive
//        val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
//            .setFields("id, webViewLink")
//            .execute()
//
//        // Mengembalikan URL publik
//        uploadedFile.webViewLink.also {
//            tempFile.delete() // Hapus file sementara
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    }
//}