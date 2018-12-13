package ddvm.coinz

import com.google.firebase.firestore.FirebaseFirestore

//items modifying user's statistics
abstract class Item {
    abstract val itemName: String
    abstract val itemDescription: String                    //description what the item does
    abstract val price: Double                              //gold amount needed to buy an item
    abstract val iconResource: Int                          //R.drawable resource of the item's icon
    open var additionalVisionRange: Int = 0                 //how much vision range is increased by item, 0 by default
    open var additionalWalletCapacity: Int = 0              //additional wallet capacity, default 0

    open fun buy(firestore: FirebaseFirestore?) {
        User.decreaseGold(firestore, this.price)            //reduce amount of user's gold
        User.setStatsChanged(true)                          //set this to notify the MainActivity of the change
    }
}

//item increases user's vision range
object Binoculars: Item() {
    override val itemName = "Binoculars"
    override val itemDescription = "Increases vision range by 50m"
    override val price = 100000.0
    override val iconResource = R.drawable.ic_binoculars
    override var additionalVisionRange = 50
    override fun buy(firestore: FirebaseFirestore?) {
        super.buy(firestore)
        User.setBinoculars(firestore, true)         //set to note the user bought binoculars
    }
}

//item increases user's wallet capacity
object Bag: Item() {
    override val itemName = "Bag"
    override val itemDescription = "Increases wallet capacity by 15 coins"
    override val price = 25000.0
    override val iconResource = R.drawable.ic_bag
    override var additionalWalletCapacity = 15
    override fun buy(firestore: FirebaseFirestore?) {
        super.buy(firestore)
        User.setBag(firestore, true)                //note the user bought bag
    }
}

//item enables the user to see coin values on the map
object Glasses: Item() {
    override val itemName = "Glasses"
    override val itemDescription = "Shows value of coins on the map"
    override val price = 10000.0
    override val iconResource = R.drawable.ic_glasses
    override fun buy(firestore: FirebaseFirestore?) {
        super.buy(firestore)
        User.setGlasses(firestore, true)            //note the user bought glasses
    }

}