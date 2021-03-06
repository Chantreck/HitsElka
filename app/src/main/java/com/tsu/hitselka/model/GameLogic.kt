package com.tsu.hitselka.model

import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.tsu.hitselka.R
import java.util.*

object GameLogic {
    private val db =
        Firebase.database("https://hitselka-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val uid = SharedPrefs.getUID()
    private val database: DatabaseReference = db.reference
    private val buildings = database.child("buildings")
    private val player = database.child("players").child(uid)
    private val playerBuildings = player.child("firstYearStats")
    private val playerResources = player.child("resources")
    private val playerStats = player.child("stats")
    private val playerGifts = player.child("gifts")

    private val objects = listOf("university", "forest", "hedgehog", "maiden", "father_frost")
    private const val OBJECTS_COUNT = 35

    fun init() {
        GameData.init()
    }

    fun firstRun(uid: String): PlayerInfo {
        val playerInfo = PlayerInfo()
        val currentLocale = Locale.getDefault().language
        if (currentLocale != "ru") {
            playerInfo.settings.lang = false
        }
        database.child("players").child(uid).setValue(playerInfo)
        return playerInfo
    }

    fun getBuildings(_buildings: MutableLiveData<List<Object>>) {
        val uid = SharedPrefs.getUID()
        val buildings = mutableListOf<Object>()

        for (building in objects) {
            val image = when (building) {
                "university" -> R.drawable.university
                "forest" -> R.drawable.forest
                "hedgehog" -> R.drawable.hedgehog
                "maiden" -> R.drawable.winter_maiden
                "father_frost" -> R.drawable.father_frost
                else -> throw Exception()
            }

            val buildingInfo = database.child("buildings").child(building)
            val buildingPlayerInfo =
                database.child("players").child(uid).child("firstYearStats").child(building)

            buildingInfo.child("maxStage").get().addOnSuccessListener { maxStageSnapshot ->
                val maxStage = maxStageSnapshot.value as Long

                buildingPlayerInfo.get().addOnSuccessListener {
                    val level = (it.value as HashMap<*, *>)["level"] as Long
                    val wands = (it.value as HashMap<*, *>)["wands"] as Long
                    val wandsNeeded = (it.value as HashMap<*, *>)["wandsNeeded"] as Long

                    for (i in 1 until maxStage) {
                        val newBuilding = Object(
                            building,
                            getName(building),
                            image,
                            i + 1L,
                            maxStage,
                            wands,
                            wandsNeeded,
                            (i < level),
                            (i > level)
                        )
                        buildings.add(newBuilding)
                    }

                    if (building == "father_frost") {
                        _buildings.value = buildings.sortedBy { it.level }
                    }
                }
            }
        }
    }

    private fun getName(type: String): Int = when (type) {
        "university" -> R.string.university
        "forest" -> R.string.forest
        "hedgehog" -> R.string.hedgehog
        "maiden" -> R.string.maiden
        "father_frost" -> R.string.fatherFrost
        else -> throw Exception()
    }

    fun getYearProgress(_progress: MutableLiveData<Int>) {
        val uid = SharedPrefs.getUID()

        database.child("players").child(uid).child("stats").child("objectsBuilt")
            .get().addOnSuccessListener {
                val objectsBuilt = (it.value as Long).toInt()
                _progress.value = objectsBuilt * 100 / OBJECTS_COUNT
            }
    }

    fun sendWands(item: Object, wands: Long, wandsForLevel: Long) {
        val stats = GameData.getStats() ?: return
        val resources = GameData.getResources() ?: return

        if (item.wandsSpent + wands != item.wandsNeeded) {
            playerBuildings.child(item.type).child("wands").setValue(item.wandsSpent + wands)
        }

        playerResources.child("wands").setValue(resources.wands - wands)
        playerStats.child("wandsUsed").setValue(stats.wandsUsed + wands)
        playerStats.child("currentLevelWandsUsed")
            .setValue(stats.currentLevelWandsUsed + wandsForLevel)
    }

    fun newLevel() {
        val oldStats = GameData.getStats() ?: return
        val gifts = GameData.getGifts() ?: return

        playerStats.setValue(
            Stats(
                oldStats.currentLevel + 1,
                0,
                oldStats.objectsBuilt,
                oldStats.wandsUsed,
            )
        )

        playerGifts.child("bright").child("gifts").setValue(gifts.bright.gifts + 1)
        if (oldStats.currentLevel % 10 == 9L) {
            playerGifts.child("special").child("gifts").setValue(gifts.special.gifts + 1)
        }
        if (oldStats.currentLevel % 100 == 99L) {
            playerGifts.child("fairytale").child("gifts").setValue(gifts.fairytale.gifts + 1)
        }
    }

    fun upgrade(item: Object) {
        val stats = GameData.getStats() ?: return
        val gifts = GameData.getGifts() ?: return

        playerStats.child("objectsBuilt").setValue(stats.objectsBuilt + 1)
        playerGifts.child("special").child("gifts").setValue(gifts.special.gifts + 2)

        val stage = "stage" + item.level.toString()
        buildings.child(item.type).child(stage).get().addOnSuccessListener {
            val newStats = BuildingStats(item.level, 0L, it.value as Long)
            playerBuildings.child(item.type).setValue(newStats)
        }
    }

    fun giftOpened(gift: Gift, count: Int, reward: List<Inventory>) {
        val resources = GameData.getResources() ?: return

        playerResources.child("wands").setValue(resources.wands + reward[0].count)
        playerResources.child("moneys").setValue(resources.moneys + reward[1].count)
        if (gift.type != "bright") {
            playerResources.child("rubies").setValue(resources.rubies + reward[2].count)
        }

        playerGifts.child(gift.type).child("gifts").setValue(gift.gifts - count)

        if ((gift.giftsOpened + count == 100 && gift.type == "bright") ||
            (gift.giftsOpened + count == 10 && gift.type == "special") ||
            (gift.giftsOpened + count == 2 && gift.type == "fairytale")
        ) {
            playerGifts.child(gift.type).child("giftsOpened").setValue(0)
            playerGifts.child(gift.type).child("level").setValue(gift.level + 1)
        } else {
            playerGifts.child(gift.type).child("giftsOpened").setValue(gift.giftsOpened + count)
        }
    }

    fun chestOpened(reward: List<Inventory>) {
        val resources = GameData.getResources() ?: return

        playerResources.child("wands").setValue(resources.wands + reward[0].count)
        playerResources.child("moneys").setValue(resources.moneys + reward[1].count)
        playerResources.child("rubies").setValue(resources.rubies + reward[2].count)
        playerStats.child("chestLastOpened").setValue(System.currentTimeMillis())
    }
}