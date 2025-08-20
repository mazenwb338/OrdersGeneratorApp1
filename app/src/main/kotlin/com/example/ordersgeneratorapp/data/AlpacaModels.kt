import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    val symbol: String,
    val qty: String,
    val side: String,
    val type: String,
    @SerializedName("time_in_force") val timeInForce: String, // ✅ Use snake_case
    @SerializedName("limit_price") val limitPrice: String? = null, // ✅ Use snake_case
    @SerializedName("stop_price") val stopPrice: String? = null, // ✅ Use snake_case
    @SerializedName("client_order_id") val clientOrderId: String? = null // ✅ Use snake_case
)

data class AlpacaOrder(
    val id: String, // ← UNIQUE ALPACA SERVER ORDER ID
    val clientOrderId: String?,
    val createdAt: String,
    val updatedAt: String?,
    val submittedAt: String?,
    val filledAt: String?,
    val expiredAt: String?,
    val canceledAt: String?,
    val failedAt: String?,
    // ✅ ADD MISSING FIELDS:
    val replacedAt: String?,
    val replacedBy: String?,
    val replaces: String?,
    val assetId: String,
    val symbol: String,
    val assetClass: String,
    val notional: String?,
    val qty: String,
    val filledQty: String?,
    val filledAvgPrice: String?,
    val orderClass: String,
    val orderType: String,
    val type: String, // ✅ ADD THIS
    val side: String,
    val timeInForce: String,
    val limitPrice: String?,
    val stopPrice: String?,
    val status: String,
    val extendedHours: Boolean,
    val legs: List<String>,
    // ✅ ADD MISSING TRAIL FIELDS:
    val trailPercent: String?,
    val trailPrice: String?,
    val hwm: String?,
    val commission: String?,
    val accountName: String? = null // ← ADD ACCOUNT NAME FIELD
)
