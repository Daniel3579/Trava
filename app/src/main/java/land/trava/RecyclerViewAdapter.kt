package land.trava

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(dataArrayList: ArrayList<PlantDataStruct>, currentLayout: View) :
    RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {

    private var dataArrayList: ArrayList<PlantDataStruct> = ArrayList(dataArrayList)
    private var plantView: View = currentLayout.findViewById(R.id.plant_view)
    private var searchView: View = currentLayout.findViewById(R.id.search_view)
    private var recyclerView: View = currentLayout.findViewById(R.id.recycler_view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val plant = dataArrayList[position]
        holder.textView.text = plant.name
        //Set Image
        plant.imageLink?.let { Storage.setPlantImage(holder.imageView, it) }

        holder.cardView.setOnClickListener {
            val plantImage = plantView.findViewById<ImageView>(R.id.plant_image)
            val plantName = plantView.findViewById<TextView>(R.id.plant_name)
            val category = plantView.findViewById<TextView>(R.id.text_category)
            val climate = plantView.findViewById<TextView>(R.id.text_climate)
            val luminosity = plantView.findViewById<TextView>(R.id.text_luminosity)
            val bloom = plantView.findViewById<TextView>(R.id.text_bloom)

            //Set Image
            plant.imageLink?.let { it1 -> Storage.setPlantImage(plantImage, it1) }
            plantName.text = plant.name
            category.text = plant.readCategory()
            climate.text = plant.readClimate()
            luminosity.text = plant.readLuminosity()
            bloom.text = plant.readBloom()

            searchView.visibility = View.GONE
            recyclerView.visibility = View.GONE
            plantView.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return dataArrayList.size
    }

    fun increaseDataList(newDataChunk: ArrayList<PlantDataStruct>) {
        dataArrayList.addAll(newDataChunk)

        if (newDataChunk.isNotEmpty()) {
            notifyItemRangeChanged(dataArrayList.size - newDataChunk.size, dataArrayList.size)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFilteredList(filteredList: ArrayList<PlantDataStruct>) {
        dataArrayList.clear()
        dataArrayList.addAll(filteredList)
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.plant_card)
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val textView: TextView = itemView.findViewById(R.id.text_view)
    }
}