package com.example.agendei_pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.compose.AsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.agendei_pro.core.model.Service
import com.example.agendei_pro.ui.viewmodel.ServicesViewModel
import java.text.NumberFormat
import java.util.Locale

data class SuggestedService(
    val name: String,
    val category: String,
    val defaultPrice: Double,
    val defaultDuration: Int,
    val imageUrl: String = ""
)

fun getSuggestionsForSegment(segment: String): List<SuggestedService> {
    return when (segment) {
        "CABELEIREIRO" -> listOf(
            SuggestedService("Corte Feminino", "Corte e Penteado", 80.0, 60, "https://images.unsplash.com/photo-1562322140-8baeececf3df?w=500"),
            SuggestedService("Corte Masculino", "Corte e Penteado", 45.0, 40, "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=500"),
            SuggestedService("Escova / Chapinha", "Corte e Penteado", 50.0, 45, "https://images.unsplash.com/photo-1522337060762-d41222a27fdf?w=500"),
            SuggestedService("Penteado Completo", "Corte e Penteado", 120.0, 60, "https://images.unsplash.com/photo-1595853035070-59a39fe84de3?w=500"),
            SuggestedService("Coloração / Tintura", "Coloração", 90.0, 90, "https://images.unsplash.com/photo-1607779097040-26e80aa78e66?w=500"),
            SuggestedService("Luzes / Mechas", "Coloração", 250.0, 180, "https://images.unsplash.com/photo-1560869713-7d0a29430f39?w=500"),
            SuggestedService("Hidratação / Reconstrução", "Tratamentos", 70.0, 45, "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=500"),
            SuggestedService("Progressiva", "Tratamentos", 180.0, 120, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Botox Capilar", "Tratamentos", 130.0, 60, "https://images.unsplash.com/photo-1522337060762-d41222a27fdf?w=500"),
            SuggestedService("Selagem Térmica", "Tratamentos", 140.0, 90, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Penteado Noiva", "Corte e Penteado", 300.0, 120, "https://images.unsplash.com/photo-1595853035070-59a39fe84de3?w=500"),
            SuggestedService("Lavado Especial", "Tratamentos", 35.0, 20, "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=500"),
            SuggestedService("Cronograma Capilar", "Tratamentos", 250.0, 60, "https://images.unsplash.com/photo-1560869713-7d0a29430f39?w=500")
        )
        "MANICURE" -> listOf(
            SuggestedService("Pé e Mão", "Unhas Comuns", 50.0, 60, "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500"),
            SuggestedService("Apenas Mão", "Unhas Comuns", 25.0, 30, "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500"),
            SuggestedService("Apenas Pé", "Unhas Comuns", 30.0, 30, "https://images.unsplash.com/photo-1519699047748-de8e457a634e?w=500"),
            SuggestedService("Alongamento em Gel", "Alongamentos", 120.0, 120, "https://images.unsplash.com/photo-1632345031435-8797b2d58045?w=500"),
            SuggestedService("Manutenção Alongamento", "Alongamentos", 70.0, 90, "https://images.unsplash.com/photo-1607779097040-26e80aa78e66?w=500"),
            SuggestedService("Esmaltação em Gel", "Unhas Comuns", 45.0, 40, "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500"),
            SuggestedService("Banho de Gel", "Alongamentos", 80.0, 60, "https://images.unsplash.com/photo-1632345031435-8797b2d58045?w=500"),
            SuggestedService("Spa dos Pés", "Tratamentos", 60.0, 45, "https://images.unsplash.com/photo-1519699047748-de8e457a634e?w=500"),
            SuggestedService("Alongamento em Fibra de Vidro", "Alongamentos", 150.0, 150, "https://images.unsplash.com/photo-1632345031435-8797b2d58045?w=500"),
            SuggestedService("Decoração Artística / Nail Art", "Outros", 15.0, 15, "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500"),
            SuggestedService("Blindagem de Unhas", "Alongamentos", 70.0, 50, "https://images.unsplash.com/photo-1632345031435-8797b2d58045?w=500"),
            SuggestedService("Cutilagem Russa", "Unhas Comuns", 40.0, 40, "https://images.unsplash.com/photo-1607779097040-26e80aa78e66?w=500")
        )
        "ESTETICA" -> listOf(
            SuggestedService("Limpeza de Pele", "Facial", 120.0, 90, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500"),
            SuggestedService("Peeling Químico", "Facial", 150.0, 45, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500"),
            SuggestedService("Drenagem Linfática", "Corporal", 90.0, 60, "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500"),
            SuggestedService("Massagem Modeladora", "Corporal", 80.0, 50, "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500"),
            SuggestedService("Massagem Relaxante", "Corporal", 100.0, 60, "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500"),
            SuggestedService("Design de Sobrancelha", "Sobrancelhas e Cílios", 30.0, 30, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Micropigmentação", "Sobrancelhas e Cílios", 350.0, 120, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Extensão de Cílios", "Sobrancelhas e Cílios", 140.0, 120, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Peeling de Diamante", "Facial", 130.0, 60, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500"),
            SuggestedService("Depilação Completa", "Corporal", 120.0, 60, "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500"),
            SuggestedService("Criolipólise", "Corporal", 250.0, 90, "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500"),
            SuggestedService("Revitalização Facial", "Facial", 90.0, 45, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500")
        )
        "TATTOO" -> listOf(
            SuggestedService("Tatuagem Pequena (Fine Line)", "Tatuagens", 150.0, 60, "https://images.unsplash.com/photo-1598257006458-087169a1f08d?w=500"),
            SuggestedService("Tatuagem Média / Escrita", "Tatuagens", 300.0, 120, "https://images.unsplash.com/photo-1611501275019-9b5cdae94fa8?w=500"),
            SuggestedService("Tatuagem Grande / Realismo", "Tatuagens", 800.0, 240, "https://images.unsplash.com/photo-1590246814883-57751c3a6288?w=500"),
            SuggestedService("Tatuagem Colorida / Aquarela", "Tatuagens", 400.0, 150, "https://images.unsplash.com/photo-1568515045052-f9a854d70bfd?w=500"),
            SuggestedService("Cobertura de Tatuagem (Cover-up)", "Tatuagens", 500.0, 180, "https://images.unsplash.com/photo-1590246814883-57751c3a6288?w=500"),
            SuggestedService("Aplicação de Piercing", "Piercings", 80.0, 30, "https://images.unsplash.com/photo-1598257006463-7c71a242ec3f?w=500"),
            SuggestedService("Atualização de Joia", "Piercings", 50.0, 20, "https://images.unsplash.com/photo-1598257006463-7c71a242ec3f?w=500"),
            SuggestedService("Remoção a Laser", "Outros", 200.0, 45, "https://images.unsplash.com/photo-1598257006458-087169a1f08d?w=500")
        )
        else -> listOf( // BARBEARIA
            SuggestedService("Corte Social", "Cabelo", 35.0, 30, "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500"),
            SuggestedService("Corte Degradê", "Cabelo", 45.0, 45, "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=500"),
            SuggestedService("Corte Degradê Navalhado", "Cabelo", 55.0, 50, "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=500"),
            SuggestedService("Barba Simples", "Barba", 25.0, 20, "https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=500"),
            SuggestedService("Barba Completa (Terapia)", "Barba", 40.0, 40, "https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=500"),
            SuggestedService("Barbaterapia", "Barba", 50.0, 45, "https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=500"),
            SuggestedService("Sobrancelha", "Cabelo", 15.0, 15, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Sobrancelha Navalhada", "Cabelo", 20.0, 20, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Corte + Barba", "Combos", 60.0, 60, "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500"),
            SuggestedService("Pigmentação", "Barba", 20.0, 20, "https://images.unsplash.com/photo-1605497746444-ac9dbd324ce8?w=500"),
            SuggestedService("Selagem / Progressiva", "Cabelo", 80.0, 60, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"),
            SuggestedService("Hidratação Especial", "Tratamentos", 30.0, 30, "https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=500")
        )
    }
}

fun getSmartImageUrls(name: String, segment: String): List<String> {
    val cleanName = name.lowercase(Locale.ROOT)
        .replace("á", "a")
        .replace("â", "a")
        .replace("ã", "a")
        .replace("é", "e")
        .replace("ê", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ô", "o")
        .replace("õ", "o")
        .replace("ú", "u")
        .replace("ç", "c")

    // Curated high-quality professional Unsplash images for salon services
    val nails = listOf(
        "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500", // applying red nail polish
        "https://images.unsplash.com/photo-1632345031435-8797b2d58045?w=500", // beautiful manicure nails
        "https://images.unsplash.com/photo-1519699047748-de8e457a634e?w=500", // pedicure bath
        "https://images.unsplash.com/photo-1607779097040-26e80aa78e66?w=500", // nail art
        "https://images.unsplash.com/photo-1522337060762-d41222a27fdf?w=500", // nail care treatment
        "https://images.unsplash.com/photo-1519014816548-bf5fe059798b?w=500", // pink nails
        "https://images.unsplash.com/photo-1604654789508-c87a5522e96d?w=500"  // filing nails
    )

    val browsAndLashes = listOf(
        "https://images.unsplash.com/photo-1560750588-73207b1ef5b8?w=500", // microblading
        "https://images.unsplash.com/photo-1620574387735-3624d75b2dbc?w=500", // eyebrow shaping
        "https://images.unsplash.com/photo-1616394584738-fc6e612e71b9?w=500", // eyebrow threading
        "https://images.unsplash.com/photo-1582284540020-8acae03f4908?w=500", // eyelash extensions
        "https://images.unsplash.com/photo-1596178060810-72cb634b8c57?w=500"  // beauty close up
    )

    val maleHairAndBeard = listOf(
        "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500", // male haircut
        "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=500", // clipper cut
        "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=500", // barber styling
        "https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=500", // straight razor shave
        "https://images.unsplash.com/photo-1605497746444-ac9dbd324ce8?w=500", // fade hairstyle
        "https://images.unsplash.com/photo-1517832606299-7ae9b720a186?w=500", // man in chair
        "https://images.unsplash.com/photo-1593702295094-aec22df26535?w=500"  // barbershop details
    )

    val femaleHair = listOf(
        "https://images.unsplash.com/photo-1562322140-8baeececf3df?w=500", // hair stylist working
        "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500", // hair salon interior
        "https://images.unsplash.com/photo-1595853035070-59a39fe84de3?w=500", // hair styling
        "https://images.unsplash.com/photo-1607779097040-26e80aa78e66?w=500", // hair coloring
        "https://images.unsplash.com/photo-1560869713-7d0a29430f39?w=500", // hair washing
        "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=500", // styling tools
        "https://images.unsplash.com/photo-1492106087820-71f1a00d2b11?w=500"  // blonde woman hair
    )

    val skincareAndSpa = listOf(
        "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500", // skincare facial mask
        "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500", // massage stones
        "https://images.unsplash.com/photo-1519699047748-de8e457a634e?w=500", // relaxing massage
        "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=500", // skincare bottles
        "https://images.unsplash.com/photo-1590439471364-192aa70c0c53?w=500", // applying facial cream
        "https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=500", // back massage
        "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=500"  // beauty clinic clay
    )

    val tattooAndPiercing = listOf(
        "https://images.unsplash.com/photo-1598257006458-087169a1f08d?w=500", // tattoo gun
        "https://images.unsplash.com/photo-1611501275019-9b5cdae94fa8?w=500", // tattooing process
        "https://images.unsplash.com/photo-1590246814883-57751c3a6288?w=500", // drawing stencil
        "https://images.unsplash.com/photo-1568515045052-f9a854d70bfd?w=500", // tattoo art
        "https://images.unsplash.com/photo-1598257006463-7c71a242ec3f?w=500"  // ear piercing jewelry
    )

    val makeup = listOf(
        "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=500", // makeup brushes
        "https://images.unsplash.com/photo-1522337654788-8b13dee7a37e?w=500", // applying makeup
        "https://images.unsplash.com/photo-1512496015851-a90fb38ba796?w=500", // lipsticks collection
        "https://images.unsplash.com/photo-1457974189564-a022510f54bc?w=500"  // cosmetics powders
    )

    val waxing = listOf(
        "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?w=500", // hot wax spa
        "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=500", // waxing leg
        "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500"  // waxing skin
    )

    // Modern studio interiors based on salon segments
    val interiorBarbearia = listOf(
        "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=500",
        "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500",
        "https://images.unsplash.com/photo-1599351431202-1e0f0137899a?w=500",
        "https://images.unsplash.com/photo-1593702295094-aec22df26535?w=500"
    )

    val interiorCabeleireiro = listOf(
        "https://images.unsplash.com/photo-1521590832167-7bcbfaa6381f?w=500",
        "https://images.unsplash.com/photo-1562322140-8baeececf3df?w=500",
        "https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=500",
        "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500"
    )

    val interiorManicure = listOf(
        "https://images.unsplash.com/photo-1604654894610-df63bc536371?w=500",
        "https://images.unsplash.com/photo-1632345031435-8797b2d58045?w=500",
        "https://images.unsplash.com/photo-1519014816548-bf5fe059798b?w=500"
    )

    val interiorEstetica = listOf(
        "https://images.unsplash.com/photo-1600334188945-1a73d329029e?w=500",
        "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=500",
        "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=500"
    )

    val interiorTattoo = listOf(
        "https://images.unsplash.com/photo-1598257006458-087169a1f08d?w=500",
        "https://images.unsplash.com/photo-1611501275019-9b5cdae94fa8?w=500",
        "https://images.unsplash.com/photo-1590246814883-57751c3a6288?w=500"
    )

    val genericSalon = listOf(
        "https://images.unsplash.com/photo-1521590832167-7bcbfaa6381f?w=500",
        "https://images.unsplash.com/photo-1562322140-8baeececf3df?w=500",
        "https://images.unsplash.com/photo-1585747860715-2ba37e788b70?w=500"
    )

    // Step 1: Match keywords to appropriate curated photo category
    val list = when {
        cleanName.contains("unha") || cleanName.contains("alongamento") || 
        cleanName.contains("manicure") || cleanName.contains("pedicure") || 
        cleanName.contains("nail") || cleanName.contains("esmalte") || 
        cleanName.contains("cutilagem") -> nails

        cleanName.contains("sobrancelha") || cleanName.contains("cilios") || 
        cleanName.contains("cilio") || cleanName.contains("extensao") || 
        cleanName.contains("microblading") || cleanName.contains("henna") ||
        cleanName.contains("micropigmentacao") -> browsAndLashes

        cleanName.contains("barba") || cleanName.contains("barbaterapia") || 
        cleanName.contains("bigode") || cleanName.contains("degrade") || 
        cleanName.contains("navalha") || cleanName.contains("undercut") || 
        cleanName.contains("barbeiro") || (cleanName.contains("corte") && cleanName.contains("masculino")) || 
        (segment == "BARBEARIA" && (cleanName.contains("corte") || cleanName.contains("cabelo") || cleanName.contains("social"))) ||
        cleanName.contains("homem") || cleanName.contains("masculino") -> maleHairAndBeard

        cleanName.contains("corte") || cleanName.contains("cabelo") || 
        cleanName.contains("escova") || cleanName.contains("chapinha") || 
        cleanName.contains("penteado") || cleanName.contains("hidratacao") || 
        cleanName.contains("reconstrucao") || cleanName.contains("progressiva") || 
        cleanName.contains("botox") || cleanName.contains("selagem") || 
        cleanName.contains("luzes") || cleanName.contains("mechas") || 
        cleanName.contains("coloracao") || cleanName.contains("tintura") || 
        cleanName.contains("franja") || cleanName.contains("quimica") || 
        cleanName.contains("feminino") -> femaleHair

        cleanName.contains("pele") || cleanName.contains("limpeza") || 
        cleanName.contains("facial") || cleanName.contains("peeling") || 
        cleanName.contains("criolipolise") || cleanName.contains("drenagem") || 
        cleanName.contains("massagem") || cleanName.contains("relaxante") || 
        cleanName.contains("modeladora") || cleanName.contains("spa") || 
        cleanName.contains("corporal") -> skincareAndSpa

        cleanName.contains("tatuagem") || cleanName.contains("tattoo") || 
        cleanName.contains("piercing") || cleanName.contains("tatuar") || 
        cleanName.contains("joia") -> tattooAndPiercing

        cleanName.contains("maquiagem") || cleanName.contains("make") || 
        cleanName.contains("rimel") || cleanName.contains("batom") || 
        cleanName.contains("sombra") -> makeup

        cleanName.contains("depilacao") || cleanName.contains("cera") -> waxing

        else -> emptyList()
    }

    if (list.isNotEmpty()) return list

    // Step 2: Fall back to high-quality studio interiors if no keyword matches (prevents cats/irrelevant results)
    return when (segment) {
        "BARBEARIA" -> interiorBarbearia
        "CABELEIREIRO" -> interiorCabeleireiro
        "MANICURE" -> interiorManicure
        "ESTETICA" -> interiorEstetica
        "TATTOO" -> interiorTattoo
        else -> genericSalon
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServicesScreen(
    viewModel: ServicesViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val services by viewModel.services.collectAsState()
    val segment by viewModel.salonSegment.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSuggestion by remember { mutableStateOf<SuggestedService?>(null) }

    val suggestions = remember(segment) {
        getSuggestionsForSegment(segment)
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Meus Serviços") }, 
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }
            ) 
        },
        floatingActionButton = { 
            FloatingActionButton(onClick = { 
                selectedSuggestion = null
                showAddDialog = true 
            }) { 
                Icon(Icons.Default.Add, null) 
            } 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            Text(
                text = "Sugestões Rápidas (Ramo: ${
                    when (segment) {
                        "CABELEIREIRO" -> "Cabelo"
                        "ESTETICA" -> "Estética"
                        "MANICURE" -> "Manicure"
                        "TATTOO" -> "Tattoo"
                        else -> "Barbearia"
                    }
                }):",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.labelSmall
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp), 
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions) { s ->
                    AssistChip(
                        onClick = { 
                            selectedSuggestion = s
                            showAddDialog = true
                        }, 
                        label = { Text(text = s.name) }
                    )
                }
            }

            if (services.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum serviço cadastrado.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f), 
                    contentPadding = PaddingValues(16.dp), 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(services) { service ->
                        ServiceItem(service, onDelete = { viewModel.deleteService(service.id) })
                    }
                }
            }
        }

        if (showAddDialog) {
            val isUploading by viewModel.isUploadingImage.collectAsState()
            var wasUploading by remember { mutableStateOf(false) }
            
            LaunchedEffect(isUploading) {
                if (isUploading) {
                    wasUploading = true
                } else if (wasUploading) {
                    showAddDialog = false
                    wasUploading = false
                }
            }

            AddServiceDialog(
                segment = segment,
                prefilledService = selectedSuggestion,
                isUploading = isUploading,
                onDismiss = { if (!isUploading) showAddDialog = false },
                onConfirm = { n, p, d, c, o, img, localUri -> 
                    viewModel.addService(n, p, d, c, o, img, localUri)
                    if (localUri == null || localUri == android.net.Uri.EMPTY) {
                        // If no local upload is needed, close immediately
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun ServiceItem(service: Service, onDelete: () -> Unit) {
    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (service.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = service.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(service.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                if (service.observation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(service.observation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(currency.format(service.price), fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = onDelete) { 
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) 
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    segment: String,
    prefilledService: SuggestedService?,
    isUploading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String, String, String, Uri?) -> Unit
) {
    var name by remember(prefilledService) { mutableStateOf(prefilledService?.name ?: "") }
    var price by remember(prefilledService) { mutableStateOf(prefilledService?.defaultPrice?.toString() ?: "") }
    var duration by remember(prefilledService) { mutableStateOf(prefilledService?.defaultDuration?.toString() ?: "30") }
    var category by remember(prefilledService) { mutableStateOf(prefilledService?.category ?: "") }
    var observation by remember { mutableStateOf("") }
    var imageUrl by remember(prefilledService) { mutableStateOf(prefilledService?.imageUrl ?: "") }

    var localImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var serviceExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    var isCustomService by remember { mutableStateOf(false) }
    var isCustomCategory by remember { mutableStateOf(false) }

    val suggestedServices = remember(segment) {
        getSuggestionsForSegment(segment)
    }

    val suggestedCategories = remember(segment) {
        when (segment) {
            "CABELEIREIRO" -> listOf("Corte e Penteado", "Coloração", "Tratamentos", "Manicure", "Outros")
            "MANICURE" -> listOf("Unhas Comuns", "Alongamentos", "Tratamentos", "Outros")
            "ESTETICA" -> listOf("Facial", "Corporal", "Sobrancelhas e Cílios", "Outros")
            "TATTOO" -> listOf("Tatuagens", "Piercings", "Outros")
            else -> listOf("Cabelo", "Barba", "Combos", "Outros")
        }
    }

    // Keep candidate URLs list in memory
    var candidateUrls by remember(name) {
        mutableStateOf(if (name.isNotBlank()) getSmartImageUrls(name, segment) else emptyList())
    }
    var selectedImageIndex by remember(name) { mutableIntStateOf(0) }

    // Auto-update imageUrl when name changes and no local image is selected
    LaunchedEffect(name) {
        if (name.isNotBlank() && localImageUri == null) {
            val urls = getSmartImageUrls(name, segment)
            candidateUrls = urls
            selectedImageIndex = 0
            if (urls.isNotEmpty()) {
                imageUrl = urls[0]
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            localImageUri = uri
            imageUrl = "" // Clear internet URL
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Serviço") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Visual Image Preview 📸
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val hasImage = (localImageUri != null && localImageUri != Uri.EMPTY) || imageUrl.isNotBlank()
                    if (hasImage) {
                        SubcomposeAsyncImage(
                            model = if (localImageUri != null && localImageUri != Uri.EMPTY) localImageUri else imageUrl,
                            contentDescription = "Visualização do Serviço",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        ) {
                            val state = painter.state
                            if (state is AsyncImagePainter.State.Loading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (state is AsyncImagePainter.State.Error) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Erro ao carregar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                SubcomposeAsyncImageContent()
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Sem Foto", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Image Actions Row (Change / Search another)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botão para carregar do dispositivo 📂
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Da Galeria", fontSize = 12.sp)
                    }

                    // Botão para rodar a imagem da internet 🔄
                    Button(
                        onClick = {
                            if (candidateUrls.isNotEmpty()) {
                                selectedImageIndex = (selectedImageIndex + 1) % candidateUrls.size
                                imageUrl = candidateUrls[selectedImageIndex]
                                localImageUri = null // Reset local image if picking from search list
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = candidateUrls.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar Outra", fontSize = 12.sp)
                    }
                }

                // Nome do Serviço com controle de customizado/dropdown
                Column {
                    if (isCustomService) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome do Serviço") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { isCustomService = false }) {
                                    Icon(Icons.Default.List, "Selecionar da lista")
                                }
                            }
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = serviceExpanded,
                            onExpandedChange = { serviceExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Nome do Serviço") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = serviceExpanded,
                                onDismissRequest = { serviceExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✍️ Nome Personalizado...", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        name = ""
                                        isCustomService = true
                                        serviceExpanded = false
                                    }
                                )
                                HorizontalDivider()
                                suggestedServices.forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = { Text(suggestion.name) },
                                        onClick = {
                                            name = suggestion.name
                                            category = suggestion.category
                                            price = suggestion.defaultPrice.toString()
                                            duration = suggestion.defaultDuration.toString()
                                            imageUrl = suggestion.imageUrl
                                            localImageUri = null
                                            serviceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { isCustomService = true; name = "" },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(top = 2.dp)
                        ) {
                            Text("+ Cadastrar serviço personalizado", fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = price, 
                    onValueChange = { price = it }, 
                    label = { Text("Preço (R$)") }, 
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = duration, 
                    onValueChange = { duration = it }, 
                    label = { Text("Duração (min)") }, 
                    modifier = Modifier.fillMaxWidth()
                )

                // Categoria com controle de customizado/dropdown
                Column {
                    if (isCustomCategory) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Categoria") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { isCustomCategory = false }) {
                                    Icon(Icons.Default.List, "Selecionar da lista")
                                }
                            }
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoria") },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🏷️ Nova Categoria...", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        category = ""
                                        isCustomCategory = true
                                        categoryExpanded = false
                                    }
                                )
                                HorizontalDivider()
                                suggestedCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { isCustomCategory = true; category = "" },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(top = 2.dp)
                        ) {
                            Text("+ Criar nova categoria", fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = observation, 
                    onValueChange = { observation = it }, 
                    label = { Text("Observação/Descrição") }, 
                    modifier = Modifier.height(70.dp).fillMaxWidth()
                )
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    onConfirm(
                        name, 
                        price.replace(",", ".").toDoubleOrNull() ?: 0.0, 
                        duration.toIntOrNull() ?: 30, 
                        category, 
                        observation,
                        imageUrl,
                        localImageUri
                    ) 
                },
                enabled = name.isNotBlank() && price.isNotBlank() && !isUploading
            ) { 
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvando...")
                } else {
                    Text("Salvar")
                }
            } 
        },
        dismissButton = { 
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) { Text("Cancelar") } 
        }
    )
}
