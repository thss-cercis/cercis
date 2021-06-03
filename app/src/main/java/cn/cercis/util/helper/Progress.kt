package cn.cercis.util.helper

sealed class Progress<ProgressType>(open val progress: ProgressType) {
    data class Running<ProgressType>(override val progress: ProgressType) :
        Progress<ProgressType>(progress)

    data class Failed<ProgressType>(
        override val progress: ProgressType,
        val causeId: Int,
        val description: String,
    ) : Progress<ProgressType>(progress)

    data class Finished<ProgressType>(override val progress: ProgressType) :
        Progress<ProgressType>(progress)
}
