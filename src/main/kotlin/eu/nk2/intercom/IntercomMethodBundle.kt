package eu.nk2.intercom

import java.io.Serializable

data class IntercomMethodBundle(
    val publisherId: Int,
    val methodId: Int,
    val parameters: Array<Any>
): Serializable {
    companion object {
        private const val serialVersionUID = 20200720174649L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntercomMethodBundle

        if (publisherId != other.publisherId) return false
        if (methodId != other.methodId) return false
        if (!parameters.contentEquals(other.parameters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publisherId
        result = 31 * result + methodId
        result = 31 * result + parameters.contentHashCode()
        return result
    }
}
