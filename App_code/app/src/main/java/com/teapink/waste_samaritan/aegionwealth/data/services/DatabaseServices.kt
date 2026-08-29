package com.teapink.waste_samaritan.aegionwealth.data.services

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioMultiRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioRecord
import com.teapink.waste_samaritan.aegionwealth.utils.Constants
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DatabaseServices(
    firebase: FirebaseFirestore
) {

    val mUserCollection = firebase.collection(Constants.USER_COLLECTION)
    val mPortfolioCollection = firebase.collection(Constants.PORTFOLIO_COLLECTION)
    val mPortfolioMultiCollection = firebase.collection(Constants.MULTI_PORTFOLIO_COLLECTION)

    suspend fun saveUserRecord(user: UserProfile): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // .await() turns the Firebase Task into a Kotlin Coroutine
                mUserCollection.document(user.uid).set(user).await()

                Resource.Success(Unit)
            } catch (e: Exception) {
                // Catches Firebase network errors, permission denied, etc.
                Resource.Error(e.localizedMessage ?: "Failed to save user to cloud.")
            }
        }
    }

    suspend fun savePortfolioRecord(record: PortfolioRecord): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                val uid = mPortfolioCollection.document().id
                // .await() turns the Firebase Task into a Kotlin Coroutine
                record.documentId = uid
                Log.d("FirebaseLogged", "${record}")
                mPortfolioCollection.document(record.documentId).set(record).await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                // Catches Firebase network errors, permission denied, etc.
                Log.d("FirebaseLoggedError", "${e}")
                Resource.Error(e.localizedMessage ?: "Failed to save portfolio to cloud.")
            }
        }
    }

    suspend fun savePortfolioMultiRecord(record: PortfolioMultiRecord): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val uid = mPortfolioMultiCollection.document().id
                // .await() turns the Firebase Task into a Kotlin Coroutine
                record.documentId = uid
                Log.d("FirebaseLogged", "${record}")
                mPortfolioMultiCollection.document(record.documentId).set(record).await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                // Catches Firebase network errors, permission denied, etc.
                Log.d("FirebaseLoggedError", "${e}")
                Resource.Error(e.localizedMessage ?: "Failed to save portfolio to cloud.")
            }
        }
    }

    suspend fun deletePortfolioRecord(record: PortfolioRecord): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirebaseLogged", "Delete ${record}")
                mPortfolioCollection.document(record.documentId).delete().await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                // Catches Firebase network errors, permission denied, etc.
                Log.d("FirebaseLoggedError", "${e}")
                Resource.Error(e.localizedMessage ?: "Failed to save portfolio to cloud.")
            }
        }
    }

    suspend fun deletePortfolioMultiRecord(record: PortfolioMultiRecord): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FirebaseLogged", "delete ${record}")
                mPortfolioMultiCollection.document(record.documentId).delete().await()
                Resource.Success(Unit)
            } catch (e: Exception) {
                // Catches Firebase network errors, permission denied, etc.
                Log.d("FirebaseLoggedError", "${e}")
                Resource.Error(e.localizedMessage ?: "Failed to save portfolio to cloud.")
            }
        }
    }


    suspend fun getUserPortfolioHistory(userId: String): Result<List<PortfolioHistoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isBlank()) return@withContext Result.failure(Exception("User not logged in"))

                // 1. Fetch Equity Portfolios (Async)
                val equityTask = async {
                    mPortfolioCollection // Check your exact collection name
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                        .toObjects(PortfolioRecord::class.java)
                        .map { PortfolioHistoryItem.Equity(it) }
                }

                // 2. Fetch Multi-Asset Portfolios (Async)
                val multiAssetTask = async {
                    mPortfolioMultiCollection// Check your exact collection name
                        .whereEqualTo("userId", userId)
                        .orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                        .toObjects(PortfolioMultiRecord::class.java)
                        .map { PortfolioHistoryItem.MultiAsset(it) }
                }

                // 3. Wait for both to finish, merge, and sort by newest first
                val allHistory =
                    (equityTask.await() + multiAssetTask.await()).sortedByDescending { it.timestamp }

                Result.success(allHistory)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getEquityHistory(userId: String): Result<List<PortfolioHistoryItem.Equity>> {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isBlank()) return@withContext Result.failure(Exception("User ID is blank"))

                val records = mPortfolioCollection.whereEqualTo("userId", userId).get()
                    .await() // Fetch safely without orderBy
                    .toObjects(PortfolioRecord::class.java).map { PortfolioHistoryItem.Equity(it) }
                    .sortedByDescending { it.timestamp } // Sort locally to prevent crashes

                Result.success(records)
            } catch (e: Exception) {
                Log.e("FirebaseError", "Equity fetch failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getMultiAssetHistory(userId: String): Result<List<PortfolioHistoryItem.MultiAsset>> {
        return withContext(Dispatchers.IO) {
            try {
                if (userId.isBlank()) return@withContext Result.failure(Exception("User ID is blank"))

                val records = mPortfolioMultiCollection.whereEqualTo("userId", userId).get()
                    .await() // Fetch safely without orderBy
                    .toObjects(PortfolioMultiRecord::class.java)
                    .map { PortfolioHistoryItem.MultiAsset(it) }
                    .sortedByDescending { it.timestamp } // Sort locally to prevent crashes

                Log.d("FirebaseLogged", "Multi Data ${records}")

                Result.success(records)
            } catch (e: Exception) {
                Log.e("FirebaseError", "Multi-Asset fetch failed", e)
                Result.failure(e)
            }
        }
    }

    // Returns a live stream of Equity History
    fun getEquityHistoryFlow(userId: String): Flow<Result<List<PortfolioHistoryItem.Equity>>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(Result.failure(Exception("User ID is blank")))
                close()
                return@callbackFlow
            }

            val listener = mPortfolioCollection.whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseLive", "Equity listen failed.", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val records = snapshot.toObjects(PortfolioRecord::class.java)
                                .map { PortfolioHistoryItem.Equity(it) }
                            trySend(Result.success(records))
                        } catch (e: Exception) {
                            Log.e("FirebaseLive", "Equity parsing failed.", e)
                            trySend(Result.failure(e))
                        }
                    }
                }

            // Suspends until the flow is cancelled, then cleans up the listener
            awaitClose { listener.remove() }
        }

    // Returns a live stream of Multi-Asset History
    fun getMultiAssetHistoryFlow(userId: String): Flow<Result<List<PortfolioHistoryItem.MultiAsset>>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(Result.failure(Exception("User ID is blank")))
                close()
                return@callbackFlow
            }

            val listener = mPortfolioMultiCollection.whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseLive", "MultiAsset listen failed.", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val records = snapshot.toObjects(PortfolioMultiRecord::class.java)
                                .map { PortfolioHistoryItem.MultiAsset(it) }
                            trySend(Result.success(records))
                        } catch (e: Exception) {
                            Log.e("FirebaseLive", "MultiAsset parsing failed.", e)
                            trySend(Result.failure(e))
                        }
                    }
                }

            awaitClose { listener.remove() }
        }

}