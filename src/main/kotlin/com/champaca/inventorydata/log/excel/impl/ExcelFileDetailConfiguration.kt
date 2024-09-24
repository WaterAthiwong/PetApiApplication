package com.champaca.inventorydata.log.excel.impl

import com.champaca.inventorydata.log.excel.LogFileDataPosition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExcelFileDetailConfiguration {

    @Bean
    fun getParkExcelFileDetails(
        tungKwian: TungKwianDataPosition,
        maeMoh: MaeMohDataPosition,
        huaiPlaDook: HuaiPlaDookDataPosition,
        maeSin: MaeSinMaeSoongDataPosition,
        yangKao: YangKaoDataPosition,
        srisat: SrisatchanalaiDataPosition,
        pakPat: PakPatDataPosition,
        thongPaPoom: ThongPaPoomDataPosition,
        klongSuanMak: KlongSuanMakDataPosition,
    ): Map<Int, LogFileDataPosition> {
        // The park name e.g. สวนป่าทุ่งเกวียน, สวนป่าน้ำสวยห้วยปลาดุก match with supplier table's "name" column.
        return mapOf(
            7 to tungKwian, // สวนป่าทุ่งเกวียน
            41 to maeMoh, // สวนป่าแม่เมาะ
            9 to huaiPlaDook, //สวนป่าน้ำสวยห้วยปลาดุก
            13 to maeSin, // สวนป่าแม่สิน-แม่สูง
            14 to yangKao, // สวนป่ายางขาว
            16 to srisat, // สวนป่าศรีสัชนาลัย
            11 to pakPat, // สวนป่าปากปาด
            5 to thongPaPoom, // สวนป่าทองผาภูมิ
            52 to huaiPlaDook, // ไม้เอกชน อำเภอ วังชิ้น จังหวัดแพร่ ใช้เหมือน สวนป่าน้ำสวยห้วยปลาดุก
            54 to thongPaPoom, // สวนป่าห้วยเขย่ง ใช้เหมือน สวนป่าทองผาภูมิ //2024-06-12
            1369 to huaiPlaDook, // ไม้เอกชน อำเภอ น้ำยืน จังหวัดอุบลราชธานี ใช้เหมือน สวนป่าน้ำสวยห้วยปลาดุก //2024-08-29
            1376 to huaiPlaDook, // ไม้เอกชน อำเภอเมือง จังหวัดน่าน  ใช้เหมือน สวนป่าน้ำสวยห้วยปลาดุก //2024-09-19
            4 to klongSuanMak, // สวนป่าคลองสวนหมาก ใช้เหมือน สวนป่าน้ำสวยห้วยปลาดุก เปลี่ยนแถวเริ่มต้น //2024-09-23
        )
    }
}