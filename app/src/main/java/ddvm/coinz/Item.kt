package ddvm.coinz

import com.google.firebase.firestore.FirebaseFirestore

abstract class Item {
    abstract val itemName: String
    abstract val itemDescription: String
    open var addVisionRange: Int = 0
    open var addWalletCapacity: Int = 0
    open var coinRecognition: Boolean = false

    fun equip(firestore: FirebaseFirestore?) {
        User.equipItem(firestore, this)
    }
}

class Binoculars: Item() {
    override val itemName = "Binoculars"
    override val itemDescription = "Increases vision range by 50m"
    override var addVisionRange = 50
}