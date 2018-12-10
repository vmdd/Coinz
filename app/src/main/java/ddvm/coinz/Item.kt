package ddvm.coinz

import com.google.firebase.firestore.FirebaseFirestore

abstract class Item {
    abstract val itemName: String
    abstract val itemDescription: String
    abstract val price: Double
    abstract val iconResource: Int
    open var additionalVisionRange: Int = 0
    open var additionalWalletCapacity: Int = 0
    open var coinRecognition: Boolean = false

    open fun buy(firestore: FirebaseFirestore?) {
        User.decreaseGold(firestore, this.price)
    }
}

class Binoculars: Item() {
    override val itemName = "Binoculars"
    override val itemDescription = "Increases vision range by 50m"
    override val price = 100000.0
    override val iconResource = R.drawable.ic_binoculars
    override var additionalVisionRange = 50
    override fun buy(firestore: FirebaseFirestore?) {
        super.buy(firestore)
        User.setBinoculars(firestore, true)
        User.increaseVisionRange(additionalVisionRange)
    }
}

class Bag: Item() {
    override val itemName = "Bag"
    override val itemDescription = "Increases wallet capacity by 15 coins"
    override val price = 25000.0
    override val iconResource = R.drawable.ic_bag
    override var additionalWalletCapacity = 15
    override fun buy(firestore: FirebaseFirestore?) {
        super.buy(firestore)
        User.setBag(firestore, true)
        User.increaseWalletCapacity(15)
    }

}