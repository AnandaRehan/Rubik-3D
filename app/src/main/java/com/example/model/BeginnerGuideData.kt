package com.example.model

data class GuideStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val algorithm: String,
    val tip: String,
    val badgeColor: CubeColor
)

data class NotationGuideItem(
    val notation: String,
    val name: String,
    val description: String,
    val color: CubeColor
)

object BeginnerGuideData {
    val notations = listOf(
        NotationGuideItem("R / R'", "Right (Sisi Kanan)", "R = Putar kanan ke atas (searah jarum jam).\nR' = Putar kanan ke bawah.", CubeColor.RED),
        NotationGuideItem("L / L'", "Left (Sisi Kiri)", "L = Putar kiri ke bawah (searah jarum jam).\nL' = Putar kiri ke atas.", CubeColor.ORANGE),
        NotationGuideItem("U / U'", "Up (Sisi Atas)", "U = Putar atas ke kiri (searah jarum jam).\nU' = Putar atas ke kanan.", CubeColor.WHITE),
        NotationGuideItem("D / D'", "Down (Sisi Bawah)", "D = Putar bawah ke kanan (searah jarum jam).\nD' = Putar bawah ke kiri.", CubeColor.YELLOW),
        NotationGuideItem("F / F'", "Front (Sisi Depan)", "F = Putar depan ke kanan (searah jarum jam).\nF' = Putar depan ke kiri.", CubeColor.GREEN),
        NotationGuideItem("B / B'", "Back (Sisi Belakang)", "B = Putar belakang searah jarum jam.\nB' = Putar belakang berlawanan.", CubeColor.BLUE)
    )

    val beginnerSteps = listOf(
        GuideStep(
            stepNumber = 1,
            title = "Cross Putih (Tanda Tambah)",
            subtitle = "Lapisan Pertama - Tepi",
            description = "Susun 4 keping tepi (edge) berwarna Putih mengelilingi titik tengah Putih di atas, dan pastikan warna sampingnya sejajar dengan warna titik tengah (Center) masing-masing sisi.",
            algorithm = "F R U R' U' F'",
            tip = "Buat 'Bunga Aster' (tepi putih mengelilingi titik tengah kuning) terlebih dahulu, lalu putar 180° ke atas!",
            badgeColor = CubeColor.WHITE
        ),
        GuideStep(
            stepNumber = 2,
            title = "Sudut Putih & Lapisan Pertama",
            subtitle = "Rumus Dasar 'Sexy Move'",
            description = "Tempatkan keping sudut (corner) Putih di bawah posisinya, lalu ulangi rumus 4 langkah dasar ini sampai sudut Putih masuk dengan benar.",
            algorithm = "R U R' U'",
            tip = "Rumus R U R' U' adalah rumus terpenting di Rubik! Jika diulang 6 kali berturut-turut, Rubik akan kembali ke posisi semula.",
            badgeColor = CubeColor.WHITE
        ),
        GuideStep(
            stepNumber = 3,
            title = "Lapisan Tengah (Middle Layer)",
            subtitle = "Memasukkan Tepi ke Kanan / Kiri",
            description = "Pegang Rubik dengan sisi Putih di bawah dan Kuning di atas. Cari keping tepi tanpa warna kuning, sejajarkan dengan center-nya, lalu masukkan ke kanan dengan rumus ini.",
            algorithm = "U R U' R' U' F' U F",
            tip = "Untuk memasukkan ke sisi kiri, gunakan bayangan cerminnya: U' L' U L U F U' F'.",
            badgeColor = CubeColor.GREEN
        ),
        GuideStep(
            stepNumber = 4,
            title = "Cross Kuning di Sisi Atas",
            subtitle = "Membentuk Garis & Tanda Tambah Kuning",
            description = "Ubah pola titik atau huruf 'L' kuning di sisi atas menjadi garis horizontal, lalu menjadi tanda tambah (Cross) Kuning penuh.",
            algorithm = "F R U R' U' F'",
            tip = "Jika melihat pola garis kuning, pegang secara horizontal (kiri-kanan) sebelum menjalankan rumus.",
            badgeColor = CubeColor.YELLOW
        ),
        GuideStep(
            stepNumber = 5,
            title = "Meratakan Sisi Kuning (Sune)",
            subtitle = "Menyamakan Seluruh Permukaan Atas",
            description = "Gunakan algoritma klasik 'Sune' untuk memutar orientasi sudut-sudut atas hingga seluruh 9 keping di sisi atas berwarna Kuning.",
            algorithm = "R U R' U R U2 R'",
            tip = "Posisikan 1 sudut kuning yang sudah menghadap ke atas di pojok Kiri-Depan sebelum memutar rumus.",
            badgeColor = CubeColor.ORANGE
        ),
        GuideStep(
            stepNumber = 6,
            title = "Menukar Posisi Sudut Atas",
            subtitle = "Menempatkan Sudut ke Pojok yang Tepat",
            description = "Putar sudut-sudut di lapisan atas agar berada di antara ketiga warna sisi yang sesuai (meskipun arah warnanya belum terbalik sempurna).",
            algorithm = "U R U' L' U R' U' L",
            tip = "Cari satu sudut yang sudah berada di pojok yang benar, pegang di pojok Kanan-Depan-Atas, lalu jalankan rumus.",
            badgeColor = CubeColor.RED
        ),
        GuideStep(
            stepNumber = 7,
            title = "Penyelesaian Akhir (Orientasi Sudut)",
            subtitle = "Langkah Terakhir Menuju Solved!",
            description = "Putar sudut yang belum rata satu per satu menggunakan R' D' R D sampai warna atasnya menyatu, lalu putar U saja untuk membawa sudut berikutnya ke pojok Kanan-Depan.",
            algorithm = "R' D' R D R' D' R D",
            tip = "JANGAN memutar seluruh bodi Rubik di langkah ini! Cukup putar lapisan U untuk mengganti sudut yang ingin diselesaikan.",
            badgeColor = CubeColor.BLUE
        )
    )
}
