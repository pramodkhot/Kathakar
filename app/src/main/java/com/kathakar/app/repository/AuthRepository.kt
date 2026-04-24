package com.kathakar.app.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currentUserFlow: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            val uid = fbAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                db.collection(FirestoreCollections.USERS).document(uid)
                    .addSnapshotListener { snap, _ ->
                        trySend(snap?.toObject(User::class.java))
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Resource<User> = try {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: return Resource.Error("No UID")
        if (result.additionalUserInfo?.isNewUser == true) {
            createUserWithCoins(uid, result.user?.displayName ?: "Reader",
                result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "")
        }
        Resource.Success(fetchUser(uid))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Google sign-in failed")
    }

    suspend fun signInWithEmail(email: String, password: String): Resource<User> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Resource.Success(fetchUser(result.user?.uid ?: return Resource.Error("No UID")))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Sign-in failed")
    }

    suspend fun register(name: String, email: String, password: String): Resource<User> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return Resource.Error("No UID")
        createUserWithCoins(uid, name, email, "")
        Resource.Success(fetchUser(uid))
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Registration failed")
    }

    fun signOut() = auth.signOut()

    private suspend fun createUserWithCoins(uid: String, name: String, email: String, photoUrl: String) {
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
        val txnRef  = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        db.batch().apply {
            set(userRef, User(
                userId = uid, name = name, email = email, photoUrl = photoUrl,
                role = UserRole.READER, coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP,
                createdAt = Timestamp.now()
            ))
            set(txnRef, CoinTransaction(
                txnId = txnRef.id, userId = uid, type = CoinTxnType.SIGNUP_BONUS,
                coinsAmount = MvpConfig.FREE_COINS_ON_SIGNUP,
                note = "Welcome! ${MvpConfig.FREE_COINS_ON_SIGNUP} free coins",
                createdAt = Timestamp.now()
            ))
        }.commit().await()
    }

    private suspend fun fetchUser(uid: String): User =
        db.collection(FirestoreCollections.USERS).document(uid)
            .get().await().toObject(User::class.java) ?: User(userId = uid)
}
