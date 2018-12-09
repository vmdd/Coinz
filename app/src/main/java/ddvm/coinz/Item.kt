package ddvm.coinz

import com.google.firebase.firestore.FirebaseFirestore

abstract class Item {
    abstract val itemName: String
    abstract val itemDescription: String
    open var additionalVisionRange: Int = 0
    open var additionalWalletCapacity: Int = 0
    open var coinRecognition: Boolean = false

    fun equip(firestore: FirebaseFirestore?) {
        User.equipItem(firestore, this)
    }
}

class Binoculars: Item() {
    override val itemName = "Binoculars"
    override val itemDescription = "Increases vision range by 50m"
    override var additionalVisionRange = 50
}