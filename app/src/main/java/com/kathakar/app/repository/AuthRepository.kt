package com.kathakar.app.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kathakar.app.domain.model.CoinTransaction
import com.kathakar.app.domain.model.CoinTxnType
import com.kathakar.app.domain.model.MvpConfig
import com.kathakar.app.domain.model.User
import com.kathakar.app.domain.model.UserRole
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
                    .addSnapshotListener { snap, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        val user = snap?.toObject(User::class.java)
                        if (user != null) {
                            trySend(user)
                        } else {
                            // User doc doesn't exist yet, create it
                            val firebaseUser = fbAuth.currentUser
                            if (firebaseUser != null) {
                                val newUser = User(
                                    userId = firebaseUser.uid,
                                    name = firebaseUser.displayName ?: "User",
                                    email = firebaseUser.email ?: "",
                                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                                    role = UserRole.READER,
                                    coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP
                                )
                                trySend(newUser)
                            }
                        }
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Resource<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign in failed - no user ID")
            val isNew = result.additionalUserInfo?.isNewUser == true
            if (isNew) {
                createUserWithCoins(
                    uid = uid,
                    name = result.user?.displayName ?: "User",
                    email = result.user?.email ?: "",
                    photoUrl = result.user?.photoUrl?.toString() ?: ""
                )
            } else {
                ensureUserDocExists(uid, result.user?.displayName ?: "User",
                    result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "")
            }
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Google sign-in failed: ${e.localizedMessage}")
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign in failed - no user ID")
            ensureUserDocExists(uid, result.user?.email?.substringBefore("@") ?: "User",
                result.user?.email ?: "", "")
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Sign-in failed: ${e.localizedMessage}")
        }
    }

    suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Registration failed - no user ID")
            createUserWithCoins(uid, name, email, "")
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Registration failed: ${e.localizedMessage}")
        }
    }

    fun signOut() = auth.signOut()

    private suspend fun createUserWithCoins(
        uid: String, name: String, email: String, photoUrl: String
    ) {
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
        val txnRef = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()

        db.batch().apply {
            set(userRef, User(
                userId = uid,
                name = name,
                email = email,
                photoUrl = photoUrl,
                role = UserRole.READER,
                coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP,
                createdAt = Timestamp.now()
            ))
            set(txnRef, CoinTransaction(
                txnId = txnRef.id,
                userId = uid,
                type = CoinTxnType.SIGNUP_BONUS,
                coinsAmount = MvpConfig.FREE_COINS_ON_SIGNUP,
                note = "Welcome! ${MvpConfig.FREE_COINS_ON_SIGNUP} free coins",
                createdAt = Timestamp.now()
            ))
        }.commit().await()
    }

    // Ensures user doc exists for existing Firebase Auth users
    // who may not have a Firestore document yet
    private suspend fun ensureUserDocExists(
        uid: String, name: String, email: String, photoUrl: String
    ) {
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
        val snap = userRef.get().await()
        if (!snap.exists()) {
            createUserWithCoins(uid, name, email, photoUrl)
        }
    }

    private suspend fun fetchUser(uid: String): User {
        return try {
            val doc = db.collection(FirestoreCollections.USERS)
                .document(uid).get().await()
            doc.toObject(User::class.java) ?: User(
                userId = uid,
                name = auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: "",
                coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP
            )
        } catch (e: Exception) {
            User(
                userId = uid,
                name = auth.currentUser?.displayName ?: "User",
                email = auth.currentUser?.email ?: "",
                coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP
            )
        }
    }
}
