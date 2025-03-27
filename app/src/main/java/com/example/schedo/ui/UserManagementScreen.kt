package com.example.schedo.ui.theme

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.schedo.model.User
import com.example.schedo.network.GroupRequest
import com.example.schedo.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun UserManagementScreen() {
    val coroutineScope = rememberCoroutineScope()
    var users = remember { mutableStateListOf<User>() }
    var newUserName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current  // Mengambil context dari komposabel
    var showAddUser by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current


    fun fetchUsers() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = RetrofitInstance.api.getUsers()
                users.clear()
                users.addAll(response)
                println("Fetched users: $response")
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching users: ${e.message}")
            }
            isLoading = false
        }
    }

    // Memuat data saat pertama kali dibuka
    LaunchedEffect(Unit) {
        fetchUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("User List", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f) // Agar daftar user bisa scroll
            ) {
                items(users) { user ->
                    // Deklarasi onDeleteGroup di luar UserItem
                    val onDeleteGroup: (Int?) -> Unit = { groupId ->
                        groupId?.let { id ->  // Pastikan ID tidak null sebelum eksekusi
                            user.id?.let { userId ->
                                coroutineScope.launch {
                                    try {
                                        println("Menghapus grup dengan ID: $id untuk user ID: $userId")
                                        RetrofitInstance.api.deleteGroup(userId, id)
                                        fetchUsers()
                                    } catch (e: Exception) {
                                        println("Error: ${e.message}")
                                    }
                                }
                            } ?: println("Error: User ID is null")
                        } ?: println("Error: Group ID is null")
                    }

// Gunakan di UserItem
                    UserItem(
                        user = user,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    user.id?.let {
                                        RetrofitInstance.api.deleteUser(it)
                                        fetchUsers()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onDeleteGroup = onDeleteGroup, // Sekarang sesuai dengan tipe (Int?) -> Unit
                        onAddGroup = { name ->
                            coroutineScope.launch {
                                try {
                                    if (name.isNotEmpty()) {
                                        user.id?.let { userId ->
                                            fetchUsers()
                                        }
                                    } else {
                                        println("Nama Grup tidak boleh kosong: $name")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                    Divider()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddUser = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add User")
        }


        if (showAddUser) {
            AlertDialog(
                onDismissRequest = { showAddUser = false },
                title = { Text("Add Group") },
                text = {
                    // Gunakan Column untuk menata elemen-elemen vertikal
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp) // Memberikan jarak antar elemen
                    ) {
                        OutlinedTextField(
                            value = newUserName,
                            onValueChange = { newUserName = it },
                            label = { Text("Enter Name") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done // Menutup keyboard saat "Done" ditekan
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide() // Menutup keyboard saat "Done"
                                }
                            )
                        )

                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("Enter Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done // Menutup keyboard saat "Done" ditekan
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide() // Menutup keyboard saat "Done"
                                }
                            )
                        )

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("Enter Pass") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done // Menutup keyboard saat "Done" ditekan
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide() // Menutup keyboard saat "Done"
                                }
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val newUser = User(name = newUserName, email = newEmail, pass = newPass)
                                    val createdUser = RetrofitInstance.api.createUser(newUser)
                                    println("User created: $createdUser")
                                    newUserName = ""
                                    newEmail = ""
                                    newPass = ""
                                    fetchUsers()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    println("Error creating user: ${e.message}")
                                }
                            }
                        },
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator() // Menampilkan indikator loading saat mengirim request
                        } else {
                            Text("Tambah User")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddUser = false }) {
                        Text("Cancel")
                    }
                }
            )
        }


    }
}


@Composable
fun UserItem(user: User, onDelete: () -> Unit, onAddGroup: (String) -> Unit, onDeleteGroup: (Int?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Tambahkan state loading untuk menonaktifkan tombol saat proses pengiriman
    var isLoading by remember { mutableStateOf(false) }

    // State untuk menangani pesan kesalahan
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // State yang diupdate agar memastikan status yang selalu terkini
    val currentOnAddGroup = rememberUpdatedState(onAddGroup)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = "Name: ${user.name}", style = MaterialTheme.typography.titleMedium)

        // Menampilkan Groups
        if (user.groups.isNotEmpty()) {
            Text(text = "Groups:", style = MaterialTheme.typography.bodyLarge)

            Column {
                user.groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "- ${group.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f) // Membuat teks mengisi ruang yang tersedia
                        )

                        Button(
                            onClick = { onDeleteGroup(group.id) }, // Mengirim group.id yang sesuai
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Delete Group")
                        }
                    }
                }
            }
        } else {
            Text(text = "No groups", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Group")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete User")
            }
        }

        // Menampilkan Snackbar jika ada pesan kesalahan
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    Button(onClick = { errorMessage = null }) {
                        Text("Tutup")
                    }
                }
            ) {
                Text(text = errorMessage ?: "")
            }
        }

        // Dialog untuk menambahkan group
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Group") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done // Menutup keyboard saat "Done" ditekan
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide() // Menutup keyboard saat "Done"
                            }
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        // Pastikan input nama tidak kosong
                        if (name.isNotEmpty() && !isLoading) {
                            isLoading = true // Mengatur status loading saat operasi dimulai
                            Log.d("AddGroup", "Group Name: $name")  // Menambahkan log untuk debugging

                            coroutineScope.launch {
                                try {
                                    user.id?.let { userId ->
                                        val groupRequest = GroupRequest(name)
                                        Log.d("AddGroup", "Sending request with: $groupRequest") // Log request body

                                        val response = RetrofitInstance.api.addGroupToUser(userId, groupRequest)
                                        if (response.isSuccessful) {
                                            currentOnAddGroup.value(name) // Gunakan nilai yang selalu terbaru
                                            showDialog = false  // Menutup dialog setelah berhasil
                                        } else {
                                            val errorBody = response.errorBody()?.string()
                                            Log.e("Retrofit Error", "Error: $errorBody")
                                        }
                                    }
                                } catch (e: HttpException) {
                                    val errorResponse = e.response()?.errorBody()?.string()
                                    Log.e("HttpException", "Error body: $errorResponse")
                                } catch (e: Exception) {
                                    Log.e("Exception", "Unexpected error: ${e.message}")
                                } finally {
                                    isLoading = false // Mengubah status loading menjadi false setelah operasi selesai
                                }
                            }
                            keyboardController?.hide() // 🔥 Tutup keyboard setelah submit
                        } else {
                            // Jika nama grup kosong
                            Log.d("AddGroup", "Group Name is empty!")  // Log jika name kosong
                            errorMessage = "Nama grup tidak boleh kosong"
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator() // Menampilkan indikator loading saat mengirim request
                        } else {
                            Text("Tambah Group")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
