package com.champaca.inventorydata.fg.flooring

import com.champaca.inventorydata.fg.flooring.model.MatCodeAttributeChangeType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProcessOutputChangeConfiguration {

    companion object {
        // RM department
        const val SIDE_RIP = 20
        const val SLICING = 21
        const val RM_SANDING = 22

        // Flooring department
        const val PRESS = 10
        const val FIXING = 13
        const val SANDING = 11
        const val FRENCH_POLISHING = 14
        const val BRUSHING = 15
        const val SIDE_FORMING = 16
        const val HEADER_FOOTER_FORMING = 17
        const val ALL_SIDES_FORMING = 18
        const val FINISHING = 12
    }

    @Bean
    fun getProcessOutputChanges(): Map<Int, List<MatCodeAttributeChangeType>> {
        return mapOf(
            SIDE_RIP to listOf(MatCodeAttributeChangeType.NEW_WIDTH),
            SLICING to listOf(MatCodeAttributeChangeType.NEW_THICKNESS, MatCodeAttributeChangeType.NEW_SKU_GROUP),
            RM_SANDING to emptyList(),

            FIXING to emptyList(),
            SANDING to listOf(MatCodeAttributeChangeType.NEW_THICKNESS),
            FRENCH_POLISHING to emptyList(),
            BRUSHING to emptyList(),
            SIDE_FORMING to listOf(MatCodeAttributeChangeType.NEW_WIDTH, MatCodeAttributeChangeType.INCREASE_MAIN_SKU_GROUP_FROM_3_TO_4),
            HEADER_FOOTER_FORMING to listOf(MatCodeAttributeChangeType.NEW_LENGTH, MatCodeAttributeChangeType.INCREASE_MAIN_SKU_GROUP_FROM_3_TO_4),
            ALL_SIDES_FORMING to listOf(MatCodeAttributeChangeType.NEW_WIDTH, MatCodeAttributeChangeType.NEW_LENGTH, MatCodeAttributeChangeType.INCREASE_MAIN_SKU_GROUP_FROM_3_TO_4)
        )
    }

    @Bean
    fun getProcessOutputLocation(): Map<Int, String> {
        return mapOf(
            SIDE_RIP to "BPRMZ9999",
            SLICING to "BPRMZ9999",
            RM_SANDING to "BPRMZ9999",

            PRESS to "BPFL1Z999",
            FIXING to "BPFL2Z999",
            SANDING to "BPFL3Z999",
            FRENCH_POLISHING to "BPFL4Z999",
            BRUSHING to "BPFL5Z999",
            SIDE_FORMING to "BPFL6Z999",
            HEADER_FOOTER_FORMING to "BPFL7Z999",
            ALL_SIDES_FORMING to "BPFL7Z999",
            FINISHING to "BPFL8Z999",
        )
    }

    @Bean
    fun getProcessNewGroup(): Map<Int, List<String>> {
        return mapOf(
            SLICING to listOf("2L0", "2L6")
        )
    }
}