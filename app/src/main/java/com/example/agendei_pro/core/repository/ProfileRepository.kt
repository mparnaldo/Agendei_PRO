package com.example.agendei_pro.core.repository

import com.example.agendei_pro.core.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            val snapshot = firestore.collection("profiles").document(user.uid).get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile != null) {
                    val googlePhotoUrl = user.photoUrl?.toString() ?: ""
                    if (profile.photoUrl != googlePhotoUrl && googlePhotoUrl.isNotEmpty()) {
                        val updated = profile.copy(photoUrl = googlePhotoUrl)
                        saveProfile(updated)
                        updated
                    } else {
                        profile
                    }
                } else null
            } else {
                // Se não existe, cria um novo com os dados do Google
                val newProfile = UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "Usuário",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                saveProfile(newProfile)
                newProfile
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        return try {
            firestore.collection("profiles").document(profile.uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateFcmToken(token: String) {
        val user = auth.currentUser ?: return
        firestore.collection("profiles").document(user.uid).update("fcmToken", token)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Usuário não logado"))
        return try {
            // 1. Deleta os dados do perfil no Firestore
            firestore.collection("profiles").document(user.uid).delete().await()
            
            // 2. Se for dono de salão, teria que deletar o salão e serviços (lógica futura)
            // Por enquanto focamos no perfil
            
            // 3. Deleta a conta de autenticação do Firebase
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
