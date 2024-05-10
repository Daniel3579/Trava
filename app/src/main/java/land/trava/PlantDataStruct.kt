package land.trava

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.util.ObjectsCompat.requireNonNull
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.Map

class PlantDataStruct {
    /**
     * Necessary variables
     */
    var name: String? = null
    var category: ArrayList<Byte>? = null
    var climate: ArrayList<Byte>? = null
    var luminosity: ArrayList<Byte>? = null
    var bloom: ArrayList<Byte>? = null
    var imageLink: String? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    /**
     * Empty constructor
     */
    constructor(context: Context) {
        PlantDataStruct.context = context
    }

    /**
     * Convert constructor
     */
    @Throws(IOException::class)
    constructor(plantData: Map<String, Any>) {
        this.name = getLocalText("plant_name_" + requireNonNull(plantData["name"]))
        this.category = objectToByteArrayList(plantData["category"])
        this.luminosity = objectToByteArrayList(plantData["luminosity"])
        this.bloom = objectToByteArrayList(plantData["bloom"])
        this.climate = objectToByteArrayList(plantData["climate"])
        this.imageLink = requireNonNull(plantData["imageLink"]).toString()
    }

    /**
     * Overridden method toString() for returning plant name
     */
    override fun toString(): String {
        return name!!
    }

    /**
     * Transforms plant climate data to human readable format
     */
    fun readClimate(): String {
        val strBuilder = StringBuilder()

        for (element in climate!!) {
            strBuilder.append(element).append("–")
        }

        var str = strBuilder.toString()
        str = str.substring(0, str.length - 1)

        return str
    }

    /**
     * Transforms plant luminosity data to human readable format
     */
    fun readLuminosity(): String {
        val strBuilder = StringBuilder()

        for (element in luminosity!!) {
            when (element.toInt()) {
                1 -> strBuilder.append("🌕 ")
                2 -> strBuilder.append("🌗 ")
                3 -> strBuilder.append("🌑 ")
                else -> Log.e("Firestore", "$name has bad luminosity data: $luminosity")
            }
        }

        var str = strBuilder.toString()
        str = str.substring(0, str.length - 1)

        return str
    }

    /**
     * Transforms plant bloom data to human readable format
     */
    fun readBloom(): String {
        val strBuilder = StringBuilder()

        for (element in bloom!!) {
            strBuilder.append(getLocalText("month_$element")).append("–")
            //For exception
//            Log.e("Firestore", "$name has bad bloom data: $bloom")
        }

        var str = strBuilder.toString()
        str = str.substring(0, str.length - 1)

        return str
    }

    /**
     * Transforms plant category data to human readable format
     */
    fun readCategory(): String {
        val strBuilder = StringBuilder()

        for (element in category!!) {
            strBuilder.append(getLocalText("category_$element")).append(",\n\n")
            //For exception
//            Log.e("Firestore", "$name has bad category data: $category")
        }

        var str = strBuilder.toString()
        str = str.substring(0, str.length - 3)

        return str
    }

    /**
     * Auxiliary method for convert constructor
     */
    private fun objectToByteArrayList(`object`: Any?): ArrayList<Byte> {
        val list = ArrayList<Byte>()
        val objectList = `object` as ArrayList<Long>

        for (i in objectList) {
            list.add(i.toByte())
        }

        return list
    }

    @SuppressLint("DiscouragedApi")
    private fun getLocalText(strID: String): String {
        return context.getString(context.resources.getIdentifier(strID, "string", context.packageName))
    }
}