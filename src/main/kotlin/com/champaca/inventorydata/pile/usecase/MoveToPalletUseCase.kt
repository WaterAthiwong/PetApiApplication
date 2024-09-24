package com.champaca.inventorydata.pile.usecase

import com.champaca.inventorydata.ExposedInfoLogger
import com.champaca.inventorydata.databasetable.Pile
import com.champaca.inventorydata.pallet.PalletRepository
import com.champaca.inventorydata.pile.PileError
import com.champaca.inventorydata.pile.request.MoveToPalletRequest
import com.champaca.inventorydata.pile.response.MoveToPalletResponse
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.sql.DataSource

@Service
class MoveToPalletUseCase(
    val dataSource: DataSource,
    val palletRepository: PalletRepository
) {
    fun execute(userId: String, request: MoveToPalletRequest): MoveToPalletResponse {
        Database.connect(dataSource)

        var errorType = PileError.NONE
        var count = 0
        transaction {
            addLogger(ExposedInfoLogger)

            val pallet = palletRepository.findByCode(request.palletCode)
            if (pallet == null) {
                errorType = PileError.PALLET_NOT_FOUND
                return@transaction
            }

            val now = LocalDateTime.now()
            count = Pile.update({ Pile.code.inList(request.pileCodes) }) {
                it[this.palletId] = pallet.id.value
                it[this.updatedAt] = now
            }
        }

        return if (errorType == PileError.NONE) {
            return MoveToPalletResponse.Success(count = count)
        } else {
            return MoveToPalletResponse.Failure(errorType = errorType)
        }
    }
}