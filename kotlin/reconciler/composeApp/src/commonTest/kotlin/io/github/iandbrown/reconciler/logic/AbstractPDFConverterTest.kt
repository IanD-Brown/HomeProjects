package io.github.iandbrown.reconciler.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbstractPDFConverterTest {

    private class TestPDFConverter(private val items: Map<RectArea, String>) : AbstractPDFConverter() {
        override fun getItems(): Map<RectArea, String> = items
    }

    @Test
    fun testGetDateRange() {
        val items = mapOf(
            RectArea(0f, 0f, 0f, 0f) to "1st Jan 2022 to 31st Dec 2023",
            RectArea(10f, 10f, 10f, 10f) to "Other text"
        )
        val converter = TestPDFConverter(items)
        val range = converter.getDateRange()
        assertEquals(2022, range.first)
        assertEquals(2023, range.second)
    }

    @Test
    fun testCalcRows() {
        val items = listOf(
            RectArea(0f, 100f, 10f, 20f) to "Row1-Col1",
            RectArea(150f, 250f, 12f, 18f) to "Row1-Col2", // Overlaps Row 1
            RectArea(0f, 100f, 30f, 40f) to "Row2-Col1"
        )
        val rows = calcRows(items)
        assertEquals(2, rows.size)
        assertEquals(Range(10f, 20f), rows[0])
        assertEquals(Range(30f, 40f), rows[1])
    }

    @Test
    fun testRowContentFiltering() {
        val items = mapOf(
            RectArea(10f, 50f, 10f, 20f) to "Date",
            RectArea(60f, 100f, 10f, 20f) to "Description",
            RectArea(10f, 50f, 30f, 40f) to "2023-01-01",
            RectArea(60f, 100f, 30f, 40f) to "Lunch"
        )
        val converter = TestPDFConverter(items)

        // Filter for rows containing "Lunch"
        val rows = converter.rowContent { it.contains("Lunch") }
        assertEquals(1, rows.size)
        assertTrue(rows[0].values.contains("Lunch"))
        assertTrue(rows[0].values.contains("2023-01-01"))
    }

    @Test
    fun testMergeOverlapping() {
        // Note: mergeOverlapping is private in AbstractPDFConverter.kt.
        // We test it indirectly through rowContent.

        val sortedItems = listOf(
            RectArea(10f, 50f, 10f, 20f) to "Part 1",
            RectArea(40f, 80f, 10f, 20f) to " and Part 2",
            RectArea(100f, 150f, 10f, 20f) to "Separate"
        )
        val rowRanges = listOf(Range(10f, 20f))

        val result = rowContent(rowRanges, sortedItems) { true }
        assertEquals(1, result.size)
        val row = result[0]

        // Should have 2 entries after merge: "Part 1 and Part 2" and "Separate"
        assertEquals(2, row.size)
        assertTrue(row.values.contains("Part 1 and Part 2"))
        assertTrue(row.values.contains("Separate"))
    }

    @Test
    fun testTextAreaHolderStringAt() {
        val holder = TextAreaHolder()
        holder.stringAt("Hello", sequenceOf(RectArea(10f, 20f, 10f, 20f)))
        assertEquals("Hello", holder.currentText)
        holder.stringAt(" ", sequenceOf(RectArea(20f, 30f, 10f, 20f)))
        assertEquals("Hello ", holder.currentText)
        holder.stringAt(null, sequenceOf(RectArea(30f, 40f, 10f, 20f)))
        assertEquals(null, holder.currentText)
        holder.stringAt("World", sequenceOf(RectArea(30f, 40f, 10f, 20f)))
        assertEquals("World", holder.currentText)
    }

    @Test
    fun testCalcRowsWithLaterRowBelow() {
        val items = mapOf(
            RectArea(10f, 50f, 10f, 19f) to "Date",
            RectArea(60f, 100f, 10f, 22f) to "Description",
            RectArea(10f, 50f, 30f, 40f) to "2023-01-01",
            RectArea(60f, 100f, 30f, 40f) to "Lunch"
        )

        val sortedItems = getSortedItems(items)

        assertEquals("Date", sortedItems[0].second)
        assertEquals("Description", sortedItems[1].second)
        assertEquals("2023-01-01", sortedItems[2].second)
        assertEquals("Lunch", sortedItems[3].second)

        val rows = calcRows(sortedItems)
        assertEquals(2, rows.size)
        assertEquals(Range(10f, 22f), rows[0])
        assertEquals(Range(30f, 40f), rows[1])
    }

    @Test
    fun testCalcRowsWithInvalidTop() {
        val items = mapOf(
            RectArea(10f, 50f, Float.MAX_VALUE, 1f) to "XXX"
        )

        val rows = calcRows(getSortedItems(items))

        assertTrue { rows.isEmpty() }
    }

    @Test
    fun calcRowsWithMultiPagesAndLargeValue() {
        val converter = TestPDFConverter(testItems())

        val rows = converter.rowContent { true }
        assertTrue { rows.any { item -> item.values.contains("22.18") } }
        assertTrue { rows.any { item -> item.values.contains("10,000.00") } }
        assertTrue { rows.any { item -> item.values.contains("981.76") } }
    }
}

private fun testItems() : Map<RectArea, String> {
    return mapOf(
    Pair(RectArea(2.8346457F, 61.214066F, 9.176636F, 14.963104F), "09 01 3654483906"),
    Pair(RectArea(515.9055F, 541.1963F, 9.176636F, 14.963104F), "BX0098"),
    Pair(RectArea(393.0809F, 563.1606F, 107.289734F, 114.01076F), "Call Telephone Banking for questions or"),
    Pair(RectArea(393.0809F, 563.1607F, 117.63977F, 124.360794F), "lost or stolen cards 0330 9 123 123, open"),
    Pair(RectArea(393.08093F, 515.1486F, 127.98981F, 134.71083F), "24 hours a day 7 days a week."),
    Pair(RectArea(393.08093F, 563.16064F, 148.68988F, 155.4109F), "So that we can improve how we help you,"),
    Pair(RectArea(393.08093F, 543.1387F, 159.03992F, 165.76094F), "we might record or monitor your calls."),
    Pair(RectArea(62.36212F, 160.86774F, 163.62097F, 170.342F), "MR IAN DAVID BROWN"),
    Pair(RectArea(62.36212F, 135.88356F, 173.97101F, 180.69203F), "2 ALGARTH RISE"),
    Pair(RectArea(393.08093F, 563.16077F, 179.73999F, 186.46101F), "If you have sight or hearing loss you can"),
    Pair(RectArea(62.36212F, 87.86828F, 184.32104F, 191.04207F), "YORK"),
    Pair(RectArea(393.08087F, 553.18286F, 190.08966F, 196.81068F), "use Relay UK on 18001 0330 9 123 123"),
    Pair(RectArea(62.36212F, 141.36461F, 194.67108F, 201.3921F), "UNITED KINGDOM"),
    Pair(RectArea(62.36212F, 105.87738F, 205.02112F, 211.74214F), "YO31 1HD"),
    Pair(RectArea(393.08087F, 563.16046F, 210.50183F, 217.22285F), "Online Banking service and information"),
    Pair(RectArea(393.08084F, 527.1456F, 220.85187F, 227.57289F), "available at www.santander.co.uk"),
    Pair(RectArea(393.08084F, 516.1476F, 241.26367F, 247.9847F), "Santander Banking Operations"),
    Pair(RectArea(393.08084F, 439.10712F, 251.61371F, 258.33472F), "Sunderland"),
    Pair(RectArea(393.08084F, 434.5981F, 261.96375F, 268.68478F), "SR43 4FP"),
    Pair(RectArea(393.08084F, 508.59644F, 293.55585F, 300.28137F), "Online and Mobile Banking"),
    Pair(RectArea(393.08084F, 488.61646F, 304.8943F, 311.61533F), "ID 33418055 J BROWN"),
    Pair(RectArea(51.148663F, 203.86105F, 327.89886F, 336.0329F), "Your account summary for"),
    Pair(RectArea(51.148666F, 258.71884F, 344.03265F, 353.5752F), "5th May 2026 to 3rd Jun 2026"),
    Pair(RectArea(59.641846F, 296.19678F, 369.99814F, 377.66257F), "Santander Edge Up current account earnings"),
    Pair(RectArea(241.96513F, 284.01434F, 390.80768F, 397.29416F), "This month"),
    Pair(RectArea(297.2661F, 351.1308F, 390.80768F, 397.29416F), "Since opening"),
    Pair(RectArea(59.268066F, 235.96545F, 400.31055F, 406.79703F), "Santander Edge Up current account (cashback"),
    Pair(RectArea(59.26806F, 106.510735F, 410.08545F, 416.57193F), "and interest)"),
    Pair(RectArea(241.96509F, 267.9579F, 410.08545F, 416.57617F), "£14.04"),
    Pair(RectArea(297.26608F, 335.07373F, 410.08545F, 416.57617F), "£2,378.35"),
    Pair(RectArea(388.68314F, 545.3584F, 419.01123F, 428.55377F), "News and information"),
    Pair(RectArea(51.148605F, 324.54272F, 433.30704F, 439.79776F), "Account name MRS JACQUELINE BROWN & MR IAN DAVID BROWN"),
    Pair(RectArea(390.07166F, 522.55176F, 438.24036F, 444.49643F), "The new tax year started on 6 April"),
    Pair(RectArea(51.14859F, 351.21533F, 445.2125F, 451.69897F), "Account number: 54483906   Sort Code: 09 01 36   Statement number: 06/2026"),
    Pair(RectArea(51.14859F, 285.48462F, 457.1183F, 463.60477F), "BIC: ABBYGB2LXXX   IBAN: GB17 ABBY 0901 3654 4839 06"),
    Pair(RectArea(390.07162F, 551.03235F, 458.94043F, 465.1925F), "In April’s statement, we incorrectly stated that"),
    Pair(RectArea(51.14859F, 236.83115F, 469.02374F, 475.51022F), "Balance brought forward from 4th May Statement"),
    Pair(RectArea(321.93124F, 359.7389F, 469.02374F, 475.51022F), "£3,054.91"),
    Pair(RectArea(390.07166F, 559.04803F, 468.1402F, 474.39227F), "deposits received on or after 6 April 2026 would"),
    Pair(RectArea(51.14859F, 109.26269F, 480.9292F, 487.41568F), "Total money in:"),
    Pair(RectArea(317.20514F, 359.7387F, 480.9292F, 487.41568F), "£15,435.22"),
    Pair(RectArea(390.07166F, 563.0242F, 477.3403F, 483.59238F), "count towards the 2025/2026 tax year. To clarify,"),
    Pair(RectArea(390.07166F, 563.4641F, 486.5404F, 492.79248F), "the new tax year runs from 6 April 2026 to 5 April"),
    Pair(RectArea(51.14859F, 114.46465F, 492.83466F, 499.32114F), "Total money out:"),
    Pair(RectArea(319.10086F, 359.73895F, 492.83466F, 499.32114F), "-£6,781.73"),
    Pair(RectArea(390.07172F, 554.16016F, 495.74017F, 501.99225F), "2027. Any deposits received on or after 6 April"),
    Pair(RectArea(51.14859F, 242.48346F, 505.11926F, 511.61F), "Your balance at close of business 3rd Jun 2026"),
    Pair(RectArea(317.20514F, 359.7387F, 505.11926F, 511.61F), "£11,708.40"),
    Pair(RectArea(390.07172F, 561.6961F, 504.94028F, 511.19235F), "2026, will count towards the 2026/2027 tax year."),
    Pair(RectArea(51.14859F, 353.52753F, 523.5729F, 530.0593F), "Your overdraft limit is £2,000.00. Arranged overdraft interest rate is 39.94% EAR"),
    Pair(RectArea(51.14859F, 88.93933F, 533.3478F, 539.8342F), "(variable)."),
    Pair(RectArea(51.14859F, 335.27695F, 546.36633F, 552.6184F), "Credit interest rate: The current interest rate on your Santander Edge Up current"),
    Pair(RectArea(51.14859F, 350.41293F, 555.5664F, 561.8185F), "account is 2.10% AER/ 2.08% gross (variable) on all balances up to 25,000 GBP. To"),
    Pair(RectArea(51.14859F, 333.5248F, 564.76654F, 571.0186F), "receive interest you must meet the specific conditions in the key facts document"),
    Pair(RectArea(51.14862F, 189.84477F, 580.8605F, 587.1166F), "Interest and refunds paid this period"),
    Pair(RectArea(51.148613F, 68.04496F, 592.7659F, 599.018F), "Date"),
    Pair(RectArea(93.24282F, 176.38713F, 592.41925F, 598.6713F), "Why we are paying you"),
    Pair(RectArea(332.16336F, 359.73935F, 592.41925F, 598.6713F), "Amount"),
    Pair(RectArea(51.14862F, 77.82855F, 604.6714F, 610.92346F), "3rd Jun"),
    Pair(RectArea(93.24283F, 200.85112F, 604.6714F, 610.92346F), "Interest on your credit balance"),
    Pair(RectArea(339.7232F, 359.73962F, 604.6714F, 610.92346F), "£9.03"),
    Pair(RectArea(51.14862F, 51.14862F, 608.9822F, 611.4822F), "snjobndbsndibndibnd"),
    Pair(RectArea(414.0F, 505.14584F, 825.1995F, 831.4556F), "Continued on reverse...."),
    Pair(RectArea(56.73755F, 142.11807F, 833.51935F, 838.8334F), "MCBX00002_20260428_G_112"),
    Pair(RectArea(491.18985F, 561.2404F, 1675.8365F, 1681.854F), "Page number: 1 of  3"),
    Pair(RectArea(319.00012F, 559.0541F, 867.8076F, 873.82513F), "Account name: MRS JACQUELINE BROWN & MR IAN DAVID BROWN"),
    Pair(RectArea(319.00012F, 485.351F, 877.5872F, 883.6085F), "Account number: 54483906  (Sort Code 09 01 36)"),
    Pair(RectArea(319.00012F, 411.9856F, 887.2249F, 893.24243F), "Statement number: 06/2026"),
    Pair(RectArea(428.5007F, 496.46606F, 887.2249F, 893.24243F), "Page number: 2 of 3"),
    Pair(RectArea(265.0682F, 338.00607F, 952.072F, 958.09326F), "Important messages"),
    Pair(RectArea(54.07408F, 547.1346F, 960.697F, 966.71826F), "Important information about compensation arrangements: We’re covered by the Financial Services Compensation Scheme (‘FSCS’). The FSCS"),
    Pair(RectArea(54.07408F, 524.7319F, 969.322F, 975.33954F), "can pay compensation to depositors if a bank is unable to meet its financial obligations. The account(s) shown in this statement are eligible for"),
    Pair(RectArea(54.07408F, 544.7796F, 977.947F, 983.96454F), "compensation under the scheme. Santander UK plc is an authorised deposit taker and accepts deposits under this name and the cahoot, Santander"),
    Pair(RectArea(54.07408F, 272.10046F, 986.572F, 992.58954F), "Business and Santander Corporate & Commercial trading names."),
    Pair(RectArea(54.07408F, 490.53165F, 1003.82196F, 1009.8395F), "Further details can be found in the FSCS Information Sheet and Exclusions List, you can get a copy in your local Santander branch."),
    Pair(RectArea(102.32782F, 500.8727F, 1023.9067F, 1029.9242F), "You can find out more information about the compensation provided by the FSCS, on their website at www.FSCS.org.uk"),
    Pair(RectArea(54.074078F, 545.73975F, 1043.5112F, 1049.5325F), "For customers with an overdraft If you have a problem with your agreement, please try to resolve it with us in the first instance. If you're not happy"),
    Pair(RectArea(54.074078F, 537.0245F, 1052.1365F, 1058.154F), "with the way we handled your complaint or the result, you may be able to complain to the Financial Ombudsman Service. If you don't take up your"),
    Pair(RectArea(54.074078F, 501.40668F, 1060.7615F, 1066.779F), "problem with us first you will not be entitled to complain to the Ombudsman. We can provide details of how to contact the Ombudsman."),
    Pair(RectArea(54.074078F, 362.07092F, 1078.0115F, 1084.0327F), "You can find details of rates and charges on our website or through your local branch."),
    Pair(RectArea(54.074078F, 304.19308F, 1095.2615F, 1101.279F), "We’ll calculate interest or fees daily on any outstanding overdrawn balance."),
    Pair(RectArea(54.074078F, 535.337F, 1112.5114F, 1118.5288F), "What's AER? - AER stands for Annual Equivalent Rate and shows what the interest rate would be if we paid interest and added it to your account"),
    Pair(RectArea(54.074078F, 91.17682F, 1121.1364F, 1127.1538F), "each year. "),
    Pair(RectArea(54.074078F, 399.21857F, 1138.3862F, 1144.4038F), "What`s gross rate? - The gross rate is the interest rate we pay where no income tax has been deducted."),
    Pair(RectArea(54.074078F, 534.0846F, 1155.6366F, 1161.654F), "What's EAR? - EAR stands for Effective Annual Rate and represents the yearly cost of an overdraft, which takes account of how often we charge"),
    Pair(RectArea(54.074078F, 541.8471F, 1164.2616F, 1170.279F), "interest to the account, and doesn`t include any other fees or charges. Overdrafts depend on your circumstances and you must repay any overdraft"),
    Pair(RectArea(54.074078F, 253.74034F, 1172.8866F, 1178.904F), "when we ask in line with our General Terms and Conditions."),
    Pair(RectArea(54.074078F, 511.98172F, 1190.1365F, 1196.154F), "Santander UK plc. Registered Office: 2 Triton Square, Regent's Place, London, NW1 3AN, United Kingdom. Registered Number 2294747."),
    Pair(RectArea(54.074078F, 520.9818F, 1198.7615F, 1204.779F), "Registered in England and Wales. www.santander.co.uk. Telephone 0330 9 123 123. Calls may be recorded or monitored. Authorised by the"),
    Pair(RectArea(54.074078F, 533.8894F, 1207.3865F, 1213.404F), "Prudential Regulation Authority and regulated by the Financial Conduct Authority and the Prudential Regulation Authority. Our Financial Services"),
    Pair(RectArea(54.074078F, 543.8723F, 1216.0115F, 1222.029F), "Register number is 106054. You can check this on the Financial Services Register by visiting the FCA’s website www.fca.org.uk/register. Santander"),
    Pair(RectArea(54.074078F, 205.82254F, 1224.6364F, 1230.6538F), "and the flame logo are registered trademarks."),
    Pair(RectArea(54.074078F, 536.82196F, 1241.8864F, 1247.9038F), "Santander Edge Up current account earnings include interest received on eligible credit balances and cashback received on all eligible household"),
    Pair(RectArea(54.074078F, 536.432F, 1250.5116F, 1256.529F), "bills paid by Direct Debit. The amount shown is for the account number on this statement and does not take into account the monthly account fee."),
    Pair(RectArea(54.074078F, 69.24293F, 1268.0728F, 1273.8593F), "Date"),
    Pair(RectArea(116.4444F, 219.1405F, 1268.0728F, 1273.8593F), "Average balance for the month"),
    Pair(RectArea(522.56F, 548.99884F, 1268.0728F, 1273.8593F), "Amount"),
    Pair(RectArea(54.074066F, 77.41884F, 1277.994F, 1283.777F), "3rd Jun"),
    Pair(RectArea(116.44409F, 188.03947F, 1277.994F, 1283.777F), "Average credit balance"),
    Pair(RectArea(517.86304F, 548.9988F, 1277.994F, 1283.777F), "£5,766.10"),
    Pair(RectArea(54.074036F, 408.84415F, 1299.7311F, 1307.8651F), "Direct Debit Cashback paid from 4th May 2026 to 3rd Jun 2026"),
    Pair(RectArea(54.074036F, 69.24289F, 1309.381F, 1315.1675F), "Date"),
    Pair(RectArea(116.33319F, 193.72452F, 1309.381F, 1315.1675F), "Why we are paying you"),
    Pair(RectArea(522.56006F, 548.9989F, 1309.381F, 1315.1675F), "Amount"),
    Pair(RectArea(54.074036F, 77.41881F, 1319.5857F, 1325.3687F), "3rd Jun"),
    Pair(RectArea(116.33322F, 230.31323F, 1319.5857F, 1325.3687F), "Direct Debit Cashback for this period"),
    Pair(RectArea(531.48456F, 548.9984F, 1319.5857F, 1325.3687F), "£5.01"),
    Pair(RectArea(116.333374F, 257.55716F, 1329.2238F, 1335.0067F), "Direct Debit Cashback since account opening"),
    Pair(RectArea(523.70074F, 548.99854F, 1329.2238F, 1335.0067F), "£921.02"),
    Pair(RectArea(54.074036F, 325.50363F, 1350.9609F, 1359.095F), "Your transactions 5th May 2026 to 3rd Jun 2026"),
    Pair(RectArea(54.074036F, 68.857895F, 1360.4137F, 1366.1967F), "Date"),
    Pair(RectArea(115.99989F, 151.00658F, 1360.4137F, 1366.1967F), "Description"),
    Pair(RectArea(427.30432F, 455.70306F, 1360.4137F, 1366.1967F), "Money in"),
    Pair(RectArea(469.68668F, 502.3694F, 1360.4137F, 1366.1967F), "Money out"),
    Pair(RectArea(517.86993F, 548.9986F, 1360.4137F, 1366.1967F), "£ Balance"),
    Pair(RectArea(54.074036F, 78.97279F, 1369.9406F, 1375.7235F), "5th May"),
    Pair(RectArea(115.99961F, 282.47208F, 1369.9406F, 1375.727F), "Balance brought forward from previous statement"),
    Pair(RectArea(521.7541F, 548.99786F, 1369.9406F, 1375.7235F), "3,054.91"),
    Pair(RectArea(115.99986F, 394.08624F, 1379.467F, 1385.25F), "FASTER PAYMENTS RECEIPT REF.OCTOPUSENERG-F5CZN FROM OCTOPUS ENE"),
    Pair(RectArea(54.074036F, 78.97279F, 1383.4924F, 1389.2754F), "5th May"),
    Pair(RectArea(434.29755F, 455.70337F, 1383.4923F, 1389.2753F), "400.00"),
    Pair(RectArea(521.7558F, 548.9996F, 1383.4923F, 1389.2753F), "3,454.91"),
    Pair(RectArea(115.99986F, 139.35164F, 1387.5173F, 1393.3003F), "577964"),
    Pair(RectArea(54.074005F, 78.97276F, 1397.0442F, 1402.8271F), "8th May"),
    Pair(RectArea(115.99958F, 411.2499F, 1397.0442F, 1402.8271F), "DIRECT DEBIT PAYMENT TO VODAFONE LTD REF 7028137417-1002, MANDATE NO 0091"),
    Pair(RectArea(484.85324F, 502.3671F, 1397.0442F, 1402.8271F), "52.40"),
    Pair(RectArea(521.7526F, 548.9964F, 1397.0442F, 1402.8271F), "3,402.51"),
    Pair(RectArea(54.073975F, 78.97273F, 1406.5707F, 1412.3536F), "8th May"),
    Pair(RectArea(115.99955F, 419.81073F, 1406.5707F, 1412.3536F), "DIRECT DEBIT PAYMENT TO PRACTICE PLAN REF PB37089251 AE6B6, MANDATE NO 0084"),
    Pair(RectArea(484.85303F, 502.36688F, 1406.5707F, 1412.3536F), "17.81"),
    Pair(RectArea(521.75244F, 548.9962F, 1406.5707F, 1412.3536F), "3,384.70"),
    Pair(RectArea(54.074005F, 78.97276F, 1416.0975F, 1421.8805F), "8th May"),
    Pair(RectArea(115.99958F, 419.41876F, 1416.0975F, 1421.8805F), "DIRECT DEBIT PAYMENT TO PRACTICE PLAN REF PB48730561 AF2A6, MANDATE NO 0093"),
    Pair(RectArea(484.85303F, 502.36688F, 1416.0975F, 1421.8805F), "17.81"),
    Pair(RectArea(521.75244F, 548.9962F, 1416.0975F, 1421.8805F), "3,366.89"),
    Pair(RectArea(54.074005F, 78.97276F, 1425.6244F, 1431.4073F), "9th May"),
    Pair(RectArea(115.99958F, 382.6482F, 1425.6244F, 1431.4073F), "FASTER PAYMENTS RECEIPT REF.2 PKS photo frames FROM Leeds Schools' Foo"),
    Pair(RectArea(438.18686F, 455.7007F, 1425.6244F, 1431.4073F), "99.90"),
    Pair(RectArea(521.7528F, 548.9966F, 1425.6244F, 1431.4073F), "3,466.79"),
    Pair(RectArea(54.074005F, 78.97276F, 1435.1512F, 1440.9342F), "9th May"),
    Pair(RectArea(115.99958F, 381.26215F, 1435.1512F, 1440.9342F), "FASTER PAYMENTS RECEIPT REF.INS MUM 16.04 FROM JACQUELINE BROWN"),
    Pair(RectArea(438.18668F, 455.70053F, 1435.1512F, 1440.9342F), "21.55"),
    Pair(RectArea(521.753F, 548.99677F, 1435.1512F, 1440.9342F), "3,488.34"),
    Pair(RectArea(54.074036F, 78.97279F, 1444.6777F, 1450.4607F), "9th May"),
    Pair(RectArea(115.99961F, 389.0461F, 1444.6777F, 1450.4607F), "FASTER PAYMENTS RECEIPT REF.BGAS MUM 07.04 FROM JACQUELINE BROWN"),
    Pair(RectArea(438.18674F, 455.7006F, 1444.6777F, 1450.4607F), "28.74"),
    Pair(RectArea(521.75305F, 548.9968F, 1444.6777F, 1450.4607F), "3,517.08"),
    Pair(RectArea(54.074066F, 78.972824F, 1454.2046F, 1459.9875F), "9th May"),
    Pair(RectArea(115.99964F, 407.7009F, 1454.2046F, 1459.9875F), "FASTER PAYMENTS RECEIPT REF.WATER MAR APR MAY FROM JACQUELINE BROWN"),
    Pair(RectArea(438.18658F, 455.70044F, 1454.2046F, 1459.9875F), "45.00"),
    Pair(RectArea(521.75287F, 548.99664F, 1454.2046F, 1459.9875F), "3,562.08"),
    Pair(RectArea(54.074066F, 82.864784F, 1463.7314F, 1469.5144F), "11th May"),
    Pair(RectArea(115.999825F, 341.6148F, 1463.7314F, 1469.5144F), "DIRECT DEBIT PAYMENT TO O2 REF D7121703, MANDATE NO 0090"),
    Pair(RectArea(484.85388F, 502.36774F, 1463.7314F, 1469.5144F), "18.43"),
    Pair(RectArea(521.7533F, 548.9971F, 1463.7314F, 1469.5144F), "3,543.65"),
    Pair(RectArea(54.074066F, 82.864784F, 1473.2583F, 1479.0413F), "11th May"),
    Pair(RectArea(115.999825F, 178.62126F, 1473.2583F, 1479.0413F), "CHEQUE DEPOSIT"),
    Pair(RectArea(434.29663F, 455.70245F, 1473.2583F, 1479.0413F), "204.80"),
    Pair(RectArea(521.7549F, 548.99866F, 1473.2583F, 1479.0413F), "3,748.45"),
    Pair(RectArea(115.99989F, 445.85782F, 1482.7849F, 1488.5679F), "BILL PAYMENT VIA FASTER PAYMENT TO LEEDS SCHOOLS FO REFERENCE repay SCHOOLHIRE ,"),
    Pair(RectArea(54.074066F, 82.864784F, 1486.8102F, 1492.5931F), "14th May"),
    Pair(RectArea(484.85626F, 502.37012F, 1486.8102F, 1492.5931F), "88.00"),
    Pair(RectArea(521.7557F, 548.99945F, 1486.8102F, 1492.5931F), "3,660.45"),
    Pair(RectArea(115.99989F, 180.18233F, 1490.8351F, 1496.618F), "MANDATE NO 0115"),
    Pair(RectArea(115.99989F, 426.405F, 1500.3619F, 1506.1449F), "BILL PAYMENT VIA FASTER PAYMENT TO LEEDS SCHOOLS FO REFERENCE SCHOOLHIRE ,"),
    Pair(RectArea(54.074066F, 82.864784F, 1504.3868F, 1510.1698F), "14th May"),
    Pair(RectArea(484.85626F, 502.37012F, 1504.3868F, 1510.1698F), "88.00"),
    Pair(RectArea(521.7557F, 548.99945F, 1504.3868F, 1510.1698F), "3,572.45"),
    Pair(RectArea(115.99989F, 180.18233F, 1508.4119F, 1514.1948F), "MANDATE NO 0115"),
    Pair(RectArea(54.074066F, 82.864784F, 1517.9387F, 1523.7217F), "14th May"),
    Pair(RectArea(115.999825F, 362.7756F, 1517.9387F, 1523.7217F), "FASTER PAYMENTS RECEIPT REF.WEETWOOD FROM Leeds Schools' Foo"),
    Pair(RectArea(434.295F, 455.70084F, 1517.9387F, 1523.7217F), "488.00"),
    Pair(RectArea(521.7529F, 548.9967F, 1517.9387F, 1523.7217F), "4,060.45"),
    Pair(RectArea(54.074036F, 82.86475F, 1527.4656F, 1533.2485F), "14th May"),
    Pair(RectArea(115.999794F, 369.4045F, 1527.4656F, 1533.2485F), "FASTER PAYMENTS RECEIPT REF.ice packs U12G FROM Leeds Schools' Foo"),
    Pair(RectArea(438.1868F, 455.70065F, 1527.4656F, 1533.2485F), "24.98"),
    Pair(RectArea(521.7531F, 548.9969F, 1527.4656F, 1533.2485F), "4,085.43"),
    Pair(RectArea(54.074036F, 82.86475F, 1536.9921F, 1542.775F), "14th May"),
    Pair(RectArea(115.999794F, 383.78238F, 1536.9921F, 1542.775F), "FASTER PAYMENTS RECEIPT REF.Plates U11xmas Par FROM Leeds Schools' Foo"),
    Pair(RectArea(438.18668F, 455.70053F, 1536.9921F, 1542.775F), "13.58"),
    Pair(RectArea(521.753F, 548.99677F, 1536.9921F, 1542.775F), "4,099.01"),
    Pair(RectArea(54.074036F, 82.86475F, 1546.5189F, 1552.3019F), "15th May"),
    Pair(RectArea(115.999794F, 295.72318F, 1546.5189F, 1552.3019F), "BANK GIRO CREDIT REF WYPF 871625, WYPF 871625"),
    Pair(RectArea(434.2957F, 455.7015F, 1546.5189F, 1552.3019F), "960.93"),
    Pair(RectArea(521.7536F, 548.9974F, 1546.5189F, 1552.3019F), "5,059.94"),
    Pair(RectArea(54.074036F, 82.86475F, 1556.0458F, 1561.8287F), "15th May"),
    Pair(RectArea(115.999794F, 343.9808F, 1556.0458F, 1561.8287F), "BANK GIRO CREDIT REF WK749193D DWP SP, 000000003772212369"),
    Pair(RectArea(434.29507F, 455.7009F, 1556.0458F, 1561.8287F), "965.20"),
    Pair(RectArea(521.7533F, 548.9971F, 1556.0458F, 1561.8287F), "6,025.14"),
    Pair(RectArea(54.074005F, 82.86472F, 1565.5728F, 1571.3557F), "15th May"),
    Pair(RectArea(115.99976F, 392.14722F, 1565.5728F, 1571.3557F), "DIRECT DEBIT PAYMENT TO NATIONAL TRUST REF 49TNCXR, MANDATE NO 0092"),
    Pair(RectArea(484.85333F, 502.3672F, 1565.5728F, 1571.3557F), "14.00"),
    Pair(RectArea(521.75275F, 548.9965F, 1565.5728F, 1571.3557F), "6,011.14"),
    Pair(RectArea(54.074005F, 82.86472F, 1575.0991F, 1580.8821F), "18th May"),
    Pair(RectArea(115.99976F, 430.7029F, 1575.0991F, 1580.8821F), "DIRECT DEBIT PAYMENT TO YORKSHIRE WATER REF 5298468800100000, MANDATE NO 0080"),
    Pair(RectArea(484.8531F, 502.36694F, 1575.0991F, 1580.8821F), "20.00"),
    Pair(RectArea(521.7525F, 548.9963F, 1575.0991F, 1580.8821F), "5,991.14"),
    Pair(RectArea(54.074036F, 82.86475F, 1584.6261F, 1590.409F), "18th May"),
    Pair(RectArea(115.999794F, 434.9728F, 1584.6261F, 1590.409F), "BILL PAYMENT VIA FASTER PAYMENT TO AMANDA REFERENCE mortgage , MANDATE NO 0091"),
    Pair(RectArea(480.96103F, 502.36685F, 1584.6261F, 1590.409F), "442.00"),
    Pair(RectArea(521.7524F, 548.99615F, 1584.6261F, 1590.409F), "5,549.14"),
    Pair(RectArea(54.074005F, 82.86472F, 1594.153F, 1599.9359F), "20th May"),
    Pair(RectArea(115.99976F, 343.59576F, 1594.153F, 1599.9359F), "BANK GIRO CREDIT REF WK637942A DWP SP, 000000003780417957"),
    Pair(RectArea(428.4571F, 455.70087F, 1594.153F, 1599.9359F), "1,070.36"),
    Pair(RectArea(521.7533F, 548.9971F, 1594.153F, 1599.9359F), "6,619.50"),
    Pair(RectArea(54.074005F, 82.86472F, 1603.6798F, 1609.4628F), "20th May"),
    Pair(RectArea(115.99976F, 453.2496F, 1603.6798F, 1609.4628F), "DIRECT DEBIT PAYMENT TO HOME INSURANCELBIS REF HBP603117218-EE7D7, MANDATE NO 0097"),
    Pair(RectArea(484.85275F, 502.3666F, 1603.6798F, 1609.4628F), "22.18"),
    Pair(RectArea(521.75214F, 548.9959F, 1603.6798F, 1609.4628F), "6,597.32"),
    Pair(RectArea(54.074005F, 82.86472F, 1613.2067F, 1618.9896F), "20th May"),
    Pair(RectArea(115.99976F, 178.6212F, 1613.2067F, 1618.9896F), "CHEQUE DEPOSIT"),
    Pair(RectArea(438.1887F, 455.70255F, 1613.2067F, 1618.9896F), "79.20"),
    Pair(RectArea(521.755F, 548.9988F, 1613.2067F, 1618.9896F), "6,676.52"),
    Pair(RectArea(54.074036F, 82.47276F, 1622.7332F, 1628.5161F), "21st May"),
    Pair(RectArea(115.99978F, 431.85788F, 1622.7332F, 1628.5161F), "DIRECT DEBIT PAYMENT TO DIAMOND RESORTS MA REF L000001692935, MANDATE NO 0095"),
    Pair(RectArea(480.96097F, 502.3668F, 1622.7332F, 1628.5161F), "636.74"),
    Pair(RectArea(521.7523F, 548.9961F, 1622.7332F, 1628.5161F), "6,039.78"),
    Pair(RectArea(54.074005F, 82.86472F, 1632.26F, 1638.043F), "27th May"),
    Pair(RectArea(115.99976F, 422.1209F, 1632.26F, 1638.043F), "DIRECT DEBIT PAYMENT TO OCTOPUS ENERGY REF A-BC5002CA-001, MANDATE NO 0094"),
    Pair(RectArea(480.9611F, 502.3669F, 1632.26F, 1638.043F), "114.00"),
    Pair(RectArea(521.75244F, 548.9962F, 1632.26F, 1638.043F), "5,925.78"),
    Pair(RectArea(54.073975F, 76.641754F, 1641.787F, 1647.57F), "1st Jun"),
    Pair(RectArea(115.99952F, 349.75537F, 1641.787F, 1647.57F), "BANK GIRO CREDIT REF TRUSTEES OF EMERSO, IAN DAVID BROWN"),
    Pair(RectArea(434.2951F, 455.70093F, 1641.787F, 1647.57F), "981.76"),
    Pair(RectArea(521.75336F, 548.99713F, 2483.6768F, 2489.4597F), "6,907.54"),
    Pair(RectArea(319.00012F, 559.0541F, 1709.6975F, 1715.7151F), "Account name: MRS JACQUELINE BROWN & MR IAN DAVID BROWN"),
    Pair(RectArea(319.00012F, 485.351F, 1719.477F, 1725.4983F), "Account number: 54483906  (Sort Code 09 01 36)"),
    Pair(RectArea(319.00012F, 411.9856F, 1729.1147F, 1735.1323F), "Statement number: 06/2026"),
    Pair(RectArea(428.5007F, 496.46606F, 1729.1147F, 1735.1323F), "Page number: 3 of 3"),
    Pair(RectArea(54.074074F, 68.85793F, 1765.6887F, 1771.4717F), "Date"),
    Pair(RectArea(115.99992F, 151.0066F, 1765.6887F, 1771.4717F), "Description"),
    Pair(RectArea(427.30438F, 455.70312F, 1765.6887F, 1771.4717F), "Money in"),
    Pair(RectArea(469.68674F, 502.36945F, 1765.6887F, 1771.4717F), "Money out"),
    Pair(RectArea(517.87F, 548.99866F, 1765.6887F, 1771.4717F), "£ Balance"),
    Pair(RectArea(54.074097F, 76.64188F, 1775.2153F, 1780.9983F), "1st Jun"),
    Pair(RectArea(115.99964F, 417.8439F, 1775.2153F, 1780.9983F), "DIRECT DEBIT PAYMENT TO COM WORKERS UNION REF 1000219874, MANDATE NO 0100"),
    Pair(RectArea(488.74524F, 502.36713F, 1775.2153F, 1780.9983F), "6.69"),
    Pair(RectArea(521.7527F, 548.99646F, 1775.2153F, 1780.9983F), "6,900.85"),
    Pair(RectArea(54.074097F, 76.64188F, 1784.7422F, 1790.5251F), "1st Jun"),
    Pair(RectArea(115.99964F, 415.13492F, 1784.7422F, 1790.5251F), "DIRECT DEBIT PAYMENT TO CITY OF YORK GENER REF 8824880368, MANDATE NO 0081"),
    Pair(RectArea(480.96112F, 502.36694F, 1784.7422F, 1790.5251F), "281.00"),
    Pair(RectArea(521.7528F, 548.9966F, 1784.7422F, 1790.5251F), "6,619.85"),
    Pair(RectArea(54.074066F, 76.641846F, 1794.269F, 1800.052F), "1st Jun"),
    Pair(RectArea(115.99961F, 452.87155F, 1794.269F, 1800.052F), "DIRECT DEBIT PAYMENT TO SANTANDERCARDS LTD REF 001582025000016870, MANDATE NO 0054"),
    Pair(RectArea(475.12277F, 502.36655F, 1794.269F, 1800.052F), "4,472.77"),
    Pair(RectArea(521.7521F, 548.99585F, 1794.269F, 1800.052F), "2,147.08"),
    Pair(RectArea(54.074066F, 76.641846F, 1803.7959F, 1809.5789F), "1st Jun"),
    Pair(RectArea(115.99961F, 397.24304F, 1803.7959F, 1809.5789F), "DIRECT DEBIT PAYMENT TO TV LICENCE MBP REF 3335832709, MANDATE NO 0096"),
    Pair(RectArea(484.85336F, 502.36722F, 1803.7959F, 1809.5789F), "15.03"),
    Pair(RectArea(521.75275F, 548.9965F, 1803.7959F, 1809.5789F), "2,132.05"),
    Pair(RectArea(54.074036F, 76.641815F, 1813.3225F, 1819.1055F), "1st Jun"),
    Pair(RectArea(115.99958F, 406.58093F, 1813.3225F, 1819.1055F), "DIRECT DEBIT PAYMENT TO V12 RETAIL FINANCE REF 024189801, MANDATE NO 0101"),
    Pair(RectArea(480.96115F, 502.36697F, 1813.3225F, 1819.1055F), "134.87"),
    Pair(RectArea(521.75287F, 548.99664F, 1813.3225F, 1819.1055F), "1,997.18"),
    Pair(RectArea(54.074036F, 76.641815F, 1822.8494F, 1828.6323F), "1st Jun"),
    Pair(RectArea(115.99958F, 431.47272F, 1822.8494F, 1828.6323F), "DIRECT DEBIT PAYMENT TO YORKSHIRE WATER REF 516401670000000X, MANDATE NO 0099"),
    Pair(RectArea(484.85312F, 502.36697F, 1822.8494F, 1828.6323F), "15.00"),
    Pair(RectArea(521.7525F, 548.9963F, 1822.8494F, 1828.6323F), "1,982.18"),
    Pair(RectArea(54.074036F, 76.641815F, 1832.3762F, 1838.1592F), "1st Jun"),
    Pair(RectArea(115.99958F, 375.8162F, 1832.3762F, 1838.1592F), "FASTER PAYMENTS RECEIPT REF.ASSISTANCE FROM JACQUELINE BROWN"),
    Pair(RectArea(424.56482F, 455.70056F, 1832.3762F, 1838.1592F), "10,000.00"),
    Pair(RectArea(517.86084F, 548.9966F, 1832.3762F, 1838.1592F), "11,982.18"),
    Pair(RectArea(54.073975F, 76.641754F, 1841.9031F, 1847.686F), "1st Jun"),
    Pair(RectArea(115.99952F, 373.86313F, 1841.9031F, 1847.686F), "FASTER PAYMENTS RECEIPT REF.MAY WATER FROM JACQUELINE BROWN"),
    Pair(RectArea(438.187F, 455.70087F, 1841.9031F, 1847.686F), "15.00"),
    Pair(RectArea(517.86084F, 548.9966F, 1841.9031F, 1847.686F), "11,997.18"),
    Pair(RectArea(54.073944F, 76.64172F, 1851.4299F, 1857.2129F), "1st Jun"),
    Pair(RectArea(115.99949F, 360.25525F, 1851.4299F, 1857.2129F), "FASTER PAYMENTS RECEIPT REF.MAY INS FROM JACQUELINE BROWN"),
    Pair(RectArea(438.187F, 455.70087F, 1851.4299F, 1857.2129F), "22.18"),
    Pair(RectArea(517.8612F, 548.99695F, 1851.4299F, 1857.2129F), "12,019.36"),
    Pair(RectArea(115.9998F, 440.02673F, 1860.9563F, 1866.7393F), "BILL PAYMENT VIA FASTER PAYMENT TO JOSEPH REILLY REFERENCE 2 Algarth , MANDATE NO"),
    Pair(RectArea(54.073975F, 76.641754F, 1864.9817F, 1870.7646F), "1st Jun"),
    Pair(RectArea(480.96442F, 502.37024F, 1864.9817F, 1870.7646F), "320.00"),
    Pair(RectArea(517.86365F, 548.9994F, 1864.9817F, 1870.7646F), "11,699.36"),
    Pair(RectArea(115.9998F, 131.56764F, 1869.0066F, 1874.7896F), "0121"),
    Pair(RectArea(54.073975F, 77.41875F, 1879.7817F, 1885.5647F), "3rd Jun"),
    Pair(RectArea(115.99964F, 270.39722F, 1879.7817F, 1885.5647F), "MAINTAINING THE ACCOUNT - MONTHLY FEE"),
    Pair(RectArea(488.74667F, 502.36856F, 1879.7817F, 1885.5647F), "5.00"),
    Pair(RectArea(517.862F, 548.99774F, 1879.7817F, 1885.5647F), "11,694.36"),
    Pair(RectArea(54.073975F, 77.41875F, 1890.5569F, 1896.3398F), "3rd Jun"),
    Pair(RectArea(115.99964F, 265.7423F, 1890.5569F, 1896.3398F), "INTEREST PAID AFTER TAX 0.00 DEDUCTED"),
    Pair(RectArea(442.08F, 455.70187F, 1890.5569F, 1896.3398F), "9.03"),
    Pair(RectArea(517.8622F, 548.9979F, 1890.5569F, 1896.3398F), "11,703.39"),
    Pair(RectArea(54.073975F, 77.41875F, 1900.0834F, 1905.8663F), "3rd Jun"),
    Pair(RectArea(115.99964F, 254.11543F, 1900.0834F, 1905.8663F), "6 Direct Debit Payments at 1,00% Cashback"),
    Pair(RectArea(442.0801F, 455.702F, 1900.0834F, 1905.8663F), "5.01"),
    Pair(RectArea(517.8623F, 548.99805F, 1900.0834F, 1905.8663F), "11,708.40"),
    Pair(RectArea(54.073944F, 79.3577F, 1909.6102F, 1915.3967F), "3rd Jun"),
    Pair(RectArea(115.99963F, 257.97238F, 1909.6102F, 1915.3967F), "Balance carried forward to next statement:"),
    )
}
