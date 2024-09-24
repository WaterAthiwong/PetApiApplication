package com.champaca.inventorydata.masterdata.species.usecase

import com.champaca.inventorydata.masterdata.model.SimpleData
import com.champaca.inventorydata.model.Species
import org.springframework.stereotype.Service

@Service
class GetSpeciesUseCase {

    val speciesForProcess = mapOf<String, List<Species>>(
        "SM" to listOf(Species.PT, Species.OA),
        "WH" to listOf(Species.AF, Species.AH, Species.AP, Species.BE, Species.BG, Species.BR, Species.CH, Species.DA,
            Species.DG, Species.DS, Species.EU, Species.HK, Species.IR, Species.JA, Species.LP, Species.MA, Species.MH,
            Species.MK, Species.MP, Species.NR, Species.NT, Species.OA, Species.OV, Species.PA, Species.PN, Species.PM,
            Species.PT, Species.RF, Species.RP, Species.SD, Species.SP, Species.ST, Species.TB, Species.TN, Species.WE,
            Species.WN, Species.WO, Species.YA),
        "RC" to listOf(Species.AH, Species.DA, Species.DS, Species.MA, Species.MK, Species.NT, Species.OA, Species.PT, Species.SD, Species.TB, Species.WN, Species.YA),
        "RM" to listOf(Species.AH, Species.BR, Species.CH, Species.DA, Species.DG, Species.DS, Species.HK, Species.JA, Species.MH, Species.MK,
            Species.MP, Species.MX, Species.NT, Species.OA, Species.OV, Species.PA, Species.PN, Species.PT, Species.PW, Species.RF, Species.SD,
            Species.TB, Species.TE, Species.WE, Species.WN),
        "PR" to listOf(Species.AH, Species.BR, Species.CH, Species.DA, Species.DG, Species.DS, Species.HK, Species.JA, Species.MH, Species.MK,
            Species.MP, Species.MX, Species.NT, Species.OA, Species.OV, Species.PA, Species.PN, Species.PT, Species.PW, Species.RF,
            Species.TB, Species.TE, Species.WE, Species.WN),
        "SD" to listOf(Species.AH, Species.BR, Species.CH, Species.DA, Species.DG, Species.DS, Species.HK, Species.JA, Species.MH, Species.MK,
            Species.MP, Species.MX, Species.NT, Species.OA, Species.OV, Species.PA, Species.PN, Species.PT, Species.PW, Species.RF,
            Species.TB, Species.TE, Species.WE, Species.WN),
        "FN" to listOf(Species.AH, Species.BR, Species.CH, Species.DA, Species.DG, Species.DS, Species.HK, Species.JA, Species.MH, Species.MK,
            Species.MP, Species.MX, Species.NT, Species.OA, Species.OV, Species.PA, Species.PN, Species.PT, Species.PW, Species.RF,
            Species.TB, Species.TE, Species.WE, Species.WN),
    )

    fun execute(processPrefix: String?): List<SimpleData> {
        val species = if (speciesForProcess.containsKey(processPrefix)) {
            speciesForProcess[processPrefix]!!
        } else {
            Species.values().toList()
        }
        return species.map {
            SimpleData(id = null, code = it.name, name = it.longName)
        }
    }
}
