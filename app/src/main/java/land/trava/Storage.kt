package land.trava

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide

@SuppressLint("StaticFieldLeak")
object Storage {
    private lateinit var context: Context

    /**
     * Set context
     */
    fun setContext(context: Context) {
        this.context = context
    }

    /**
     * Set plant image to imageView
     */
    fun setPlantImage(imageView: ImageView, path: String) {
        Glide.with(context).load(path).error(R.drawable.p1).into(imageView)
    }
}