package io.github.iandbrown.reconciler.logic

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

internal class PDFConverterAndroid : AbstractPDFConverter {
    private val items: Map<RectArea, String>

    constructor(source: ByteArray) {
        val document = PDDocument.load(source)
        val textStripper = MyTextStripper()

        textStripper.startPage = 0
        textStripper.endPage = document.numberOfPages - 1
        textStripper.getText(document)

        items = textStripper.areaHolder.items

        document.close()
    }

    override fun getItems(): Map<RectArea, String> = items
}

private class MyTextStripper : PDFTextStripper() {
    val areaHolder = TextAreaHolder()

    @Throws(IOException::class)
    override fun startPage(page: PDPage?) {
        val cropBox = page?.cropBox!!
        areaHolder.startPage(cropBox.lowerLeftX, cropBox.lowerLeftY, cropBox.height)
        super.startPage(page)
    }

    @Throws(IOException::class)
    override fun writeLineSeparator() {
        areaHolder.saveCurrent()
        super.writeLineSeparator()
    }

    @Throws(IOException::class)
    override fun getText(doc: PDDocument?): String? {
        sortByPosition = true
        areaHolder.clear(dropThreshold)
        return super.getText(doc)
    }

    @Throws(IOException::class)
    override fun writeWordSeparator() {
        areaHolder.saveCurrent()

        super.writeWordSeparator()
    }

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: MutableList<TextPosition?>?) {
        areaHolder.stringAt(text,
            textPositions?.asSequence()?.map { RectArea(it?.x!!, it.x + it.width, it.y, it.y + it.height) }!!
        )
        super.writeString(text, textPositions)
    }
}
