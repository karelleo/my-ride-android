package com.myride.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var db: ReceiptDb
    private lateinit var root: LinearLayout
    private var currentTab = "Beranda"
    private var selectedAttachment = ""
    private val green = Color.rgb(0, 170, 91)
    private val ink = Color.rgb(28, 32, 36)
    private val muted = Color.rgb(111, 119, 126)
    private val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ReceiptDb(this)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        showHome()
    }

    private fun base(title: String, subtitle: String? = null) {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(246,247,248)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(20, 18, 16, 14); setBackgroundColor(Color.WHITE) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        texts.addView(label(title, 23f, ink, true))
        if (subtitle != null) texts.addView(label(subtitle, 12f, muted, false))
        header.addView(texts)
        val more = TextView(this).apply { text = "⋮"; textSize = 28f; setTextColor(ink); gravity = Gravity.CENTER; setPadding(14,0,8,0); setOnClickListener { showMore() } }
        header.addView(more)
        root.addView(header)
        setContentView(root)
    }

    private fun label(s: String, size: Float, color: Int = ink, bold: Boolean = false): TextView = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun card(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,14,16,14); setBackgroundColor(Color.WHITE); elevation = 2f }
    private fun addGap(parent: LinearLayout, h: Int = 10) { parent.addView(View(this), LinearLayout.LayoutParams(1,h)) }
    private fun money(v: Long) = "Rp${nf.format(v)}"

    private fun showHome() {
        currentTab = "Beranda"; base("Halo! 👋", "Semua nota perjalanan dan pesanan dalam satu tempat")
        val scroll = ScrollView(this); val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,14,16,18) }
        val search = EditText(this).apply { hint = "Cari nota, lokasi, atau transaksi"; setSingleLine(true); textSize = 14f; setPadding(14,4,14,4); setBackgroundColor(Color.WHITE) }
        body.addView(search, LinearLayout.LayoutParams(-1,52)); addGap(body,10)
        val uploadHome = Button(this).apply {
            text = "＋  Upload nota PDF / foto"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { showMore() }
        }
        body.addView(uploadHome, LinearLayout.LayoutParams(-1,50)); addGap(body,14)
        body.addView(label("Layanan",18f,ink,true)); addGap(body,8)
        val services = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        services.addView(serviceTile("🚗","GoCar"), LinearLayout.LayoutParams(0,104,1f)); services.addView(serviceTile("🍜","GoFood"), LinearLayout.LayoutParams(0,104,1f)); services.addView(serviceTile("📦","GoSend"), LinearLayout.LayoutParams(0,104,1f)); services.addView(serviceTile("⋯","Lainnya"), LinearLayout.LayoutParams(0,104,1f))
        body.addView(services); addGap(body,16)
        body.addView(label("Aktivitas terakhir",18f,ink,true)); addGap(body,8)
        val recentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun refreshRecent(q: String = "") {
            recentContainer.removeAllViews()
            val filtered = db.all().filter { listOf(it.service,it.date,it.origin,it.destination,it.driver).joinToString(" ").contains(q, true) }.take(8)
            if (filtered.isEmpty()) recentContainer.addView(emptyCard("Belum ada nota", "Pilih menu ⋮ untuk upload screenshot, foto, atau PDF nota."))
            else filtered.forEach { recentContainer.addView(receiptCard(it) { showDetail(it.id) }); addGap(recentContainer,8) }
        }
        refreshRecent()
        body.addView(recentContainer)
        search.addTextChangedListener(SimpleWatcher { refreshRecent(it) })
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f)); root.addView(bottomNav())
    }

    private fun serviceTile(emoji: String, name: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(4,12,4,12)
        val e = label(emoji,27f); e.gravity = Gravity.CENTER; addView(e)
        val t = label(name,12f,ink,true); t.gravity = Gravity.CENTER; addView(t)
        setOnClickListener { showActivity(name) }
    }

    private fun emptyCard(title: String, desc: String) = card().apply { addView(label(title,16f,ink,true)); addGap(this,4); addView(label(desc,13f,muted)) }

    private fun receiptCard(r: Receipt, click: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(14,13,14,13); setBackgroundColor(Color.WHITE); elevation = 1f
        val icon = label(if (r.service.contains("Food",true)) "🍜" else "🚗",25f); addView(icon)
        val info = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(12,0,4,0); layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
        info.addView(label(r.service,15f,ink,true)); info.addView(label(r.date,11f,muted)); info.addView(label(if (r.origin.isNotBlank() || r.destination.isNotBlank()) "${r.origin} → ${r.destination}" else "Nota tersimpan",12f,muted))
        addView(info); addView(label(money(r.amount),14f,ink,true)); setOnClickListener { click() }
    }

    private fun showActivity(filter: String = "Semua") {
        currentTab = "Aktivitas"; base("Aktivitas", "Riwayat nota yang tersimpan di perangkat")
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,12,16,12) }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Semua","GoCar","GoFood","GoSend").forEach { c -> val b = Button(this).apply { text=c; textSize=11f; isAllCaps=false; setOnClickListener { showActivity(c) } }; chips.addView(b) }
        body.addView(chips)
        addGap(body,8)
        val uploadActivity = Button(this).apply {
            text = "＋  Upload nota PDF / foto"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener { showMore() }
        }
        body.addView(uploadActivity, LinearLayout.LayoutParams(-1,48))
        addGap(body,8)
        val search = EditText(this).apply { hint="Cari transaksi atau lokasi"; setSingleLine(true) }
        body.addView(search)
        val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        fun refresh(q:String="") { list.removeAllViews(); val rows=db.all().filter { (filter=="Semua" || it.service.equals(filter,true)) && listOf(it.service,it.date,it.origin,it.destination,it.driver).joinToString(" ").contains(q,true) }; if(rows.isEmpty()) list.addView(emptyCard("Nota tidak ditemukan","Tekan Upload nota untuk memilih PDF atau foto bukti transaksi.")) else rows.forEach { list.addView(receiptCard(it){showDetail(it.id)}); addGap(list,8) } }
        refresh(); search.addTextChangedListener(SimpleWatcher { refresh(it) })
        val sc=ScrollView(this); sc.addView(list); body.addView(sc,LinearLayout.LayoutParams(-1,0,1f)); root.addView(body,LinearLayout.LayoutParams(-1,0,1f)); root.addView(bottomNav())
    }

    private fun showMap() {
        currentTab="Maps"; base("Maps", "Peta dan simulasi rute historis")
        val map = MapView(this); map.setTileSource(TileSourceFactory.MAPNIK); map.setMultiTouchControls(true)
        val start=GeoPoint(-6.2297,106.8295); val end=GeoPoint(-6.2146,106.8451)
        map.controller.setZoom(13.5); map.controller.setCenter(start)
        val line=Polyline().apply { setPoints(listOf(start,GeoPoint(-6.225,106.835),GeoPoint(-6.220,106.840),end)); outlinePaint.color=green; outlinePaint.strokeWidth=9f }
        map.overlays.add(line)
        val a=Marker(map).apply { position=start; title="Penjemputan"; setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM) }
        val b=Marker(map).apply { position=end; title="Tujuan"; setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM) }
        map.overlays.add(a); map.overlays.add(b)
        root.addView(map,LinearLayout.LayoutParams(-1,0,1f))
        val note=card(); note.addView(label("Contoh rute historis",15f,ink,true)); addGap(note,4); note.addView(label("Peta menggunakan OpenStreetMap. Rute contoh ini bukan lokasi GPS langsung.",12f,muted)); root.addView(note); root.addView(bottomNav())
    }

    private fun showDetail(id: Long) {
        val r=db.find(id) ?: return
        currentTab="Detail"; base("Detail nota",r.service+" • "+r.date)
        val sc=ScrollView(this); val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,14,16,18) }
        val total=card(); total.addView(label("Total pembayaran",13f,muted)); addGap(total,5); total.addView(label(money(r.amount),27f,ink,true)); body.addView(total); addGap(body,12)
        val route=card(); route.addView(label("Rute perjalanan",17f,ink,true)); addGap(route,8); route.addView(label("●  ${r.origin.ifBlank { "Lokasi penjemputan belum tersedia" }}",14f)); addGap(route,7); route.addView(label("●  ${r.destination.ifBlank { "Lokasi tujuan belum tersedia" }}",14f)); body.addView(route); addGap(body,12)
        val details=card(); details.addView(label("Informasi transaksi",17f,ink,true)); addGap(details,8)
        listOf("Layanan" to r.service,"Tanggal" to r.date,"Driver" to r.driver.ifBlank { "Belum tersedia" },"Nomor kendaraan" to r.plate.ifBlank { "Belum tersedia" },"Catatan" to r.note.ifBlank { "-" }).forEach { details.addView(label(it.first,11f,muted)); details.addView(label(it.second,14f,ink,true)); addGap(details,7) }
        body.addView(details); addGap(body,12)
        val map=MapView(this); map.setTileSource(TileSourceFactory.MAPNIK); map.setMultiTouchControls(true); val p1=GeoPoint(-6.2297,106.8295); val p2=GeoPoint(-6.2146,106.8451); map.controller.setZoom(13.5); map.controller.setCenter(p1); map.overlays.add(Polyline().apply { setPoints(listOf(p1,GeoPoint(-6.222,106.838),p2)); outlinePaint.color=green; outlinePaint.strokeWidth=8f }); body.addView(map,LinearLayout.LayoutParams(-1,420)); addGap(body,10)
        val download=Button(this).apply { text="Download nota PDF"; isAllCaps=false; setTextColor(Color.WHITE); setBackgroundColor(green); setOnClickListener { savePdf(r) } }; body.addView(download)
        sc.addView(body); root.addView(sc,LinearLayout.LayoutParams(-1,0,1f)); root.addView(bottomNav())
    }

    private fun showMore() {
        val options=arrayOf("Upload PDF nota","Upload foto / screenshot nota","Batal")
        AlertDialog.Builder(this).setTitle("Upload nota").setItems(options) { _, which ->
            when(which) { 0 -> pickFile("application/pdf"); 1 -> pickFile("image/*") }
        }.show()
    }

    private fun pickFile(type: String) {
        val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); this.type=type; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
        startActivityForResult(i,1001)
    }

    @Deprecated("Legacy activity result used to keep compatibility with the simple Activity implementation")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==1002 && resultCode==RESULT_OK) {
            val uri=data?.data
            val receipt=pendingPdf
            if(uri!=null && receipt!=null) writePdf(uri,receipt)
            pendingPdf=null
            return
        }
        if(requestCode==1001 && resultCode==RESULT_OK) {
            val uri=data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_:Exception) {}
            selectedAttachment=uri.toString()
            val mime=contentResolver.getType(uri) ?: ""
            if(mime.equals("application/pdf",true) || mime.endsWith("/pdf",true)) {
                readFirstPdfPage(uri)
            } else {
                try {
                    val image=InputImage.fromFilePath(this,uri)
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                        .addOnSuccessListener { showImportForm(uri,it.text) }
                        .addOnFailureListener { showImportForm(uri,"") }
                } catch (_:Exception) { showImportForm(uri,"") }
            }
        }
    }

    // PDF import deliberately reads page 1 only; later invoice pages are ignored.
    private fun readFirstPdfPage(uri: Uri) {
        try {
            val descriptor = contentResolver.openFileDescriptor(uri, "r")
            if (descriptor == null) {
                showImportForm(uri, "")
                return
            }
            val renderer = PdfRenderer(descriptor)
            if (renderer.pageCount < 1) {
                renderer.close()
                descriptor.close()
                showImportForm(uri, "")
                return
            }
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(
                (page.width * 2).coerceAtLeast(1),
                (page.height * 2).coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            descriptor.close()

            val image = InputImage.fromBitmap(bitmap, 0)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener {
                    bitmap.recycle()
                    showImportForm(uri, it.text)
                }
                .addOnFailureListener {
                    bitmap.recycle()
                    showImportForm(uri, "")
                }
        } catch (e: Exception) {
            Toast.makeText(this, "PDF tidak bisa dibaca: ${e.message}", Toast.LENGTH_LONG).show()
            showImportForm(uri, "")
        }
    }

    private fun showImportForm(uri: Uri, ocr: String) {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,8,20,0) }
        val serviceName = when {
            ocr.contains("GoCar Comfort",true) -> "GoCar Comfort"
            ocr.contains("GoCar",true) -> "GoCar"
            ocr.contains("GoRide",true) -> "GoRide"
            ocr.contains("GoFood",true) -> "GoFood"
            ocr.contains("GoSend",true) -> "GoSend"
            else -> "Gojek"
        }
        val dateMatch = Regex(
            "(?i)(?:Senin|Selasa|Rabu|Kamis|Jumat|Sabtu|Minggu)?[,]?\\s*(\\d{1,2}\\s+(?:Januari|Februari|Maret|April|Mei|Juni|Juli|Agustus|September|Oktober|November|Desember)\\s+\\d{4})"
        ).find(ocr)?.groupValues?.getOrNull(1)
        val totalMatch = Regex(
            "(?is)Total\\s+(?:dibayar|pembayaran)\\s*.{0,90}?Rp\\s*([0-9.]+)"
        ).find(ocr)?.groupValues?.getOrNull(1)
            ?: Regex("(?i)Total dibayar\\s*Rp\\s*([0-9.]+)").find(ocr)?.groupValues?.getOrNull(1)
        val orderNo = Regex("(?i)ID pesanan\\s*:\\s*([A-Z0-9-]+)").find(ocr)?.groupValues?.getOrNull(1).orEmpty()

        val service=EditText(this).apply { hint="Layanan"; setText(serviceName) }
        val date=EditText(this).apply { hint="Tanggal transaksi"; setText(dateMatch ?: "") }
        val amount=EditText(this).apply {
            hint="Nominal total dibayar (angka saja)"
            inputType=android.text.InputType.TYPE_CLASS_NUMBER
            val digits = totalMatch?.filter { it.isDigit() }
            if (!digits.isNullOrBlank()) setText(digits)
        }
        val origin=EditText(this).apply { hint="Lokasi penjemputan / asal" }
        val dest=EditText(this).apply { hint="Lokasi tujuan" }
        val driver=EditText(this).apply { hint="Nama driver / merchant" }
        val plate=EditText(this).apply { hint="Nomor kendaraan (opsional)" }
        val order=EditText(this).apply { hint="ID pesanan"; setText(orderNo) }

        // Use only OCR from the first page. Extracted fields are editable before saving.
        val lines = ocr.lines().map { it.trim() }.filter { it.isNotBlank() }
        val platePattern = Regex("\\b[A-Z]{1,2}\\s?\\d{1,4}\\s?[A-Z]{1,3}\\b")
        val plateIndex = lines.indexOfFirst { platePattern.containsMatchIn(it.uppercase()) }
        if (plateIndex >= 0) {
            val plateText = platePattern.find(lines[plateIndex].uppercase())?.value
            if (!plateText.isNullOrBlank()) plate.setText(plateText)
            if (plateIndex > 0) driver.setText(lines[plateIndex-1].take(60))
        }
        val pickupIndex = lines.indexOfFirst { it.contains("Dijemput",true) && it.contains("dari",true) }
        if (pickupIndex >= 0) origin.setText(lines.drop(pickupIndex).take(3).joinToString(" ").substringAfter("dari", "").take(100).trim())
        val destinationIndex = lines.indexOfFirst { it.contains("Sampai",true) && it.contains("di",true) }
        if (destinationIndex >= 0) dest.setText(lines.drop(destinationIndex).take(3).joinToString(" ").substringAfter("di", "").take(100).trim())

        listOf(service,date,amount,origin,dest,driver,plate,order).forEach { box.addView(it); addGap(box,3) }
        AlertDialog.Builder(this)
            .setTitle("Periksa nota sebelum disimpan")
            .setMessage(if(ocr.isNotBlank()) "PDF/foto berhasil dibaca. Untuk PDF hanya halaman pertama yang diproses. Periksa hasil ekstraksi sebelum menyimpan." else "Teks belum terbaca otomatis. Isi data yang tersedia; file asli tetap dilampirkan.")
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Batal",null)
            .setPositiveButton("Simpan ke Aktivitas") { _,_ ->
                val amt=amount.text.toString().filter { it.isDigit() }.toLongOrNull() ?: 0L
                val note = if(order.text.isNotBlank()) "ID pesanan: ${order.text}" else ""
                db.add(
                    service.text.toString().ifBlank{"Gojek"},
                    date.text.toString(),
                    amt,
                    origin.text.toString(),
                    dest.text.toString(),
                    driver.text.toString(),
                    plate.text.toString(),
                    note,
                    uri.toString()
                )
                Toast.makeText(this,"Nota berhasil ditambahkan ke Aktivitas",Toast.LENGTH_LONG).show()
                showActivity()
            }.show()
    }

    private fun savePdf(r:Receipt) {
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/pdf"; putExtra(Intent.EXTRA_TITLE,"Nota_${r.id}_MyRide.pdf") }
        pendingPdf=r; startActivityForResult(intent,1002)
    }
    private var pendingPdf:Receipt?=null
    private fun writePdf(uri:Uri,r:Receipt) {
        val doc=PdfDocument(); val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create()); val p=Paint().apply { color=ink; textSize=18f; isFakeBoldText=true }
        page.canvas.drawText("MY RIDE - DETAIL NOTA",40f,55f,p); p.textSize=13f; p.isFakeBoldText=false
        val lines=listOf("Layanan: ${r.service}","Tanggal: ${r.date}","Total: ${money(r.amount)}","Asal: ${r.origin}","Tujuan: ${r.destination}","Driver / Merchant: ${r.driver}","Kendaraan: ${r.plate}","Catatan: ${r.note}","","Dokumen ini dibuat dari data yang tersimpan di My Ride.")
        var y=100f; lines.forEach { page.canvas.drawText(it.take(85),40f,y,p); y+=28f }; doc.finishPage(page)
        try { contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }; Toast.makeText(this,"PDF berhasil disimpan",Toast.LENGTH_LONG).show() } catch(e:Exception) { Toast.makeText(this,"Gagal menyimpan PDF: ${e.message}",Toast.LENGTH_LONG).show() } finally { doc.close() }
    }

    private fun bottomNav(): LinearLayout = LinearLayout(this).apply {
        setBackgroundColor(Color.WHITE); gravity=Gravity.CENTER; orientation=LinearLayout.HORIZONTAL
        listOf("⌂\nBeranda","▤\nAktivitas","⌖\nMaps","♙\nProfil").forEach { item -> val parts=item.split("\n"); val t=TextView(this@MainActivity).apply { text=item; gravity=Gravity.CENTER; textSize=12f; setTextColor(if(parts[1]==currentTab) green else muted); setPadding(4,10,4,10); layoutParams=LinearLayout.LayoutParams(0,58,1f); setOnClickListener { when(parts[1]) { "Beranda"->showHome(); "Aktivitas"->showActivity(); "Maps"->showMap(); else->showProfile() } } }; addView(t) }
    }
    private fun showProfile() { currentTab="Profil"; base("Profil", "Pengaturan aplikasi"); val b=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,18,18,18) }; b.addView(emptyCard("My Ride • Versi 1.0.0","Aplikasi penyimpanan dan pembaca nota lokal. Tidak terhubung dengan akun Gojek dan tidak dapat melakukan pemesanan atau pembayaran.")); root.addView(b,LinearLayout.LayoutParams(-1,0,1f)); root.addView(bottomNav()) }

    override fun onResume() { super.onResume() }
    override fun onPause() { super.onPause() }
}

private class SimpleWatcher(val changed:(String)->Unit): android.text.TextWatcher {
    override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
    override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { changed(s?.toString() ?: "") }
    override fun afterTextChanged(s:android.text.Editable?) {}
}
