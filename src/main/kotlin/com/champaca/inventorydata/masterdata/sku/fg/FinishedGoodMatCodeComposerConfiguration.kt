package com.champaca.inventorydata.masterdata.sku.fg

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FinishedGoodMatCodeComposerConfiguration{
    @Bean
    fun getFgMatCodeComposers(
        ceilingMatCodeComposer: CeilingMatCodeComposer,
        deckingMatCodeComposer: DeckingMatCodeComposer,
        doorMatCodeComposer: DoorMatCodeComposer,
        endingMatCodeComposer: EndingMatCodeComposer,
        flooringMatCodeComposer: FlooringMatCodeComposer,
        railMatCodeComposer: RailMatCodeComposer,
        skirtMatCodeComposer: SkirtMatCodeComposer,
        stairMatCodeComposer: StairMatCodeComposer,
        furnitureMatCodeComposer: FurnitureMatCodeComposer,
        furniturePartMatCodeComposer: FurniturePartMatCodeComposer
    ): Map<FinishedGoodType, FinishedGoodMatCodeComposer> {
        return mapOf(
            FinishedGoodType.CEILING to ceilingMatCodeComposer,
            FinishedGoodType.DECKING to deckingMatCodeComposer,
            FinishedGoodType.DOOR to doorMatCodeComposer,
            FinishedGoodType.ENDING to endingMatCodeComposer,
            FinishedGoodType.FRAME to doorMatCodeComposer,
            FinishedGoodType.FLOORING to flooringMatCodeComposer,
            FinishedGoodType.RAIL to railMatCodeComposer,
            FinishedGoodType.SKIRT to skirtMatCodeComposer,
            FinishedGoodType.STAIR to stairMatCodeComposer,
            FinishedGoodType.WALL to skirtMatCodeComposer,
            FinishedGoodType.FURNITURE to furnitureMatCodeComposer,
            FinishedGoodType.FURNITURE_PART to furniturePartMatCodeComposer
        )
    }
}