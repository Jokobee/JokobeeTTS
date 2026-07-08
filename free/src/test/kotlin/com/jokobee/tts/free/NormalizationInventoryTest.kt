package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RÉGRESSION — couverture des normaliseurs fr/en_US/en_GB/es/pt_BR/it sur les catégories
 * devises (symbole avant/après, par locale), heures, points cardinaux, ordinaux, abréviations,
 * dates (mois nommé) et dates numériques (JJ/MM ou MM/JJ selon la locale).
 * Chaque cas = (label, entrée, sortie attendue) ; toute dérive casse le test.
 */
class NormalizationInventoryTest {

    private data class Case(val label: String, val input: String, val expected: String)

    private val cases: Map<String, Pair<BaseNormalizer, List<Case>>> = mapOf(
        "fr_CA" to (FrenchNormalizer(IcuVerbalizer()) to listOf(
            Case("devise \$", "Ça coûte 5,50 \$ en tout.", "Ça coûte cinq dollars et cinquante sous en tout."),
            Case("heure", "Rendez-vous à 15 h 30.", "Rendez-vous à quinze heures trente."),
            Case("cardinal", "Prends la sortie 5 N puis l'autoroute 20 SO.",
                "Prends la sortie cinq Nord puis l'autoroute vingt Sud-Ouest."),
            Case("ordinal", "Il est arrivé 1er et elle 2e.", "Il est arrivé premier et elle deuxième."),
            Case("abrév", "M. Tremblay, Mme Roy et le Dr Côté.",
                "monsieur Tremblay, madame Roy et le docteur Côté."),
            Case("date", "Né le 3 mars 2024.", "Né le trois mars deux mille vingt-quatre."),
            Case("date num", "Date : 15/03/2024.", "Date : quinze mars deux mille vingt-quatre."),
            Case("unité", "C'est à 5 km d'ici.", "C'est à cinq kilomètres d'ici."),
            Case("roman siècle", "Au XXe siècle.", "Au vingtième siècle."),
            Case("roman roi", "Sous Louis XIV.", "Sous Louis quatorze."),
            Case("fraction", "Ajoute 3/4 de tasse.", "Ajoute trois quarts de tasse."),
            Case("range", "Pages 10-15.", "Pages dix à quinze."),
            Case("symbole", "R&D à 9 h.", "R et D à neuf heures."),
        )),
        "en_US" to (EnglishNormalizer(IcuVerbalizer()) to listOf(
            Case("devise \$", "It costs \$5.50 total.", "It costs five dollars and fifty cents total."),
            Case("heure", "Meet at 3:30 PM.", "Meet at three thirty PM."),
            Case("heure 24h", "The train leaves at 15:45.", "The train leaves at fifteen forty-five."),
            Case("cardinal", "Take Highway 5 N and Exit 3 SW.",
                "Take Highway five North and Exit three Southwest."),
            Case("ordinal", "He came 1st and she came 2nd.", "He came first and she came second."),
            Case("abrév", "Mr. Smith, Mrs. Jones, Dr. Brown on Main St.",
                "Mister Smith, Misses Jones, Doctor Brown on Main Street"),
            Case("date", "Born on March 3rd 2024.", "Born on March third twenty twenty-four."),
            Case("date num", "Date: 03/15/2024.", "Date: March fifteenth twenty twenty-four."),
            Case("unit km", "It is 5 km away.", "It is five kilometers away."),
            Case("unit mph", "Top speed 60 mph.", "Top speed sixty miles per hour."),
            Case("roman", "Queen Elizabeth II reigned.", "Queen Elizabeth the second reigned."),
            Case("fraction", "Mix 3/4 of the batch.", "Mix three quarters of the batch."),
            Case("range", "Pages 10-15 missing.", "Pages ten to fifteen missing."),
            Case("symbol", "R&D and Q&A at 9 AM.", "R and D and Q and A at nine AM."),
            Case("letters", "The FBI and NASA agreed.", "The F B I and NASA agreed."),
        )),
        "en_GB" to (BritishEnglishNormalizer(IcuVerbalizer()) to listOf(
            Case("devise £", "It costs £5.50 total.", "It costs five pounds and fifty pence total."),
            Case("ordinal", "He came 1st and she came 2nd.", "He came first and she came second."),
            Case("abrév", "Mr. Smith and Dr. Brown.", "Mister Smith and Doctor Brown."),
            Case("date", "Born on 3rd March 2024.", "Born on the third of March twenty twenty-four."),
            Case("date num", "Date: 15/03/2024.", "Date: the fifteenth of March twenty twenty-four."),
            Case("unit", "It is 5 km away.", "It is five kilometers away."),
        )),
        "es" to (SpanishNormalizer(IcuVerbalizer()) to listOf(
            Case("devise \$", "Cuesta \$5,50 en total.", "Cuesta cinco dólares con cincuenta centavos en total."),
            Case("devise €", "Son €10,25.", "Son diez euros con veinticinco céntimos."),
            Case("heure", "Son las 3:30 de la tarde.", "Son las tres treinta de la tarde."),
            Case("heure 24h", "El tren sale a las 15:45.", "El tren sale a las quince cuarenta y cinco."),
            Case("cardinal", "Ve hacia el N y luego al SO.", "Ve hacia el Norte y luego al Suroeste."),
            Case("ordinal", "Quedó en 1º y ella en 2ª.", "Quedó en primero y ella en segunda."),
            Case("abrév", "El Sr. García, la Sra. López y el Dr. Ruiz.",
                "El Señor García, la Señora López y el Doctor Ruiz."),
            Case("date", "Nació el 3 de marzo de 2024.", "Nació el tres de marzo de dos mil veinticuatro."),
            Case("date num", "Fecha: 15/03/2024.", "Fecha: quince de marzo de dos mil veinticuatro."),
            Case("roman siglo", "El siglo XXI empezó.", "El siglo veintiuno empezó."),
            Case("roman rey", "Felipe II reinó.", "Felipe segundo reinó."),
            Case("fraction", "Mezcla 3/4 de harina.", "Mezcla tres cuartos de harina."),
            Case("range", "Páginas 10-15.", "Páginas diez a quince."),
            Case("symbole", "I+D y M&A.", "I más D y M y A."),
            Case("letters", "El DNI y la ONU.", "El D N I y la ONU."),
            Case("adresse", "Vivo en la Av. Colón, Depto. 3.",
                "Vivo en la Avenida Colón, Departamento tres."),
        )),
        "pt_BR" to (BrazilianPortugueseNormalizer(IcuVerbalizer()) to listOf(
            Case("devise R\$", "Custa R\$ 5,50 no total.", "Custa cinco reais e cinquenta centavos no total."),
            Case("devise €", "São €10,25.", "São dez euros e vinte e cinco centavos."),
            Case("heure", "Chego às 15h30.", "Chego às quinze horas e trinta."),
            Case("heure h", "A loja abre às 9h.", "A loja abre às nove horas."),
            Case("cardinal", "Vá para o N e depois para o SO.", "Vá para o Norte e depois para o Sudoeste."),
            Case("ordinal", "Ficou em 1º e ela em 2ª.", "Ficou em primeiro e ela em segunda."),
            Case("abrév", "O Sr. Silva, a Sra. Costa e o Dr. Souza.",
                "O Senhor Silva, a Senhora Costa e o Doutor Souza."),
            Case("date", "Nasceu em 3 de março de 2024.",
                "Nasceu em três de março de dois mil e vinte e quatro."),
            Case("date num", "Data: 15/03/2024.", "Data: quinze de março de dois mil e vinte e quatro."),
            Case("roman século", "O século XXI começou.", "O século vinte e um começou."),
            Case("roman rei", "Dom Pedro II governou.", "Dom Pedro segundo governou."),
            Case("fraction", "Misture 3/4 de farinha.", "Misture três quartos de farinha."),
            Case("range", "Páginas 10-15.", "Páginas dez a quinze."),
            Case("symbole", "P&D e M&A.", "P e D e M e A."),
            Case("letters", "O IBGE e a ONU.", "O I B G E e a ONU."),
            Case("adresse", "Moro na Av. Paulista.", "Moro na Avenida Paulista."),
        )),
        "it" to (ItalianNormalizer(IcuVerbalizer()) to listOf(
            Case("devise €", "Costa €5,50 in tutto.", "Costa cinque euro e cinquanta centesimi in tutto."),
            Case("devise \$", "Sono \$10,25.", "Sono dieci dollari e venticinque centesimi."),
            Case("heure", "Sono le 15:30.", "Sono le quindici e trenta."),
            Case("heure alle", "Il treno parte alle 9.", "Il treno parte alle nove."),
            Case("cardinal", "Vai verso N e poi a SO.", "Vai verso Nord e poi a Sud-ovest."),
            Case("ordinal", "È arrivato 1º e lei 2ª.", "È arrivato primo e lei seconda."),
            Case("abrév", "Il Sig. Rossi, il Dott. Bianchi e la Sig.ra Verdi.",
                "Il Signor Rossi, il Dottor Bianchi e la Signora Verdi."),
            Case("date", "È nato il 3 marzo 2024.", "È nato il tre marzo duemilaventiquattro."),
            Case("date num", "Data: 15/03/2024.", "Data: quindici marzo duemilaventiquattro."),
            Case("roman secolo", "Il XXI secolo è iniziato.", "Il ventunesimo secolo è iniziato."),
            Case("roman re", "Luigi XIV regnò.", "Luigi quattordicesimo regnò."),
            Case("fraction", "Mescola 3/4 di farina.", "Mescola tre quarti di farina."),
            Case("range", "Pagine 10-15.", "Pagine dieci a quindici."),
            Case("symbole", "R&S e Q&A.", "R e S e Q e A."),
            Case("letters", "L'FBI e la NATO.", "L'F B I e la NATO."),
            Case("adresse", "Abito in P.za Duomo.", "Abito in piazza Duomo."),
        )),
    )

    @Test fun regression() {
        for ((lang, pair) in cases) {
            val (norm, list) = pair
            for (c in list) {
                assertEquals("[$lang/${c.label}] «${c.input}»", c.expected, norm.normalize(c.input))
            }
        }
    }
}
