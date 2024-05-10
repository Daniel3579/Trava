package land.trava

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.collections.List

class Firestore(private val plantPath: String) {
    /**
     * Class variables
     */
    private val db = FirebaseFirestore.getInstance()
    private var lastVisible: DocumentSnapshot? = null
    val c = 12
    val databaseRecordAmount = 1526

    /**
     * Start function that loads the first 12 items
     */
    fun start(callback: getPlantsCallback) {
        // Load first for define lastVisible
        db.collection(plantPath).limit(1).get().addOnSuccessListener { documentSnapshot ->
            // Get first database item
            if (documentSnapshot.documents.isNotEmpty()) {
                lastVisible = documentSnapshot.documents[0]
            }

            // Create empty plants array
            val plantsArray = ArrayList<PlantDataStruct>()

            // Load first 12 items
            if (lastVisible != null) {
                // Default operations
                db.collection(plantPath).startAt(lastVisible!!).limit(c.toLong()).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Receive all "docs" by plantPath
                        for (document in task.result!!) {
                            try {
                                // Convert from Snapshot to PlantDataStruct
                                val plantMap = document.data
                                val plantData = PlantDataStruct(plantMap)
                                plantsArray.add(plantData)
                            } catch (e: IOException) {
                                throw RuntimeException(e)
                            }
                        }

                        // Find last item in current collection
                        val snapshotList: List<DocumentSnapshot> = task.result!!.documents
                        lastVisible = snapshotList[snapshotList.size - 1]

                        // Callback call
                        callback.onComplete(plantsArray)
                    } else {
                        Log.w(TAG, "Error getting documents.", task.exception)
                    }
                }
            }
        }
    }

    /**
     * Get plant data from Firestore 🔥
     */
    fun getPlants(callback: getPlantsCallback) {
        // Create empty plants array
        val plantsArray = ArrayList<PlantDataStruct>()

        // Get plants from database
        if (lastVisible != null) {
            // Default operations
            db.collection(plantPath).startAfter(lastVisible!!).limit(c.toLong()).get()
                .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Receive all "docs" by plantPath
                    for (document in task.result!!) {
                        try {
                            // Convert from Snapshot to PlantDataStruct
                            val plantMap = document.data
                            val plantData = PlantDataStruct(plantMap)
                            plantsArray.add(plantData)
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                    }

                    // Find last item in current collection
                    val snapshotList: List<DocumentSnapshot> = task.result!!.documents
                    if (snapshotList.isNotEmpty()) {
                        lastVisible = snapshotList[snapshotList.size - 1]
                    }

                    // Callback call
                    callback.onComplete(plantsArray)
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
        }
    }

    /**
     * Method for callback
     */
    interface getPlantsCallback {
        fun onComplete(plantsArray: ArrayList<PlantDataStruct>)
    }
}