package nl.giejay.mediaslider

import android.os.Parcel
import android.os.Parcelable
import com.zeuskartik.mediaslider.DisplayOptions
import com.zeuskartik.mediaslider.SliderItemViewHolder
import nl.giejay.mediaslider.transformations.GlideTransformations
import java.util.EnumSet

class MediaSliderConfiguration : Parcelable {
    private val displayOptions: EnumSet<DisplayOptions>?
    val startPosition: Int
    val interval: Int
    val isOnlyUseThumbnails: Boolean
    val isVideoSoundEnable: Boolean
    val animationSpeedMillis: Int
    val maxCutOffHeight: Int
    val maxCutOffWidth: Int
    val glideTransformation: GlideTransformations

    constructor(displayOptions: EnumSet<DisplayOptions>?,
                startPosition: Int,
                interval: Int,
                onlyUseThumbnails: Boolean,
                isVideoSoundEnable: Boolean,
                assets: List<SliderItemViewHolder>,
                loadMore: LoadMore?,
                onAssetSelected: (SliderItemViewHolder) -> Unit = {},
                animationSpeedMillis: Int,
                maxCutOffHeight: Int,
                maxCutOffWidth: Int,
                transformation: GlideTransformations) {
        this.displayOptions = displayOptions
        this.startPosition = startPosition
        this.interval = interval
        this.isOnlyUseThumbnails = onlyUseThumbnails
        this.isVideoSoundEnable = isVideoSoundEnable
        Companion.loadMore = loadMore
        Companion.assets = assets
        Companion.onAssetSelected = onAssetSelected
        this.animationSpeedMillis = animationSpeedMillis
        this.maxCutOffHeight = maxCutOffHeight
        this.maxCutOffWidth = maxCutOffWidth
        this.glideTransformation = transformation
    }

    private constructor(`in`: Parcel) {
        displayOptions = `in`.readSerializable() as EnumSet<DisplayOptions>?
        startPosition = `in`.readInt()
        interval = `in`.readInt()
        isOnlyUseThumbnails = `in`.readByte().toInt() != 0
        isVideoSoundEnable = `in`.readByte().toInt() != 0
        this.animationSpeedMillis = `in`.readInt()
        this.maxCutOffHeight =  `in`.readInt()
        this.maxCutOffWidth =  `in`.readInt()
        this.glideTransformation = GlideTransformations.valueOfSafe(`in`.readString()!!, GlideTransformations.CENTER_INSIDE)
    }

    val isClockVisible: Boolean
        get() = displayOptions!!.contains(DisplayOptions.CLOCK)

    val isTitleVisible: Boolean
        get() = displayOptions!!.contains(DisplayOptions.TITLE)

    val isSubtitleVisible: Boolean
        get() = displayOptions!!.contains(DisplayOptions.SUBTITLE)

    val isDateVisible: Boolean
        get() = displayOptions!!.contains(DisplayOptions.DATE)

    val isMediaCountVisible: Boolean
        get() = displayOptions!!.contains(DisplayOptions.MEDIA_COUNT)

    val isGradiantOverlayVisible: Boolean
        get() = (isMediaCountVisible || isDateVisible || isClockVisible || isTitleVisible || isSubtitleVisible) && displayOptions!!.contains(
            DisplayOptions.GRADIENT_OVERLAY)

    val isNavigationVisible: Boolean
        get() = displayOptions!!.contains(DisplayOptions.NAVIGATION)

    var items: List<SliderItemViewHolder>
        get() = assets
        set(value) {
            assets = value
        }

    val loadMore: LoadMore?
        get() = Companion.loadMore

    val onAssetSelected: (SliderItemViewHolder) -> Unit
        get() = Companion.onAssetSelected

    fun enableSlideAnimation(): Boolean {
        return displayOptions!!.contains(DisplayOptions.ANIMATE_ASST_SLIDE)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(displayOptions)
        dest.writeInt(startPosition)
        dest.writeInt(interval)
        dest.writeByte((if (isOnlyUseThumbnails) 1 else 0).toByte())
        dest.writeByte((if (isVideoSoundEnable) 1 else 0).toByte())
        dest.writeInt(animationSpeedMillis)
        dest.writeInt(maxCutOffHeight)
        dest.writeInt(maxCutOffWidth)
        dest.writeString(glideTransformation.toString())
    }

    companion object {
        // Cant be serializable so this "workaround" for these two fields
        var assets: List<SliderItemViewHolder> = emptyList()
        var loadMore: LoadMore? = null
        var onAssetSelected: (SliderItemViewHolder) -> Unit = { _ -> }

        @JvmField
        val CREATOR: Parcelable.Creator<MediaSliderConfiguration> =
            object : Parcelable.Creator<MediaSliderConfiguration> {
                override fun createFromParcel(`in`: Parcel): MediaSliderConfiguration {
                    return MediaSliderConfiguration(`in`)
                }

                override fun newArray(size: Int): Array<MediaSliderConfiguration?> {
                    return arrayOfNulls(size)
                }
            }
    }
}