package com.champaca.inventorydata.masterdata.sku.fg

enum class FinishedGoodType(val odooProductName: String) {
    CEILING("FG-Ceiling"),
    DECKING("FG-Decking"),
    DOOR("FG-Door"),
    ENDING("FG-Ending"),
    FRAME("FG-Frame"),
    FLOORING("FG-Flooring"),
    RAIL("FG-Rail"),
    SKIRT("FG-Skirt"),
    STAIR("FG-Stair"),
    WALL("FG-Wall"),
    FURNITURE("FG-Furniture"),
    FURNITURE_PART("FG-Furniture Part");

    companion object {
        fun fromOdooName(name: String): FinishedGoodType {
            return values().first { it.odooProductName == name }
        }
    }
}