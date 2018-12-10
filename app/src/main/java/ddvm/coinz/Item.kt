package ddvm.coinz

import com.google.firebase.firestore.FirebaseFirestore

abstract class Item {
    abstract val itemName: String
    abstract val itemDescription: String
    abstract val price: Double
    open var additionalVisionRange: Int = 0
    open var additionalWalletCapacity: Int = 0
    open var coinRecognition: Boolean = false

    fun buy(firestore: FirebaseFirestore?) {
        User.buyItem(firestore, this)
        User.decreaseGold(firestore, this.price)
    }
}

class Binoculars: Item() {
    override val itemName = "Binoculars"
    override val itemDescription = "Increases vision range by 50m"
    override val price = 100000.0
    override var additionalVisionRange = 50
}